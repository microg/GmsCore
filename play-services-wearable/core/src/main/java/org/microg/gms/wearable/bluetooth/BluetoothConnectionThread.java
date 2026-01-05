package org.microg.gms.wearable.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.google.android.gms.wearable.ConnectionConfiguration;

import org.microg.gms.wearable.WearableConnection;
import org.microg.gms.wearable.WearableImpl;
import org.microg.gms.wearable.proto.Connect;
import org.microg.gms.wearable.proto.RootMessage;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BluetoothConnectionThread extends Thread implements Closeable {
    private static final String TAG = "GmsWearBtConnThread";

    private static final UUID WEAR_BT_UUID = UUID.fromString("5e8945b0-9525-11e3-a5e2-0800200c9a66");

    private static final int MAX_RETRY_DELAY_MS = 60000;
    private static final int MIN_RETRY_DELAY_MS = 1000;
    private static final int BACKOFF_MULTIPLIEER = 2;
    private static final int MAX_CONSECUTIVE_FAILURES = 5;
    private static final long MIN_ATTEMPT_INTERVAL_MS = 3000;

    private final Context context;
    private final ConnectionConfiguration config;
    private final BluetoothAdapter btAdapter;
    private final Handler retryHandler;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private final AtomicBoolean immediateRetry = new AtomicBoolean(false);

    private long lastAttemptTime = 0;

    private BluetoothSocket socket;
    private WearableConnection wearableConnection;

    private final WearableImpl wearableImpl;

    public BluetoothConnectionThread(Context context, ConnectionConfiguration config, BluetoothAdapter btAdapter, WearableImpl wearableImpl) {
        super("BtThread-" + config.address);
        this.context = context;
        this.config = config;
        this.btAdapter = btAdapter;
        this.wearableImpl = wearableImpl;
        this.retryHandler = new Handler(Looper.getMainLooper());
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    @Override
    public void run(){
        Log.d(TAG, "Bluetooth connection thread started for " + config.address);

        while (running.get() && !isInterrupted()) {
            enforceMinInterval();

            if (!running.get()) break;

            try {
                connect();
            } catch (IOException e) {
                Log.w(TAG, "Connection failed for " + config.address + ": " + e.getMessage());
                closeSocket();

                if (running.get()) {
                    try {
                        waitForRetry();
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "Connection thread interrupted");
                break;
            }
        }

        closeSocket();
        Log.d(TAG, "Bluetooth connection thread stopped for " + config.address);
    }

    private void enforceMinInterval() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastAttemptTime;

        if (elapsed < MIN_ATTEMPT_INTERVAL_MS && lastAttemptTime > 0) {
            long sleepTime = MIN_ATTEMPT_INTERVAL_MS - elapsed;
            Log.d(TAG, "Enforcing min interval, sleeping " + sleepTime + "ms");
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                if (!running.get()) return;
            }
        }

        lastAttemptTime = System.currentTimeMillis();
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    private void connect() throws IOException, InterruptedException {
        if (!WearableImpl.BluetoothConnectionLock.tryAcquire(config.address, "BTLock")) {
            throw new IOException("Connection lock held by another app");
        }

        try {

            if (!running.get() || btAdapter == null || !btAdapter.isEnabled()) {
                throw new IOException("Bluetooth not available");
            }

            BluetoothDevice device = btAdapter.getRemoteDevice(config.address);
            if (device == null) throw new IOException("Could not get remote device");

            Log.d(TAG, "Connecting to " + config.address + " via " + getConnectionTypeName());

            if (config.type != WearableImpl.TYPE_BLUETOOTH_RFCOMM && config.type != 5) {
                return;
            }

            socket = device.createRfcommSocketToServiceRecord(WEAR_BT_UUID);

            if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery();

            socket.connect();
            Log.d(TAG, "Socket connected to " + config.address);

            retryCount.set(0);

            wearableConnection = new BluetoothWearableConnection(socket, config.nodeId, new ConnectionListener(context, config, wearableImpl));
            wearableConnection.run();
        } finally {
            WearableImpl.BluetoothConnectionLock.release(config.address, "BTLock");
        }
    }

    private void waitForRetry() throws InterruptedException {
        if (!running.get()) return;

        if (immediateRetry.getAndSet(false)) {
            Log.d(TAG, "Immediate retry flag set, skipping delay");
            return;
        }

        int count = retryCount.get();
        int delay = calcRetryDelay(count);
        Log.d(TAG, "Waiting " + delay + "ms before retry #" + count + " for " + config.address);

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            if (!running.get()) {
                Log.d(TAG, "Sleep interrupted for close");
            } else {
                Log.d(TAG, "Sleep interrupted for retry");
            }
        }
    }

    private void waitForExternalRetry() {
        Log.d(TAG, "Waiting for external retry trigger for " + config.address);

        try {
            while (running.get() && !immediateRetry.get()) {
                Thread.sleep(5000);
            }
            immediateRetry.set(false);
            retryCount.set(0);
        } catch (InterruptedException e) {
            if (running.get()) {
                Log.d(TAG, "External retry triggered for " + config.address);
                retryCount.set(0);
            }
        }
    }

    private int calcRetryDelay(int retryCount) {
        int delay = MIN_RETRY_DELAY_MS * (int)Math.pow(BACKOFF_MULTIPLIEER, Math.min(retryCount - 1, 6));
        return Math.min(delay, MAX_RETRY_DELAY_MS);
    }

    public void retryConnection(){
        Log.d(TAG, "Immediate retry requested for " + config.address);
        retryCount.set(0);
        immediateRetry.set(true);
        interrupt();
    }

    public void resetBackoff() {
        Log.d(TAG, "Resetting backoff for " + config.address);
        retryCount.set(0);
        lastAttemptTime = 0;
    }

    public void scheduleRetry() {
        retryCount.set(0);
        immediateRetry.set(true);
        interrupt();
//        retryHandler.post(() -> {
//            Log.d(TAG, "Scheduled retry triggered for " + config.address);
//            interrupt();
//        });
    }

    private void closeSocket() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.w(TAG, "Error closing socket", e);
            }
            socket = null;
        }
        wearableConnection = null;
    }

    private String getConnectionTypeName() {
        switch (config.type) {
            case 5: return "RFCOMM (type 5)";
            case WearableImpl.TYPE_BLUETOOTH_RFCOMM: return "RFCOMM";
            default: return "Unknown";
        }
    }

    @Override
    public void close(){
        Log.d(TAG, "Closing Bluetooth connection for " + config.address);
        running.set(false);
        closeSocket();
        interrupt();
    }

    private static class ConnectionListener implements WearableConnection.Listener {
        private final Context context;
        private final ConnectionConfiguration config;
        private final WearableImpl wearableImpl;
        private Connect peerConnect;
        private WearableConnection connection;

        public ConnectionListener(Context context, ConnectionConfiguration config, WearableImpl wearableImpl) {
            this.context = context;
            this.config = config;
            this.wearableImpl = wearableImpl;
        }

        @Override
        public void onConnected(WearableConnection connection) {
            Log.d(TAG, "Wearable connection established for " + config.address);

            this.connection = connection;

            BluetoothWearableConnection btConnection = (BluetoothWearableConnection) connection;
            this.peerConnect = btConnection.getPeerConnect();

            wearableImpl.onConnectReceived(connection, config.nodeId, peerConnect);
        }

        @Override
        public void onMessage(WearableConnection connection, RootMessage message) {
            Log.d(TAG, "Message received from " + config.address + ": " + message.toString());

        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "Wearable connection disconnected for " + config.address);
            if (connection != null && peerConnect != null) {
                wearableImpl.onDisconnectReceived(connection, peerConnect);
            }
        }
    }
}
