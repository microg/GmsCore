package org.microg.gms.wearable.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.android.gms.wearable.ConnectionConfiguration;

import org.microg.gms.wearable.WearableImpl;
import org.microg.gms.wearable.proto.RootMessage;

import java.util.HashMap;
import java.util.Map;

public class NetworkConnectionManager implements Cloneable {
    private static final String TAG = "GmsWearNetMgr";

    private static final long SHUTDOWN_JOIN_TIMEOUT = 5000;

    private final Context context;
    private final WearableImpl wearable;

    private final Map<String, NetworkConnectionThread> threads = new HashMap<>();
    private final Map<String, ConnectionConfiguration> configs = new HashMap<>();

    private final BroadcastReceiver connectivityReceiver;
    private volatile boolean shutdown = false;

    public NetworkConnectionManager(Context context, WearableImpl wearable) {
        this.context = context;
        this.wearable = wearable;

        connectivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!ConnectivityManager.EXTRA_NO_CONNECTIVITY.equals(intent.getAction())) {
                    return;
                }

                if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
                    return;
                }

                onNetworkAvailable();
            }
        };

        context.registerReceiver(
                connectivityReceiver,
                new IntentFilter(ConnectivityManager.EXTRA_NO_CONNECTIVITY)
        );

        Log.d(TAG, "initialised");
    }

    public synchronized void addConfig(ConnectionConfiguration config) {
        if (shutdown) {
            Log.w(TAG, "Manager is shut down, ignoring addConfig for " + config.address);
            return;
        }
        validateConfig(config);

        String addr = config.address;
        configs.put(addr, config);

        NetworkConnectionThread existing = threads.get(addr);
        if (existing != null) {
            if (existing.isAlive() && !existing.isInterrupted()) {
                Log.d(TAG, "Thread already active for " + addr + ", triggering retry");
                existing.triggerRetry();
            } else {
                Log.d(TAG, "Replacing dead thread for " + addr);
                existing.close();
                threads.remove(addr);
                startThread(config);
            }
            return;
        }

        if (isNetworkAvailable()) {
            startThread(config);
        } else {
            Log.d(TAG, "Network unavailable, deferring connection for " + addr);
        }
    }

    public synchronized void removeConfig(ConnectionConfiguration config) {
        if (shutdown) return;
        validateConfig(config);

        String addr = config.address;
        configs.remove(addr);

        NetworkConnectionThread t = threads.remove(addr);
        if (t != null) {
            Log.d(TAG, "Removing thread for " + addr);
            t.close();
            joinThread(t, addr);
        }
    }

    public synchronized void sendMessage(String address, RootMessage message) {
        NetworkConnectionThread t = threads.get(address);
        if (t == null || !t.isAlive()) {
            throw new IllegalArgumentException("No active connection for " + address);
        }
        t.sendMessage(message);
    }

    public synchronized boolean hasConnection(String address) {
        NetworkConnectionThread t = threads.get(address);
        return t != null && t.isAlive() && !t.isInterrupted();
    }

    private void onNetworkAvailable() {
        synchronized (this) {
            if (shutdown) return;
            Log.d(TAG, "Network available — checking " + configs.size() + " config(s)");

            for (Map.Entry<String, ConnectionConfiguration> entry : configs.entrySet()) {
                String addr   = entry.getKey();
                NetworkConnectionThread t = threads.get(addr);

                if (t == null || !t.isAlive()) {
                    Log.d(TAG, "Starting thread for " + addr + " (network regained)");
                    if (t != null) threads.remove(addr);
                    startThread(entry.getValue());
                } else {
                    t.triggerRetry();
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    public synchronized void close() {
        if (shutdown) return;
        shutdown = true;
        Log.d(TAG, "Shutting down NetworkConnectionManager (" + threads.size() + " thread(s))");

        try {
            context.unregisterReceiver(connectivityReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Error unregistering connectivity receiver", e);
        }

        for (NetworkConnectionThread t : threads.values()) t.close();
        for (Map.Entry<String, NetworkConnectionThread> e : threads.entrySet()) {
            joinThread(e.getValue(), e.getKey());
        }

        threads.clear();
        configs.clear();
        Log.d(TAG, "NetworkConnectionManager shut down");
    }

    private void startThread(ConnectionConfiguration config) {
        NetworkConnectionThread t =
                new NetworkConnectionThread(context, config, wearable);
        threads.put(config.address, t);
        t.start();
        Log.d(TAG, "Started NetworkConnectionThread for " + config.address);
    }

    private static void joinThread(NetworkConnectionThread t, String address) {
        try {
            t.join(SHUTDOWN_JOIN_TIMEOUT);
            if (t.isAlive()) {
                Log.w(TAG, "Thread for " + address + " did not stop within "
                        + SHUTDOWN_JOIN_TIMEOUT + "ms");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void validateConfig(ConnectionConfiguration config) {
        if (config == null || config.address == null) {
            throw new IllegalArgumentException("Config or address is null");
        }
        if (config.type != WearableImpl.TYPE_NETWORK) {
            throw new IllegalArgumentException("Expected TYPE_NETWORK, got type=" + config.type);
        }
        if (config.role != WearableImpl.ROLE_CLIENT) {
            throw new IllegalArgumentException("Expected ROLE_CLIENT, got role=" + config.role);
        }
    }


}
