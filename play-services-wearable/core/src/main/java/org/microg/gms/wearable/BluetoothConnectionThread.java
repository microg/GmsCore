/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import org.microg.wearable.StreamWearableConnection;
import org.microg.wearable.WearableConnection;

import java.io.IOException;
import java.util.UUID;

/**
 * Thread managing Android Bluetooth RFCOMM transport for Wearable connections.
 * Provides both server listening for incoming smartwatch connections and
 * active client connection to bonded WearOS devices.
 */
public abstract class BluetoothConnectionThread extends Thread {
    private static final String TAG = "GmsWearBtThread";

    // Standard Android Wear / WearOS RFCOMM Service Record UUID (Clockwork / Wearable sync)
    public static final UUID WEAR_RFCOMM_UUID = UUID.fromString("0000DA28-0000-1000-8000-00805F9B34FB");
    // Standard Serial Port Profile (SPP) fallback UUID
    public static final UUID SPP_FALLBACK_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String WEAR_SERVICE_NAME = "Android Wear";

    private StreamWearableConnection wearableConnection;

    protected BluetoothConnectionThread(String threadName) {
        super(threadName);
    }

    protected void setWearableConnection(StreamWearableConnection wearableConnection) {
        this.wearableConnection = wearableConnection;
    }

    public StreamWearableConnection getWearableConnection() {
        return wearableConnection;
    }

    public abstract void close();

    /**
     * Start listening for incoming RFCOMM connections from smartwatches.
     */
    public static BluetoothConnectionThread serverListen(final BluetoothAdapter adapter, final WearableConnection.Listener listener) {
        return new BluetoothConnectionThread("WearBtServer") {
            private BluetoothServerSocket serverSocket = null;
            private volatile boolean closed = false;

            @Override
            public void close() {
                closed = true;
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException ignored) {
                    }
                    serverSocket = null;
                }
            }

            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                if (adapter == null || !adapter.isEnabled()) {
                    Log.w(TAG, "Bluetooth adapter not available or disabled, aborting server listen");
                    return;
                }

                try {
                    // Try with standard Wearable UUID first, then fallback to SPP
                    try {
                        serverSocket = adapter.listenUsingRfcommWithServiceRecord(WEAR_SERVICE_NAME, WEAR_RFCOMM_UUID);
                    } catch (IOException e) {
                        Log.d(TAG, "Failed listening with primary Wearable UUID, falling back to SPP", e);
                        serverSocket = adapter.listenUsingRfcommWithServiceRecord(WEAR_SERVICE_NAME, SPP_FALLBACK_UUID);
                    }

                    Log.d(TAG, "Bluetooth RFCOMM server listening for WearOS connections...");
                    while (!closed && !Thread.interrupted()) {
                        BluetoothSocket socket = serverSocket.accept();
                        if (socket != null) {
                            Log.d(TAG, "Accepted incoming Bluetooth connection from: " + socket.getRemoteDevice());
                            StreamWearableConnection connection = new StreamWearableConnection(
                                    socket.getInputStream(),
                                    socket.getOutputStream(),
                                    socket,
                                    listener
                            );
                            setWearableConnection(connection);
                            connection.run(); // Runs until link disconnects or errors
                        }
                    }
                } catch (IOException e) {
                    if (!closed) {
                        Log.w(TAG, "Bluetooth server accept error", e);
                    }
                } finally {
                    close();
                }
            }
        };
    }

    /**
     * Actively connect to a bonded WearOS smartwatch by MAC address.
     */
    public static BluetoothConnectionThread clientConnect(final BluetoothAdapter adapter, final String address, final WearableConnection.Listener listener) {
        return new BluetoothConnectionThread("WearBtClient-" + address) {
            private BluetoothSocket socket = null;
            private volatile boolean closed = false;

            @Override
            public void close() {
                closed = true;
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                    socket = null;
                }
            }

            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                if (adapter == null || !adapter.isEnabled()) {
                    Log.w(TAG, "Bluetooth adapter not available, cannot connect to " + address);
                    return;
                }

                try {
                    BluetoothDevice device = adapter.getRemoteDevice(address);
                    Log.d(TAG, "Connecting to WearOS device: " + device.getName() + " [" + address + "]");

                    // Cancel discovery before connecting to prevent bandwidth reduction
                    if (adapter.isDiscovering()) {
                        adapter.cancelDiscovery();
                    }

                    // Attempt primary connection via Wearable UUID
                    try {
                        socket = device.createRfcommSocketToServiceRecord(WEAR_RFCOMM_UUID);
                        socket.connect();
                    } catch (IOException e) {
                        Log.d(TAG, "Primary RFCOMM connection failed, trying fallback SPP socket", e);
                        try {
                            if (socket != null) socket.close();
                        } catch (IOException ignored) {
                        }
                        socket = device.createRfcommSocketToServiceRecord(SPP_FALLBACK_UUID);
                        socket.connect();
                    }

                    Log.d(TAG, "Connected to WearOS device via Bluetooth RFCOMM!");
                    StreamWearableConnection connection = new StreamWearableConnection(
                            socket.getInputStream(),
                            socket.getOutputStream(),
                            socket,
                            listener
                    );
                    setWearableConnection(connection);
                    connection.run();
                } catch (IOException e) {
                    if (!closed) {
                        Log.w(TAG, "Failed connecting to WearOS Bluetooth device " + address, e);
                    }
                } finally {
                    close();
                }
            }
        };
    }
}
