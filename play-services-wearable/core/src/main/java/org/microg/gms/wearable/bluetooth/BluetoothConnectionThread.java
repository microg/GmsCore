package org.microg.gms.wearable.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.google.android.gms.wearable.ConnectionConfiguration;

import org.microg.gms.wearable.MessageHandler;
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
    private static final long MIN_ATTEMPT_INTERVAL_MS = 3000;

    private volatile boolean isConnected = false;
    private volatile long lastActivityTime = 0;
    private static final long ACTIVITY_TIMEOUT_MS = 5000;

    private final Context context;
    private final ConnectionConfiguration config;
    private final BluetoothAdapter btAdapter;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private final AtomicBoolean immediateRetry = new AtomicBoolean(false);

    private long lastAttemptTime = 0;

    private BluetoothSocket socket;
    private WearableConnection wearableConnection;

    private final WearableImpl wearableImpl;

    private final PowerManager.WakeLock wakeLock;
    private static final long SOCKET_CONNECT_TIMEOUT_MS = 30000;

    public BluetoothConnectionThread(Context context, ConnectionConfiguration config, BluetoothAdapter btAdapter, WearableImpl wearableImpl) {
        super("BtThread-" + config.address);
        this.context = context;
        this.config = config;
        this.btAdapter = btAdapter;
        this.wearableImpl = wearableImpl;
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "GmsWear:BtConnect:" + config.address);
        wakeLock.setReferenceCounted(false);

    }

    public boolean isConnectionHealthy(){
        if (!isConnected || wearableConnection == null) {
            return false;
        }

        long timeSinceActivity = System.currentTimeMillis() - lastActivityTime;
        return isAlive() && !isInterrupted() && timeSinceActivity < ACTIVITY_TIMEOUT_MS;
    }

    private void markActivity() {
        lastActivityTime = System.currentTimeMillis();
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    @Override
    public void run(){
        Log.d(TAG, "Bluetooth connection thread started for " + config.address);

        while (running.get() && !isInterrupted()) {
            enforceMinInterval();

            if (!running.get()) break;

            try {
                if (!wakeLock.isHeld()) {
                    wakeLock.acquire(5 * 60 * 1000L);
                    Log.d(TAG, "Wake lock acquired for connection attempt");
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to acquire wake lock", e);
            }

            try {
                connect();
                retryCount.incrementAndGet();
            } catch (IOException e) {
                Log.w(TAG, "Connection failed for " + config.address + ": " + e.getMessage());
                retryCount.incrementAndGet();
            } catch (InterruptedException e) {
                Log.d(TAG, "Connection thread interrupted");
                if (!running.get()) {
                    break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error in connection loop", e);
                retryCount.incrementAndGet();
            } finally {
                closeSocket();

                try {
                    if (wakeLock.isHeld()) {
                        wakeLock.release();
                        Log.d(TAG, "Wake lock released");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to release wake lock", e);
                }
            }

            if (running.get() && !isInterrupted()) {
                try {
                    waitForRetry();
                } catch (InterruptedException e) {
                    Log.d(TAG, "Retry wait interrupted");
                    if (!running.get()) {
                        break;
                    }
                }
            }
        }

        closeSocket();

        try {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to release wake lock in cleanup", e);
        }

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

            connectSocketWithTimeout(socket);
            Log.d(TAG, "Socket connected to " + config.address);

            retryCount.set(0);
            isConnected = true;
            markActivity();

            wearableConnection = new BluetoothWearableConnection(socket, config.nodeId, new ConnectionListener(context, config, wearableImpl, this));
            wearableConnection.run();

        } finally {
            isConnected = false;
            WearableImpl.BluetoothConnectionLock.release(config.address, "BTLock");
        }
    }

    private void connectSocketWithTimeout(BluetoothSocket socket) throws IOException, InterruptedException {
        final AtomicBoolean connected = new AtomicBoolean(false);
        final AtomicBoolean timedOut = new AtomicBoolean(false);
        final AtomicBoolean connectFailed = new AtomicBoolean(false);
        final Object lock = new Object();
        final IOException[] exception = new IOException[1];

        Thread connectThread = new Thread(new Runnable() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public void run() {
                try {
                    synchronized (lock) {
                        if (timedOut.get()) {
                            Log.w(TAG, "Connect aborted - already timed out");
                            return;
                        }
                    }

                    socket.connect();

                    synchronized (lock) {
                        if (!timedOut.get()) {
                            connected.set(true);
                            Log.d(TAG, "Socket connect succeeded");
                        } else {
                            Log.w(TAG, "Socket connect succeeded but timeout already occurred");
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.w(TAG, "Failed to close socket after timeout", e);
                            }
                        }
                    }
                } catch (IOException e) {
                    synchronized (lock) {
                        if (!timedOut.get()) {
                            exception[0] = e;
                            connectFailed.set(true);
                        }
                    }
                }
            }
        }, "BtSocketConnect-" + config.address);

        connectThread.start();

        long startTime = System.currentTimeMillis();
        long endTime = startTime + SOCKET_CONNECT_TIMEOUT_MS;

        while (System.currentTimeMillis() < endTime && running.get()) {
            synchronized (lock) {
                if (connected.get()) {
                    return;
                }

                if (connectFailed.get()) {
                    throw exception[0];
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                connectThread.interrupt();
                throw e;
            }
        }

        synchronized (lock) {
            if (!connected.get()) {
                timedOut.set(true);
                Log.e(TAG, "Socket connect timed out after " + SOCKET_CONNECT_TIMEOUT_MS + "ms");

                try {
                    socket.close();
                } catch (IOException e) {
                    Log.w(TAG, "Failed to close socket after timeout", e);
                }

                connectThread.interrupt();
                throw new IOException("Socket connect timed out after " + SOCKET_CONNECT_TIMEOUT_MS + "ms");
            }
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
        isConnected = false;
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

        private final BluetoothConnectionThread thread;

        private MessageHandler messageHandler;

        public ConnectionListener(Context context, ConnectionConfiguration config, WearableImpl wearableImpl, BluetoothConnectionThread thread) {
            this.context = context;
            this.config = config;
            this.wearableImpl = wearableImpl;
            this.thread = thread;
        }

        @Override
        public void onConnected(WearableConnection connection) {
            Log.d(TAG, "Wearable connection established for " + config.address);

            this.connection = connection;

            BluetoothWearableConnection btConnection = (BluetoothWearableConnection) connection;
            this.peerConnect = btConnection.getPeerConnect();

            this.messageHandler = new MessageHandler(context, wearableImpl, config);

            thread.markActivity();
            wearableImpl.onConnectReceived(connection, config.nodeId, peerConnect);
        }

        @Override
        public void onMessage(WearableConnection connection, RootMessage message) {
            Log.d(TAG, "Message received from " + config.address + ": " + message.toString());
            thread.markActivity();
            if (peerConnect != null && messageHandler != null)
                messageHandler.handleMessage(connection, peerConnect.id, message);
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
