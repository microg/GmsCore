/*
 * SPDX-FileCopyrightText: 2024-2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.microg.wearable.SocketWearableConnection;
import org.microg.wearable.WearableConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wear OS Bluetooth RFCOMM transport thread for device pairing and data bridging.
 *
 * <p>Provides both server (listen) and client (connect) modes for Bluetooth RFCOMM
 * communication between an Android phone and Wear OS smartwatch. Internally wraps
 * {@link BluetoothSocket} instances in minimal {@link Socket} proxies so that the
 * existing {@link SocketWearableConnection} framing layer operates transparently.
 *
 * <h2>Usage</h2>
 *
 * <h3>Server Mode (Phone listens for watch connections)</h3>
 * <pre>{@code
 *   BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
 *   BluetoothConnectionThread server =
 *       BluetoothConnectionThread.createServer(adapter, messageHandler);
 *   server.start();
 *   // ... later
 *   server.shutdown();
 * }</pre>
 *
 * <h3>Client Mode (Watch connects to phone)</h3>
 * <pre>{@code
 *   BluetoothDevice phone = ...; // discovered via BLE or bonded device
 *   BluetoothConnectionThread client =
 *       BluetoothConnectionThread.createClient(phone, messageHandler);
 *   client.start();
 * }</pre>
 *
 * <h2>Reconnection</h2>
 * The server thread accepts connections in an infinite loop. When a connection
 * drops, the thread automatically returns to listening for the next incoming
 * connection. Client threads attempt reconnection up to {@link #MAX_RECONNECT_ATTEMPTS}
 * times with exponential backoff.
 *
 * <h2>Pairing State</h2>
 * Tracks connection state via {@link ConnectionState} enum, accessible through
 * {@link #getConnectionState()} and {@link #isConnected()}.
 *
 * @see SocketConnectionThread
 * @see WearableConnection
 */
public class BluetoothConnectionThread extends Thread {

    private static final String TAG = "GmsWearBtThread";

    /** RFCOMM service UUID for the Wear OS Bluetooth transport. */
    public static final UUID WEAR_BT_UUID =
            UUID.fromString("a3c87500-8ed3-4bdf-8a39-a01bebede295");

    /** SDP service name advertised alongside {@link #WEAR_BT_UUID}. */
    public static final String WEAR_BT_SERVICE_NAME = "WearOS-Companion";

    /** Maximum number of reconnection attempts in client mode. */
    public static final int MAX_RECONNECT_ATTEMPTS = 5;

    /** Base backoff delay in milliseconds for reconnection attempts. */
    private static final long BASE_BACKOFF_MS = 2000L;

