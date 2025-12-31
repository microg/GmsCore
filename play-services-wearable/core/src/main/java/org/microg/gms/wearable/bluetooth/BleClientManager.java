package org.microg.gms.wearable.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.google.android.gms.wearable.ConnectionConfiguration;

import org.microg.gms.wearable.WearableImpl;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

public class BleClientManager implements Closeable {
    private static final String TAG = "GmsWearBleClient";

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final Map<String, ConnectionConfiguration> configurations = new HashMap<>();
    private final Map<String, BleClientConnection> connections = new HashMap<>();
    private final BroadcastReceiver bluetoothStateReceiver;

    public BleClientManager(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        this.bluetoothStateReceiver = new BroadcastReceiver() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                    onBluetoothAdapterStateChanged(state);
                }
            }
        };

        context.registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    public void addConfiguration(ConnectionConfiguration config) {
        validateConfiguration(config);

        String address = config.address;
        Log.d(TAG, "Adding BLE client configuration for " + address);

        if (configurations.containsKey(address)) {
            Log.d(TAG, "Configuration already exists for " + address);
            return;
        }

        configurations.put(address, config);

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth not available, deferring BLE connection");
            return;
        }

        startConnection(config);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void removeConfiguration(ConnectionConfiguration config) {
        validateConfiguration(config);

        String address = config.address;
        Log.d(TAG, "Removing BLE client configuration for " + address);

        BleClientConnection connection = connections.get(address);
        if (connection != null) {
            connection.close();
            connections.remove(address);
        }

        configurations.remove(address);
    }

    private void startConnection(ConnectionConfiguration config) {
        String address = config.address;
        if (connections.containsKey(address)) {
            Log.d(TAG, "BLE connection already active for " + address);
            return;
        }

        Log.d(TAG, "Starting BLE connection for " + address);
        BleClientConnection connection = new BleClientConnection(context, config, bluetoothAdapter);
        connections.put(address, connection);
        connection.start();
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void onBluetoothAdapterStateChanged(int state) {
        Log.d(TAG, "Bluetooth adapter state changed to " + state);

        if (state == BluetoothAdapter.STATE_ON) {
            // Start all configured connections
            for (ConnectionConfiguration config : configurations.values()) {
                String address = config.address;
                if (!connections.containsKey(address)) {
                    startConnection(config);
                }
            }
        } else if (state == BluetoothAdapter.STATE_OFF) {
            // Close all connections
            Log.d(TAG, "Closing all BLE connections due to adapter off");
            for (BleClientConnection connection : connections.values()) {
                connection.close();
            }
            connections.clear();
        }
    }

    private static void validateConfiguration(ConnectionConfiguration config) {
        if (config == null || config.address == null) {
            throw new IllegalArgumentException("Invalid configuration");
        }

        if (config.type != WearableImpl.TYPE_BLE) {
            throw new IllegalArgumentException("Invalid connection type for BLE: " + config.type);
        }

        if (config.role != WearableImpl.ROLE_CLIENT) {
            throw new IllegalArgumentException("Invalid role for BLE client: " + config.role);
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void close() {
        Log.d(TAG, "Closing BleClientManager");

        try {
            context.unregisterReceiver(bluetoothStateReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Error unregistering receiver", e);
        }

        for (BleClientConnection connection : connections.values()) {
            connection.close();
        }
        connections.clear();
        configurations.clear();
    }

}
