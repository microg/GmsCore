package org.microg.gms.wearable.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class BluetoothClient implements Closeable {
    private static final String TAG = "GmsWearBtClient";

    private final Context context;
    private final BluetoothAdapter btAdapter;
    private final BroadcastReceiver btStateReceiver;
    private final BroadcastReceiver aclConnReceiver;

    private final Map<String, ConnectionConfiguration> configurations = new HashMap<>();
    private final Map<String, BluetoothConnectionThread> connections = new HashMap<>();

    private final WearableImpl wearableImpl;

    private final ScheduledExecutorService executor;
    private final BleDeviceDiscoverer bleDiscoverer;

    private volatile boolean isShutdown = false;

    public BluetoothClient(Context context, WearableImpl wearableImpl) {
        this.context = context;
        this.btAdapter = BluetoothAdapter.getDefaultAdapter();

        this.wearableImpl = wearableImpl;

        this.btStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    onBluetoothStateChanged(state);
                }
            }
        };

        this.aclConnReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {
                    BluetoothDevice device = intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        onAclConnected(device);
                    }
                }
            }
        };

        this.bleDiscoverer = new BleDeviceDiscoverer(context, btAdapter);
        this.executor = Executors.newScheduledThreadPool(2);

        context.registerReceiver(btStateReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        context.registerReceiver(aclConnReceiver,
                new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
    }

    public void addConfig(ConnectionConfiguration config) {
        if (isShutdown) {
            Log.w(TAG, "Client is shutdown, ignoring addConfig");
            return;
        }

        validateConfig(config);

        String address = config.address;

        synchronized (this) {
            if (configurations.containsKey(address)) {
                Log.d(TAG, "Configuration already exists for " + address + ", updating");

                configurations.put(address, config);

                BluetoothConnectionThread thread = connections.get(address);
                if (thread != null) {
                    if (thread.isConnectionHealthy()) {
                        Log.d(TAG, "Connection is healthy, ignoring retry");
                    } else {
                        Log.d(TAG, "Connection unhealthy, triggering retry");
                        thread.resetBackoffAndRetryConnection();
                    }
                } else {
                    startConnection(config);
                }

                return;
            }

            configurations.put(address, config);

            if (btAdapter != null && btAdapter.isEnabled()) {
                startConnection(config);
            } else {
                Log.w(TAG, "Bluetooth disabled, deferring connection");
            }
        }
    }

    public void removeConfig(ConnectionConfiguration config) {
        if (isShutdown) {
            return;
        }

        validateConfig(config);

        String address = config.address;
        Log.d(TAG, "Removing configuration for " + address);

        synchronized (this) {
            BluetoothConnectionThread thread = connections.get(address);
            if (thread != null) {
                thread.close();
                connections.remove(address);
            }

            configurations.remove(address);
        }
    }

    private void startConnection(ConnectionConfiguration config) {
        if (isShutdown) {
            return;
        }

        String address = config.address;

        synchronized (this) {
            if (connections.containsKey(address)) {
                Log.d(TAG, "Connection already active for " + address);
                return;
            }

            if (btAdapter == null || !btAdapter.isEnabled()) {
                Log.w(TAG, "Bluetooth not available, deferring connection");
                return;
            }

            Log.d(TAG, "Starting connection for " + address);

            BluetoothConnectionThread thread = new BluetoothConnectionThread(
                    context, config, btAdapter, wearableImpl, executor, bleDiscoverer
            );

            connections.put(address, thread);
            thread.start();
        }
    }

    private void onAclConnected(BluetoothDevice device) {
        String address = device.getAddress();

        synchronized (this) {
            ConnectionConfiguration config = configurations.get(address);
            if (config != null) {
                Log.d(TAG, "ACL_CONNECTED for configured device " + address +
                        ", attempting reconnection");

                BluetoothConnectionThread thread = connections.get(address);
                if (thread != null) {
                    thread.retryConnection();
                } else {
                    startConnection(config);
                }
            }
        }
    }

    private void onBluetoothStateChanged(int state) {
        Log.d(TAG, "Bluetooth state changed to " + state);

        synchronized (this) {
            if (state == BluetoothAdapter.STATE_ON) {
                for (ConnectionConfiguration config : configurations.values()) {
                    String address = config.address;
                    if (!connections.containsKey(address)) {
                        startConnection(config);
                    } else {
                        BluetoothConnectionThread thread = connections.get(address);
                        if (thread != null) {
                            thread.resetBackoffAndRetryConnection();
                        }
                    }
                }
            } else if (state == BluetoothAdapter.STATE_OFF) {
                if (btAdapter != null && btAdapter.isEnabled()) {
                    Log.d(TAG, "Ignoring STATE_OFF - adapter still enabled");
                    return;
                }

                for (BluetoothConnectionThread thread : connections.values()) {
                    thread.close();
                }
                connections.clear();
            }
        }
    }

    private static void validateConfig(ConnectionConfiguration config){
        if (config == null || config.address == null)
            throw new IllegalArgumentException("Invalid configuration: config or address is null");

        int type = config.type;
        if ( type != WearableImpl.TYPE_BLUETOOTH_RFCOMM && type != 5)
            throw new IllegalArgumentException("Invalid connection type: " + type);

        if (config.role != WearableImpl.ROLE_CLIENT)
            throw new IllegalArgumentException("Role is not client: " + config.role);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    public void close() {
        if (isShutdown) {
            return;
        }

        Log.d(TAG, "Shutting down BluetoothClient");
        isShutdown = true;

        synchronized (this) {
            for (BluetoothConnectionThread thread : connections.values()) {
                thread.close();
            }

            for (BluetoothConnectionThread thread : connections.values()) {
                try {
                    thread.join(5000);
                    if (thread.isAlive()) {
                        Log.w(TAG, "Thread did not stop in time: " + thread.getName());
                    }
                } catch (InterruptedException e) {
                    Log.w(TAG, "Interrupted while waiting for thread", e);
                }
            }

            connections.clear();
            configurations.clear();
        }

        bleDiscoverer.shutdown();

        executor.shutdownNow();

        try {
            context.unregisterReceiver(btStateReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Error unregistering btStateReceiver", e);
        }

        try {
            context.unregisterReceiver(aclConnReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Error unregistering aclConnectedReceiver", e);
        }

        Log.d(TAG, "BluetoothClient closed");
    }
}
