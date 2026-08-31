/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.wearable.ConnectionConfiguration;

import org.microg.gms.wearable.MessageHandler;
import org.microg.gms.wearable.WearableConnectionKind;
import org.microg.gms.wearable.WearableImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Starts Wear OS RFCOMM listeners (watch connects to the phone) and optional outbound
 * reconnects to a bonded MAC. Each accepted socket is handed to {@link BluetoothWearableConnection}
 * using the existing Wearable v2 {@link MessageHandler}.
 */
public class BluetoothConnectionManager {
    private static final String TAG = "GmsWearBt";
    private static final long RECONNECT_MS = 10_000L;

    private final Context context;
    private final WearableImpl wearable;
    private final List<AcceptThread> acceptThreads = new ArrayList<>();
    private final Map<String, OutboundThread> outboundThreads = new HashMap<>();

    public BluetoothConnectionManager(Context context, WearableImpl wearable) {
        this.context = context;
        this.wearable = wearable;
    }

    public synchronized void ensureStarted(ConnectionConfiguration config) {
        if (!hasBluetoothConnectPermission()) {
            Log.w(TAG, "BLUETOOTH_CONNECT not granted; cannot start Wear OS RFCOMM");
            return;
        }
        startListeners();
        if (WearableConnectionKind.isBluetoothAddress(config.address)) {
            startOutbound(config);
        }
    }

    public synchronized void stop(String name) {
        OutboundThread outbound = outboundThreads.remove(name);
        if (outbound != null) {
            outbound.shutdown();
        }
        boolean anyBluetooth = false;
        ConnectionConfiguration[] configs = wearable.getConfigurations();
        if (configs != null) {
            for (ConnectionConfiguration config : configs) {
                if (config.enabled && WearableConnectionKind.isBluetooth(config)) {
                    anyBluetooth = true;
                    break;
                }
            }
        }
        if (!anyBluetooth) {
            stopListeners();
        }
    }

    public synchronized void stopAll() {
        for (OutboundThread outbound : outboundThreads.values()) {
            outbound.shutdown();
        }
        outboundThreads.clear();
        stopListeners();
    }

