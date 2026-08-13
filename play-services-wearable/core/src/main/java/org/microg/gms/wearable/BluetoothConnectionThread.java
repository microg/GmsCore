/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import org.microg.wearable.SocketWearableConnection;
import org.microg.wearable.WearableConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transport thread that mirrors {@code SocketConnectionThread} but uses Bluetooth RFCOMM instead
 * of TCP sockets.
 *
 * <p>Internally, each {@link BluetoothSocket} is wrapped in a minimal {@link Socket} proxy so
 * that the existing {@link SocketWearableConnection} framing code can be reused unchanged.
 * This avoids any direct dependency on the Wire 1.x API that is otherwise unavailable at
 * compile time.
 *
 * <h3>Server mode</h3>
 * <pre>
 *   BluetoothConnectionThread bct =
 *       BluetoothConnectionThread.serverListen(adapter, messageHandler);
 *   bct.start();
 * </pre>
 * The thread opens a Bluetooth RFCOMM server socket and accepts connections in a loop.
 * Each accepted connection runs the message loop synchronously before looping back.
 *
 * <h3>Client mode</h3>
 * <pre>
 *   BluetoothConnectionThread bct =
 *       BluetoothConnectionThread.clientConnect(remoteDevice, messageHandler);
 *   bct.start();
 * </pre>
 * The thread connects to {@code remoteDevice} using {@link #WEAR_BT_UUID} and runs the
 * message loop until the connection is closed.
 */
public abstract class BluetoothConnectionThread extends Thread {

    private static final String TAG = "GmsWearBtThread";

    /**
     * RFCOMM service UUID used by microG for the wearable Bluetooth transport.
     *
     * <p><b>Important:</b> This is a <em>microG experimental</em> service UUID chosen for
     * local RFCOMM pairing experiments. It is <b>not</b> a documented Google Wear OS / Play
     * Services companion RFCOMM UUID. Notably, the same 128-bit value is publicly known as
     * the Eddystone BLE service UUID; microG reuses the bit pattern only as an RFCOMM SDP
     * record id and does <b>not</b> implement Eddystone advertising or claim stock Wear
     * companion interop through this constant.
     *
     * <p>Interoperability with factory Wear OS companions requires a device-proven UUID from
     * an HCI/BT snoop of a real pairing session. Until then, treat this transport as
     * microG-to-microG / lab-only.
     */
    public static final UUID WEAR_BT_UUID =
            UUID.fromString("a3c87500-8ed3-4bdf-8a39-a01bebede295");

    /** SDP service name advertised alongside {@link #WEAR_BT_UUID}. */
    static final String WEAR_BT_SERVICE_NAME = "microG Wearable";

    /**
     * Live peer connections accepted by this thread. Server-listen mode keeps every
     * accepted peer here; a second {@link #setWearableConnection} must not close the first.
     */
    private final Map<Integer, SocketWearableConnection> wearableConnections =
            new ConcurrentHashMap<Integer, SocketWearableConnection>();

    /** Most recently registered peer; kept for existing single-slot callers. */
    private volatile SocketWearableConnection wearableConnection;

    // Package-private constructor; concrete subclasses are anonymous inner classes.
    BluetoothConnectionThread() {}

    /**
     * Registers a live peer connection without tearing down any other peer.
     * The previous single-slot setter closed {@code prev}, which dropped the first
     * RFCOMM socket as soon as a second watch connected.
     */
    protected void setWearableConnection(SocketWearableConnection connection) {
        if (connection == null) {
            return;
        }
        wearableConnections.put(System.identityHashCode(connection), connection);
        this.wearableConnection = connection;
    }

    /** Returns the most recently established {@link SocketWearableConnection}, or null. */
    public SocketWearableConnection getWearableConnection() {
        return wearableConnection;
    }

    /** Snapshot of every live peer still registered on this thread. */
    public Collection<SocketWearableConnection> getWearableConnections() {
        return Collections.unmodifiableCollection(wearableConnections.values());
    }

    /**
     * Drops a disconnected peer from the live set. Does not close other peers.
     * Package-visible so {@link WearableImpl} can forget one server-accepted socket
     * without shutting down the RFCOMM listen thread.
     */
    public void removeWearableConnection(SocketWearableConnection connection) {
        if (connection == null) {
            return;
        }
        wearableConnections.remove(System.identityHashCode(connection));
        if (this.wearableConnection == connection) {
            this.wearableConnection = null;
        }
    }

    /** Closes the underlying Bluetooth server/client socket, unblocking any pending I/O. */
    public abstract void close();

    // -------------------------------------------------------------------------
    // Internal helper
    // -------------------------------------------------------------------------

    /**
     * Wraps a {@link BluetoothSocket} in a minimal {@link Socket} proxy so that
     * {@link SocketWearableConnection} can use its streams without any Wire-version-specific
     * framing code in this module.
     *
     * <p>{@link SocketWearableConnection} calls {@link #getInputStream()},
     * {@link #getOutputStream()}, {@link #isConnected()}, {@link #isClosed()}, and {@link #close()}
     * on the socket object.
     */
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
            public boolean isConnected() {
                return btSocket != null && btSocket.isConnected();
            }

            @Override
            public boolean isClosed() {
                return btSocket == null || !btSocket.isConnected();
            }

            @Override
            public synchronized void close() throws IOException {
                if (btSocket != null) {
                    btSocket.close();
                }
            }
        };
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates a server-side thread that listens for incoming Bluetooth RFCOMM connections
     * using the well-known {@link #WEAR_BT_UUID}.
     *
     * <p>Requires {@code BLUETOOTH_CONNECT} permission on API 31+.
     *
     * @param adapter  the local {@link BluetoothAdapter}; must not be null
     * @param listener message listener shared across all accepted connections
     * @return a {@link BluetoothConnectionThread} ready to be {@link #start()}ed
     */
    @SuppressLint("MissingPermission")
    public static BluetoothConnectionThread serverListen(
            BluetoothAdapter adapter, WearableConnection.Listener listener) {
        return new BluetoothConnectionThread() {
            private volatile BluetoothServerSocket serverSocket;

            @Override
            public void close() {
                BluetoothServerSocket s = serverSocket;
                serverSocket = null;
                if (s != null) {
                    try {
                        s.close();
                    } catch (IOException e) {
                        Log.w(TAG, "server close: error", e);
                    }
                }
            }

            @Override
            @SuppressLint("MissingPermission")
            public void run() {
                try {
                    serverSocket = adapter.listenUsingRfcommWithServiceRecord(
                            WEAR_BT_SERVICE_NAME, WEAR_BT_UUID);
                    Log.d(TAG, "server: listening for RFCOMM connections");
                    while (!Thread.interrupted()) {
                        BluetoothSocket btSocket;
                        try {
                            btSocket = serverSocket.accept();
                        } catch (IOException e) {
                            // serverSocket was closed via close() – stop the loop
                            break;
                        }
                        if (btSocket == null || Thread.interrupted()) break;
                        final BluetoothSocket accepted = btSocket;
                        // Run each peer on its own worker so accept() is not blocked by one client.
                        new Thread(() -> {
                            SocketWearableConnection conn = null;
                            try {
                                conn = new SocketWearableConnection(proxySocket(accepted), listener);
                                setWearableConnection(conn);
                                try {
                                    conn.run();
                                } finally {
                                    removeWearableConnection(conn);
                                    try {
                                        accepted.close();
                                    } catch (IOException e) {
                                        Log.w(TAG, "server: close error for accepted connection", e);
                                    }
                                }
                            } catch (IOException e) {
                                Log.w(TAG, "server: error on accepted connection", e);
                                if (conn != null) {
                                    removeWearableConnection(conn);
                                }
                            }
                        }, "GmsWearBtPeer").start();
                    }
                } catch (IOException e) {
                    if (!Thread.interrupted()) {
                        Log.w(TAG, "server: socket error", e);
                    }
                } finally {
                    BluetoothServerSocket s = serverSocket;
                    serverSocket = null;
                    if (s != null) {
                        try { s.close(); } catch (IOException e) { Log.d(TAG, "server: close error in finally", e); }
                    }
                }
            }
        };
    }

    /**
     * Creates a client-side thread that connects to a known Bluetooth {@code device} via
     * RFCOMM using {@link #WEAR_BT_UUID}.
     *
     * <p>Requires {@code BLUETOOTH_CONNECT} permission on API 31+.
     *
     * @param device   the remote Bluetooth device (e.g. the wearable)
     * @param listener message listener for the connection
     * @return a {@link BluetoothConnectionThread} ready to be {@link #start()}ed
     */
    @SuppressLint("MissingPermission")
    public static BluetoothConnectionThread clientConnect(
            BluetoothDevice device, WearableConnection.Listener listener) {
        return new BluetoothConnectionThread() {
            private volatile BluetoothSocket btSocket;

            @Override
            public void close() {
                BluetoothSocket s = btSocket;
                btSocket = null;
                if (s != null) {
                    try {
                        s.close();
                    } catch (IOException e) {
                        Log.w(TAG, "client close: error", e);
                    }
                }
            }

            @Override
            @SuppressLint("MissingPermission")
            public void run() {
                BluetoothSocket s = null;
                try {
                    s = device.createRfcommSocketToServiceRecord(WEAR_BT_UUID);
                    btSocket = s;
                    s.connect();
                    SocketWearableConnection conn =
                            new SocketWearableConnection(proxySocket(s), listener);
                    setWearableConnection(conn);
                    conn.run();
                } catch (IOException e) {
                    Log.w(TAG, "client: connection error", e);
                } finally {
                    btSocket = null;
                    if (s != null) {
                        try { s.close(); } catch (IOException e) { Log.d(TAG, "client: close error in finally", e); }
                    }
                }
            }
        };
    }
}