    /** Connection states for monitoring. */
    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }

    private final Handler mainHandler;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private volatile SocketWearableConnection wearableConnection;
    private volatile ConnectionListener connectionListener;

    // Server-specific fields
    private final BluetoothAdapter adapter;
    private BluetoothServerSocket serverSocket;

    // Client-specific fields
    private final BluetoothDevice remoteDevice;
    private final boolean isServer;

    /**
     * Listener for connection state changes.
     */
    public interface ConnectionListener {
        void onStateChanged(ConnectionState state, BluetoothDevice device);
        void onError(String message, Exception e);
    }

    // Private constructor; use factory methods.
    private BluetoothConnectionThread(BluetoothAdapter adapter, boolean isServer) {
        this.adapter = adapter;
        this.isServer = isServer;
        this.remoteDevice = null;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    private BluetoothConnectionThread(BluetoothDevice remoteDevice) {
        this.adapter = null;
        this.isServer = false;
        this.remoteDevice = remoteDevice;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Creates a server thread that listens for incoming Bluetooth RFCOMM connections.
     *
     * @param adapter the local Bluetooth adapter
     * @return a new server-mode {@link BluetoothConnectionThread}
     */
    public static BluetoothConnectionThread createServer(BluetoothAdapter adapter) {
        return new BluetoothConnectionThread(adapter, true);
    }

    /**
     * Creates a client thread that connects to a remote Bluetooth device.
     *
     * @param remoteDevice the remote device to connect to
     * @return a new client-mode {@link BluetoothConnectionThread}
     */
    public static BluetoothConnectionThread createClient(BluetoothDevice remoteDevice) {
        return new BluetoothConnectionThread(remoteDevice);
    }

    /**
     * Sets an optional listener for connection state changes.
     */
    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    /**
     * Returns the current connection state.
     */
    public ConnectionState getConnectionState() {
        return connectionState;
    }

    /**
     * Returns {@code true} if the thread is currently connected to a peer.
     */
    public boolean isConnected() {
        return connectionState == ConnectionState.CONNECTED;
    }

    /**
     * Returns the most recently established {@link SocketWearableConnection}, or {@code null}.
     */
    public SocketWearableConnection getWearableConnection() {
        return wearableConnection;
    }

    /**
     * Initiates a graceful shutdown. Closes sockets and interrupts the thread.
     */
    public void shutdown() {
        running.set(false);
        closeSockets();
        interrupt();
    }

    @Override
    public void run() {
        if (isServer) {
            runServer();
        } else {
            runClient();
        }
    }

    // -------------------------------------------------------------------------
    // Server mode
    // -------------------------------------------------------------------------

    private void runServer() {
        Log.i(TAG, "Bluetooth server thread starting...");
        while (running.get()) {
            try {
                updateState(ConnectionState.DISCONNECTED);
                serverSocket = adapter.listenUsingRfcommWithServiceRecord(
                        WEAR_BT_SERVICE_NAME, WEAR_BT_UUID);
                Log.d(TAG, "Server socket opened, waiting for connections...");

                while (running.get()) {
                    updateState(ConnectionState.DISCONNECTED);
                    BluetoothSocket btSocket = serverSocket.accept();
                    if (!running.get()) break;

                    Log.i(TAG, "Accepted Bluetooth connection from "
                            + btSocket.getRemoteDevice().getAddress());
                    updateState(ConnectionState.CONNECTED);

                    Socket proxySock = proxySocket(btSocket);
                    wearableConnection = new SocketWearableConnection(
                            proxySock, proxySock.getLocalPort());
                    // The WearableImpl message handler thread will process messages
                    // Block until the socket is closed by the peer or shutdown
                    try {
                        // Keep reading to detect disconnection
                        InputStream is = proxySock.getInputStream();
                        byte[] buf = new byte[1];
                        while (running.get() && is.read(buf) != -1) {
                            // Connection alive; actual message processing is done
                            // by the WearableImpl layer
                        }
                    } catch (IOException e) {
                        Log.d(TAG, "Server connection closed: " + e.getMessage());
                    }
                    closeQuietly(btSocket);
                    wearableConnection = null;
                }
            } catch (IOException e) {
                if (running.get()) {
                    Log.w(TAG, "Server socket error, retrying: " + e.getMessage());
                    notifyError("Server error", e);
                    try { Thread.sleep(BASE_BACKOFF_MS); } catch (InterruptedException ignored) {}
                }
            } finally {
                updateState(ConnectionState.DISCONNECTED);
                closeServerSocket();
            }
        }
        Log.i(TAG, "Bluetooth server thread stopped.");
    }

    // -------------------------------------------------------------------------
    // Client mode
    // -------------------------------------------------------------------------

    private void runClient() {
        Log.i(TAG, "Bluetooth client thread starting for "
                + (remoteDevice != null ? remoteDevice.getAddress() : "unknown"));
        int attempts = 0;

        while (running.get() && attempts < MAX_RECONNECT_ATTEMPTS) {
            try {
                updateState(ConnectionState.CONNECTING);
                BluetoothSocket btSocket = remoteDevice.createRfcommSocketToServiceRecord(WEAR_BT_UUID);
                btSocket.connect();

                Log.i(TAG, "Connected to " + remoteDevice.getAddress());
                updateState(ConnectionState.CONNECTED);
                attempts = 0; // Reset on successful connection

                Socket proxySock = proxySocket(btSocket);
                wearableConnection = new SocketWearableConnection(
                        proxySock, proxySock.getLocalPort());

                try {
                    InputStream is = proxySock.getInputStream();
                    byte[] buf = new byte[1];
                    while (running.get() && is.read(buf) != -1) {
                        // Connection alive
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Client connection closed: " + e.getMessage());
                }
                closeQuietly(btSocket);
                wearableConnection = null;
                updateState(ConnectionState.DISCONNECTED);

                // Reconnection with exponential backoff
                if (running.get()) {
                    attempts++;
                    long backoff = BASE_BACKOFF_MS * (1L << Math.min(attempts, 5));
                    Log.d(TAG, "Reconnecting in " + backoff + "ms (attempt " + attempts + ")");
                    try { Thread.sleep(backoff); } catch (InterruptedException e) { break; }
                }
            } catch (IOException e) {
                if (running.get()) {
                    attempts++;
                    Log.w(TAG, "Client connection failed (attempt " + attempts + "): " + e.getMessage());
                    notifyError("Connection failed", e);
                    updateState(ConnectionState.DISCONNECTED);
                    long backoff = BASE_BACKOFF_MS * (1L << Math.min(attempts, 5));
                    try { Thread.sleep(backoff); } catch (InterruptedException ignored) { break; }
                }
            }
        }

        if (attempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "Max reconnection attempts reached. Giving up.");
            notifyError("Max reconnection attempts reached", null);
        }
        Log.i(TAG, "Bluetooth client thread stopped.");
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void updateState(ConnectionState newState) {
        ConnectionState old = connectionState;
        connectionState = newState;
        if (old != newState && connectionListener != null) {
            mainHandler.post(() -> connectionListener.onStateChanged(
                    newState, isServer ? null : remoteDevice));
        }
    }

    private void notifyError(String message, Exception e) {
        if (connectionListener != null) {
            mainHandler.post(() -> connectionListener.onError(message, e));
        }
    }

    private void closeSockets() {
        closeServerSocket();
        closeQuietly(wearableConnection);
    }

    private void closeServerSocket() {
        if (serverSocket != null) {
            try { serverSocket.close(); } catch (IOException ignored) {}
            serverSocket = null;
        }
    }

    private static void closeQuietly(java.io.Closeable c) {
        if (c != null) {
            try { c.close(); } catch (IOException ignored) {}
        }
    }

    private static void closeQuietly(BluetoothSocket s) {
        if (s != null) {
            try { s.close(); } catch (IOException ignored) {}
        }
    }

    private static void closeQuietly(SocketWearableConnection c) {
        if (c != null) {
            try { c.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Wraps a {@link BluetoothSocket} in a minimal {@link Socket} proxy so that
     * {@link SocketWearableConnection} can use its streams without any Wire-version-specific
     * framing code in this module.
     */
    @SuppressLint("NewApi")
    private static Socket proxySocket(BluetoothSocket btSocket) {
        return new Socket() {
            @Override
            public InputStream getInputStream() throws IOException {
                return btSocket.getInputStream();
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return btSocket.getOutputStream();
            }

            @Override
            public synchronized void close() throws IOException {
                btSocket.close();
                super.close();
            }

            @Override
            public boolean isConnected() {
                return btSocket.isConnected();
            }

            @Override
            public boolean isClosed() {
                return !btSocket.isConnected();
            }
        };
    }
}
