package org.microg.gms.wearable.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.google.android.gms.wearable.ConnectionConfiguration;

import org.microg.gms.wearable.WearableImpl;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

public class BluetoothClient implements Closeable {
    private static final String TAG = "GmsWearBtClient";

    private final Context context;
    private final BluetoothAdapter btAdapter;
    private final BroadcastReceiver btStateReceiver;
    private final BroadcastReceiver aclConnReceiver;

    private final Map<String, ConnectionConfiguration> configurations = new HashMap<>();
    private final Map<String, BluetoothConnectionThread> connections = new HashMap<>();

    private final WearableImpl wearableImpl;

    private volatile boolean isShutdown = false;

    public BluetoothClient(Context context, WearableImpl wearableImpl) {
        this.context = context;
        this.btAdapter = BluetoothAdapter.getDefaultAdapter();

        this.wearableImpl = wearableImpl;

        this.btStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    onAdapterStateChanged(state);

                } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        onAclConnected(device);
                    }

                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        onAclDisconnected(device.getAddress());
                    }
                }
            }
        };

        this.aclConnReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) onAclConnected(device);
                }
            }
        };

        context.registerReceiver(btStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        context.registerReceiver(aclConnReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
    }

    private void onAdapterStateChanged(int state) {
        Log.d(TAG, "Bluetooth adapter state changed to " + state);

        if (state == BluetoothAdapter.STATE_ON) {
            for (BluetoothConnectionThread thread : connections.values()) {
                thread.resetBackoff();
                thread.scheduleRetry();
            }
        } else if (state == BluetoothAdapter.STATE_OFF) {
            if (btAdapter != null && btAdapter.isEnabled()) {
                Log.d(TAG, "Ignoring STATE_OFF - adapter still enabled (stale broadcast)");
                return;
            }
            for (BluetoothConnectionThread thread : new HashMap<>(connections).values()) {
                thread.close();
            }
        }
    }


    public void addConfig(ConnectionConfiguration config) {
        if (isShutdown) {
            Log.w(TAG, "BluetoothClient is shutdown, ignoring addConfig");
            return;
        }
        validateConfig(config);

        if (btAdapter == null || !btAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth not enabled, deferring connection");
            configurations.put(config.address, config);
            return;
        }

        String address = config.address;

        if (configurations.containsKey(address)) {
            Log.d(TAG, "Configuration already exists for " + address + ", reconnecting");

            configurations.put(address, config);

            BluetoothConnectionThread thread = connections.get(address);

            if (thread != null) {
                if (thread.isConnectionHealthy()) {
                    Log.d(TAG, "Connection is active for " + address + ", ignoring retry");
                    return;
                }

                if (isThreadActive(thread)) {
                    Log.d(TAG, "Thread active but connection unhealthy, triggering retry");
                    thread.retryConnection();
                } else {
                    Log.d(TAG, "Thread not active, starting new connection");
                    connections.remove(address);
                    startConnection(config);
                }
            }

            return;
        }

        configurations.put(address, config);
        startConnection(config);

    }

    public void removeConfig(ConnectionConfiguration config) {
        if (isShutdown) {
            return;
        }

        validateConfig(config);

        String address = config.address;
        Log.d(TAG, "Removing configuration for " + address);

        BluetoothConnectionThread thread = connections.get(address);
        if (thread != null) {
            thread.close();
            connections.remove(address);
        }

        configurations.remove(address);
    }

    private void startConnection(ConnectionConfiguration config) {
        if (isShutdown) {
            return;
        }

        String address = config.address;

        if (connections.containsKey(address)) {
            Log.d(TAG, "Connection already active for " + address);
            return;
        }

        if (btAdapter == null || !btAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth not available, deferring connection for " + address);
            return;
        }

        Log.d(TAG, "Starting Bluetooth connection for " + address);
        BluetoothConnectionThread thread = new BluetoothConnectionThread(context, config, btAdapter, wearableImpl);
        connections.put(address, thread);
        thread.start();
    }

    private void onAclConnected(BluetoothDevice device) {
        String address = device.getAddress();
        ConnectionConfiguration config = configurations.get(address);
        if (config != null) {
            Log.d(TAG, "ACL_CONNECTED for configured device " + address + ", attempting reconnection");
            retryConnection(config, false);
        }
    }

    private void onAclDisconnected(String address) {
        Log.d(TAG, "ACL_DISCONNECTED for " + address);
    }

    private void onBtAdapterStateChaged(int state) {
        Log.d(TAG, "Bluetooth adapter state changed to " + state);

        if (state == BluetoothAdapter.STATE_ON) {
            for (ConnectionConfiguration config: configurations.values()) {
                String address = config.address;
                if (!connections.containsKey(address))
                    startConnection(config);
            }
        } else if (state == BluetoothAdapter.STATE_OFF) {
            if (btAdapter != null && btAdapter.isEnabled()) {
                Log.d(TAG, "Ignoring STATE_OFF broadcast - adapter is still enabled");
                return;
            }
            for (BluetoothConnectionThread thread : connections.values()) {
                thread.close();
            }
            connections.clear();
        }
    }

    public void retryConnection(ConnectionConfiguration config, boolean immediate) {
        if (isShutdown) {
            return;
        }
        validateConfig(config);

        String address = config.address;
        if (!configurations.containsKey(address)) {
            Log.w(TAG, "Configuration not found for " + address);
            return;
        }

        if (btAdapter == null || !btAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth not enabled, cannot retry connection");
            return;
        }

        BluetoothConnectionThread thread = connections.get(address);

        if (thread == null) {
            Log.d(TAG, "No connection thread exists for " + address + ", starting new one");
            startConnection(config);
            return;
        }

        if (!isThreadActive(thread)) {
            Log.d(TAG, "Connection thread not active for " + address + ", starting new one");
            connections.remove(address);
            startConnection(config);
            return;
        }

        if (immediate) {
            thread.retryConnection();
        } else {
            thread.scheduleRetry();
        }

    }

    private boolean isThreadActive(BluetoothConnectionThread thread) {
        return thread != null && thread.isAlive() && !thread.isInterrupted();
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

    @Override
    public void close() {
        if (isShutdown) {
            return;
        }

        isShutdown = true;

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
                Log.w(TAG, "Interrupted while waiting for thread to finish", e);
            }
        }

        connections.clear();
        configurations.clear();

        try {
            context.unregisterReceiver(btStateReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Error unregistering btStateReceiver", e);
        }

        try {
            context.unregisterReceiver(aclConnReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Error unregistering aclConnReceiver", e);
        }

        Log.d(TAG, "BluetoothClient closed");
    }
}
