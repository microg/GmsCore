/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.google.android.gms.wearable.ConnectionConfiguration;

import org.microg.gms.wearable.WearableImpl;
import org.microg.gms.wearable.WearableConnection;
import org.microg.gms.wearable.proto.RootMessage;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BluetoothServer implements Closeable {
    private static final String TAG = "GmsWearBtServer";

    // Use the standard Wear OS UUID
    private static final UUID WEAR_UUID = UUID.fromString("5e8945b0-9525-11e3-a5e2-0800200c9a66");

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final Map<String, BluetoothServerThread> servers = new HashMap<>();
    private final BroadcastReceiver bluetoothStateReceiver;

    public BluetoothServer(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        this.bluetoothStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                    onBluetoothAdapterStateChanged(state);
                }
            }
        };

        context.registerReceiver(bluetoothStateReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        Log.d(TAG, "BluetoothServerManager initialized");
    }

    /**
     * Add a Bluetooth server configuration
     */
    public void addConfiguration(ConnectionConfiguration config) {
        validateConfiguration(config);

        String name = config.name != null ? config.name : "WearServer";
        Log.d(TAG, "Adding Bluetooth server configuration: " + name);

        if (servers.containsKey(name)) {
            Log.d(TAG, "Server already exists: " + name);
            return;
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth not available, deferring server start");
            return;
        }

        startServer(config);
    }

    /**
     * Remove a Bluetooth server configuration
     */
    public void removeConfiguration(ConnectionConfiguration config) {
        validateConfiguration(config);

        String name = config.name != null ? config.name : "WearServer";
        Log.d(TAG, "Removing Bluetooth server configuration: " + name);

        BluetoothServerThread server = servers.get(name);
        if (server != null) {
            server.close();
            servers.remove(name);
        }
    }

    private void startServer(ConnectionConfiguration config) {
        String name = config.name != null ? config.name : "WearServer";

        if (servers.containsKey(name)) {
            Log.d(TAG, "Server already running: " + name);
            return;
        }

        Log.d(TAG, "Starting Bluetooth server: " + name);
        BluetoothServerThread server = new BluetoothServerThread(context, config, bluetoothAdapter);
        servers.put(name, server);
        server.start();
    }

    private void onBluetoothAdapterStateChanged(int state) {
        Log.d(TAG, "Bluetooth adapter state changed to " + state);

        if (state == BluetoothAdapter.STATE_OFF) {
            // Bluetooth turned off, close all servers
            Log.d(TAG, "Closing all Bluetooth servers due to adapter off");
            for (BluetoothServerThread server : servers.values()) {
                server.close();
            }
            servers.clear();
        }
        // Note: We don't auto-restart servers on STATE_ON
        // The user/system must explicitly re-enable the configuration
    }

    private static void validateConfiguration(ConnectionConfiguration config) {
        if (config == null) {
            throw new IllegalArgumentException("Invalid configuration: null");
        }

        int type = config.type;
        if (type != WearableImpl.TYPE_BLUETOOTH_RFCOMM && type != 5) {
            throw new IllegalArgumentException("Invalid connection type for Bluetooth server: " + type);
        }

        if (config.role != WearableImpl.ROLE_SERVER) {
            throw new IllegalArgumentException("Invalid role for server: " + config.role);
        }
    }

    @Override
    public void close() {
        Log.d(TAG, "Closing BluetoothServerManager");

        try {
            context.unregisterReceiver(bluetoothStateReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Error unregistering receiver", e);
        }

        for (BluetoothServerThread server : servers.values()) {
            server.close();
        }
        servers.clear();
    }

    /**
     * Individual server thread that accepts incoming connections
     */
    private static class BluetoothServerThread extends Thread implements Closeable {
        private static final String TAG = "GmsWearBtSrvThread";
        private static final int MAX_RETRY_COUNT = 3;
        private static final int RETRY_DELAY_MS = 5000;

        private final Context context;
        private final ConnectionConfiguration config;
        private final BluetoothAdapter bluetoothAdapter;
        private volatile boolean running = true;
        private BluetoothServerSocket serverSocket;
        private int retryCount = 0;

        public BluetoothServerThread(Context context, ConnectionConfiguration config, BluetoothAdapter adapter) {
            super("BtServerThread-" + (config.name != null ? config.name : "default"));
            this.context = context;
            this.config = config;
            this.bluetoothAdapter = adapter;
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void run() {
            String name = config.name != null ? config.name : "WearServer";
            Log.d(TAG, "Bluetooth server thread started: " + name);

            while (running && !isInterrupted()) {
                try {
                    // Create server socket
                    if (serverSocket == null) {
                        createServerSocket();
                    }

                    if (serverSocket != null) {
                        acceptConnection();
                        retryCount = 0; // Reset on successful accept
                    }

                } catch (IOException e) {
                    Log.w(TAG, "Server socket error: " + e.getMessage());
                    closeServerSocket();

                    if (running && retryCount < MAX_RETRY_COUNT) {
                        retryCount++;
                        Log.d(TAG, "Retrying server socket creation (attempt " + retryCount + "/" + MAX_RETRY_COUNT + ")");
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    } else if (retryCount >= MAX_RETRY_COUNT) {
                        Log.e(TAG, "Max retry count reached, stopping server");
                        break;
                    }
                }
            }

            closeServerSocket();
            Log.d(TAG, "Bluetooth server thread stopped: " + name);
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        private void createServerSocket() throws IOException {
            String name = config.name != null ? config.name : "WearServer";

            if (!running || bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                throw new IOException("Bluetooth not available");
            }

            Log.d(TAG, "Creating server socket for " + name + " via " + getConnectionTypeName());

            if (config.type != WearableImpl.TYPE_BLUETOOTH_RFCOMM && config.type != 5) {
                return;
            }
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(name, WEAR_UUID);
            Log.d(TAG, "RFCOMM server socket created on UUID: " + WEAR_UUID);

        }

        private void acceptConnection() throws IOException {
            if (serverSocket == null) {
                throw new IOException("Server socket is null");
            }

            Log.d(TAG, "Waiting for incoming connection...");

            // This blocks until a connection is made
            BluetoothSocket clientSocket = serverSocket.accept();

            if (clientSocket != null) {
                Log.d(TAG, "Client connected from: " + clientSocket.getRemoteDevice().getAddress());
                handleConnection(clientSocket);
            }
        }

        private void handleConnection(BluetoothSocket clientSocket) {
            // Spawn a new thread to handle this connection
            // so we can go back to accepting new connections
            new Thread(() -> {
                try {
                    Log.d(TAG, "Handling connection from " + clientSocket.getRemoteDevice().getAddress());

                    BluetoothWearableConnection connection = new BluetoothWearableConnection(
                            clientSocket, config.nodeId, new ServerConnectionListener(context, config, clientSocket));
                    connection.run(); // Blocks until connection closes

                } catch (IOException e) {
                    Log.w(TAG, "Error handling connection: " + e.getMessage());
                } finally {
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Error closing client socket", e);
                    }
                }
            }, "BtServerConn-" + clientSocket.getRemoteDevice().getAddress()).start();
        }

        private void closeServerSocket() {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    Log.w(TAG, "Error closing server socket", e);
                }
                serverSocket = null;
            }
        }

        private String getConnectionTypeName() {
            if (config.type == WearableImpl.TYPE_BLUETOOTH_RFCOMM) {
                return "RFCOMM";
            } else if (config.type == 5) {
                return "RFCOMM maybe";
            }
            return "Unknown";
        }

        @Override
        public void close() {
            Log.d(TAG, "Closing Bluetooth server");
            running = false;
            interrupt();
            closeServerSocket();
        }

        private static class ServerConnectionListener implements WearableConnection.Listener {
            private final Context context;
            private final ConnectionConfiguration config;
            private final BluetoothSocket socket;

            public ServerConnectionListener(Context context, ConnectionConfiguration config, BluetoothSocket socket) {
                this.context = context;
                this.config = config;
                this.socket = socket;
            }

            @Override
            public void onConnected(WearableConnection connection) {
                Log.d(TAG, "Server connection established with " + socket.getRemoteDevice().getAddress());
                // TODO: Notify WearableImpl about connection
            }

            @Override
            public void onMessage(WearableConnection connection, RootMessage message) {
                Log.d(TAG, "Server received message from " + socket.getRemoteDevice().getAddress());
                // TODO: Handle incoming messages
            }

            @Override
            public void onDisconnected() {
                Log.d(TAG, "Server connection disconnected from " + socket.getRemoteDevice().getAddress());
                // TODO: Notify WearableImpl about disconnection
            }
        }
    }
}