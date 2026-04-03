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
import java.util.UUID;

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

    /** RFCOMM service UUID for the wearable Bluetooth transport. */
    public static final UUID WEAR_BT_UUID =
            UUID.fromString("a3c87500-8ed3-4bdf-8a39-a01bebede295");

    /** SDP service name advertised alongside {@link #WEAR_BT_UUID}. */
    static final String WEAR_BT_SERVICE_NAME = "WearOS";

    private volatile SocketWearableConnection wearableConnection;

    // Package-private constructor; concrete subclasses are anonymous inner classes.
    BluetoothConnectionThread() {}

    protected void setWearableConnection(SocketWearableConnection connection) {
        this.wearableConnection = connection;
    }

    /** Returns the most recently established {@link SocketWearableConnection}, or null. */
    public SocketWearableConnection getWearableConnection() {
        return wearableConnection;
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
     * <p>{@link SocketWearableConnection} only calls {@link #getInputStream()},
     * {@link #getOutputStream()}, and {@link #close()} on the socket object, so only those
     * three methods need to be overridden here.
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
            public synchronized void close() throws IOException {
                btSocket.close();
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
                        try {
                            SocketWearableConnection conn =
                                    new SocketWearableConnection(proxySocket(btSocket), listener);
                            setWearableConnection(conn);
                            conn.run();
                        } catch (IOException e) {
                            Log.w(TAG, "server: error on accepted connection", e);
                        }
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
