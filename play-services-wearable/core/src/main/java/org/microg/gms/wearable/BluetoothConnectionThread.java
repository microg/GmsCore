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
 * Bluetooth RFCOMM transport mirroring {@code org.microg.wearable.SocketConnectionThread}.
 *
 * <p>UUIDs are taken from Pixel Watch / Wear OS traffic research
 * (see https://github.com/teccheck/wearos-research/blob/main/docs/btcomm.md):
 * <ul>
 *   <li>{@link #WEARABLE_BT_UUID} — primary WearableBt channel; watch is the RFCOMM server</li>
 *   <li>{@link #FLOW_UUID} — Flow channel; phone is the RFCOMM server</li>
 *   <li>{@link #FLOW15_UUID} — Flow15 channel; phone is the RFCOMM server</li>
 * </ul>
 *
 * Each {@link BluetoothSocket} is wrapped in a minimal {@link Socket} proxy so the existing
 * length-prefixed protobuf framing in {@link SocketWearableConnection} can be reused.
 */
public abstract class BluetoothConnectionThread extends Thread {

    private static final String TAG = "GmsWearBtThread";

    /**
     * Primary WearableBt RFCOMM UUID. The wearable advertises this service; the phone connects
     * as a client.
     */
    public static final UUID WEARABLE_BT_UUID =
            UUID.fromString("5e8945b0-9525-11e3-a5e2-0800200c9a66");

    /** Flow RFCOMM UUID. The phone advertises this service. */
    public static final UUID FLOW_UUID =
            UUID.fromString("fafbdd20-83f0-4389-addf-917ac9dae5b2");

    /** Flow15 RFCOMM UUID. The phone advertises this service. */
    public static final UUID FLOW15_UUID =
            UUID.fromString("6a1eafb1-61c0-42a0-8bb0-a336fb1c3f00");

    static final String WEARABLE_BT_SERVICE_NAME = "WearableBt";
    static final String FLOW_SERVICE_NAME = "Flow";
    static final String FLOW15_SERVICE_NAME = "Flow15";

    private volatile SocketWearableConnection wearableConnection;

    BluetoothConnectionThread() {
    }

    protected void setWearableConnection(SocketWearableConnection connection) {
        SocketWearableConnection prev = this.wearableConnection;
        this.wearableConnection = connection;
        if (prev != null && prev != connection) {
            try {
                prev.close();
            } catch (IOException e) {
                Log.w(TAG, "Failed to close previous wearable connection", e);
            }
        }
    }

    public SocketWearableConnection getWearableConnection() {
        return wearableConnection;
    }

    public abstract void close();

    private static Socket proxySocket(final BluetoothSocket btSocket) {
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
                return btSocket.isConnected();
            }

            @Override
            public boolean isClosed() {
                return !btSocket.isConnected();
            }

            @Override
            public synchronized void close() throws IOException {
                btSocket.close();
            }
        };
    }

    /**
     * Listen for incoming RFCOMM connections on {@code uuid} (phone-as-server roles such as
     * Flow / Flow15).
     */
    @SuppressLint("MissingPermission")
    public static BluetoothConnectionThread serverListen(
            BluetoothAdapter adapter, String serviceName, UUID uuid,
            WearableConnection.Listener listener) {
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
                    serverSocket = adapter.listenUsingRfcommWithServiceRecord(serviceName, uuid);
                    Log.d(TAG, "server: listening on " + serviceName + " (" + uuid + ")");
                    while (!Thread.interrupted()) {
                        BluetoothSocket btSocket;
                        try {
                            btSocket = serverSocket.accept();
                        } catch (IOException e) {
                            break;
                        }
                        if (btSocket == null || Thread.interrupted()) break;
                        try {
                            SocketWearableConnection conn =
                                    new SocketWearableConnection(proxySocket(btSocket), listener);
                            setWearableConnection(conn);
                            try {
                                conn.run();
                            } finally {
                                try {
                                    btSocket.close();
                                } catch (IOException e) {
                                    Log.w(TAG, "server: close error for accepted connection", e);
                                }
                            }
                        } catch (IOException e) {
                            Log.w(TAG, "server: error on accepted connection", e);
                        }
                    }
                } catch (IOException e) {
                    if (!Thread.interrupted()) {
                        Log.w(TAG, "server: socket error on " + serviceName, e);
                    }
                } finally {
                    close();
                }
            }
        };
    }

    /**
     * Connect to a remote wearable that advertises {@link #WEARABLE_BT_UUID}.
     */
    @SuppressLint("MissingPermission")
    public static BluetoothConnectionThread clientConnect(
            BluetoothDevice device, WearableConnection.Listener listener) {
        return clientConnect(device, WEARABLE_BT_UUID, listener);
    }

    @SuppressLint("MissingPermission")
    public static BluetoothConnectionThread clientConnect(
            BluetoothDevice device, UUID uuid, WearableConnection.Listener listener) {
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
                    s = device.createRfcommSocketToServiceRecord(uuid);
                    btSocket = s;
                    Log.d(TAG, "client: connecting to " + device.getAddress() + " via " + uuid);
                    s.connect();
                    SocketWearableConnection conn =
                            new SocketWearableConnection(proxySocket(s), listener);
                    setWearableConnection(conn);
                    conn.run();
                } catch (IOException e) {
                    if (!Thread.interrupted()) {
                        Log.w(TAG, "client: connection failed", e);
                    }
                } finally {
                    if (s != null) {
                        try {
                            s.close();
                        } catch (IOException e) {
                            Log.d(TAG, "client: close error in finally", e);
                        }
                    }
                    btSocket = null;
                }
            }
        };
    }
}