    @SuppressLint("MissingPermission")
    private void startListeners() {
        if (!acceptThreads.isEmpty()) {
            return;
        }
        BluetoothAdapter adapter = bluetoothAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Log.w(TAG, "Bluetooth adapter missing or disabled");
            return;
        }
        for (WearableBtUuids.NamedUuid named : WearableBtUuids.ALL) {
            AcceptThread thread = new AcceptThread(adapter, named.name, named.uuid);
            acceptThreads.add(thread);
            thread.start();
        }
    }

    private void stopListeners() {
        for (AcceptThread thread : acceptThreads) {
            thread.shutdown();
        }
        acceptThreads.clear();
    }

    private void startOutbound(ConnectionConfiguration config) {
        if (outboundThreads.containsKey(config.name)) {
            return;
        }
        BluetoothAdapter adapter = bluetoothAdapter();
        if (adapter == null) {
            return;
        }
        OutboundThread thread = new OutboundThread(adapter, config);
        outboundThreads.put(config.name, thread);
        thread.start();
    }

    private BluetoothAdapter bluetoothAdapter() {
        if (Build.VERSION.SDK_INT >= 18) {
            BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (manager != null) {
                return manager.getAdapter();
            }
        }
        return BluetoothAdapter.getDefaultAdapter();
    }

    private boolean hasBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT < 31) {
            return true;
        }
        return context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private void attachSocket(BluetoothSocket socket, ConnectionConfiguration config) {
        try {
            MessageHandler handler = new MessageHandler(context, wearable, config);
            BluetoothWearableConnection connection = new BluetoothWearableConnection(socket, handler);
            Thread thread = new Thread(connection, "WearBtConn-" + safeAddress(socket));
            thread.start();
            Log.d(TAG, "Wearable RFCOMM session started for " + config);
        } catch (IOException e) {
            Log.w(TAG, "Failed to wrap Bluetooth socket", e);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    @SuppressLint("MissingPermission")
    private static String safeAddress(BluetoothSocket socket) {
        try {
            BluetoothDevice device = socket.getRemoteDevice();
            return device != null ? device.getAddress() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private ConnectionConfiguration configForInbound(BluetoothSocket socket) {
        String address = safeAddress(socket);
        ConnectionConfiguration[] configs = wearable.getConfigurations();
        if (configs != null) {
            for (ConnectionConfiguration config : configs) {
                if (address.equalsIgnoreCase(config.address)) {
                    return config;
                }
            }
            for (ConnectionConfiguration config : configs) {
                if (WearableConnectionKind.isBluetooth(config)) {
                    return config;
                }
            }
        }
        return new ConnectionConfiguration("bluetooth", address, WearableConnectionKind.TYPE_BLUETOOTH, 2, true);
    }

    private class AcceptThread extends Thread {
        private final BluetoothAdapter adapter;
        private final String serviceName;
        private final UUID uuid;
        private BluetoothServerSocket serverSocket;
        private volatile boolean running = true;

        AcceptThread(BluetoothAdapter adapter, String serviceName, UUID uuid) {
            super("WearBtListen-" + serviceName);
            this.adapter = adapter;
            this.serviceName = serviceName;
            this.uuid = uuid;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            try {
                serverSocket = adapter.listenUsingRfcommWithServiceRecord(serviceName, uuid);
            } catch (IOException e) {
                Log.w(TAG, "listenUsingRfcommWithServiceRecord failed for " + serviceName, e);
                try {
                    serverSocket = adapter.listenUsingInsecureRfcommWithServiceRecord(serviceName, uuid);
                } catch (IOException e2) {
                    Log.w(TAG, "insecure listen failed for " + serviceName, e2);
                    return;
                }
            }
            Log.d(TAG, "Listening for Wear OS on " + serviceName + " " + uuid);
            while (running) {
                try {
                    BluetoothSocket socket = serverSocket.accept();
                    if (socket == null) continue;
                    Log.d(TAG, "Accepted Wear OS socket on " + serviceName + " from " + safeAddress(socket));
                    attachSocket(socket, configForInbound(socket));
                } catch (IOException e) {
                    if (running) {
                        Log.w(TAG, "accept() failed for " + serviceName, e);
                    }
                    break;
                }
            }
        }

        void shutdown() {
            running = false;
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException ignored) {
                }
            }
            interrupt();
        }
    }

    private class OutboundThread extends Thread {
        private final BluetoothAdapter adapter;
        private final ConnectionConfiguration config;
        private volatile boolean running = true;
        private BluetoothSocket socket;

        OutboundThread(BluetoothAdapter adapter, ConnectionConfiguration config) {
            super("WearBtOut-" + config.address);
            this.adapter = adapter;
            this.config = config;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            BluetoothDevice device;
            try {
                device = adapter.getRemoteDevice(config.address);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Invalid Bluetooth address " + config.address, e);
                return;
            }
            while (running) {
                BluetoothSocket connected = connect(device);
                if (!running) {
                    closeQuietly(connected);
                    return;
                }
                if (connected != null) {
                    socket = connected;
                    Log.d(TAG, "Connected outbound RFCOMM to " + config.address);
                    try {
                        MessageHandler handler = new MessageHandler(context, wearable, config);
                        new BluetoothWearableConnection(connected, handler).run();
                    } catch (IOException e) {
                        Log.w(TAG, "Outbound Wear OS session ended", e);
                    }
                    socket = null;
                }
                if (!running) return;
                try {
                    Thread.sleep(RECONNECT_MS);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        @SuppressLint("MissingPermission")
        private BluetoothSocket connect(BluetoothDevice device) {
            adapter.cancelDiscovery();
            for (WearableBtUuids.NamedUuid named : WearableBtUuids.ALL) {
                BluetoothSocket attempt = tryConnect(device, named.uuid, false);
                if (attempt != null) return attempt;
                attempt = tryConnect(device, named.uuid, true);
                if (attempt != null) return attempt;
            }
            return null;
        }

        @SuppressLint("MissingPermission")
        private BluetoothSocket tryConnect(BluetoothDevice device, UUID uuid, boolean insecure) {
            BluetoothSocket attempt = null;
            try {
                attempt = insecure
                        ? device.createInsecureRfcommSocketToServiceRecord(uuid)
                        : device.createRfcommSocketToServiceRecord(uuid);
                attempt.connect();
                return attempt;
            } catch (IOException e) {
                closeQuietly(attempt);
                return null;
            }
        }

        void shutdown() {
            running = false;
            closeQuietly(socket);
            interrupt();
        }
    }

    private static void closeQuietly(BluetoothSocket socket) {
        if (socket == null) return;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
