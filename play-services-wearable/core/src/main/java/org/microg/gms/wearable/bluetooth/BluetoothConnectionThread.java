package org.microg.gms.wearable.bluetooth;

import android.Manifest;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.PowerManager;
import android.os.SystemClock;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BluetoothConnectionThread extends Thread implements Closeable {
    private static final String TAG = "GmsWearBtConnThread";

    private static final UUID WEAR_BT_UUID = UUID.fromString("5e8945b0-9525-11e3-a5e2-0800200c9a66");

    private static final long MIN_ATTEMPT_INTERVAL_MS = 3000;
    private static final long SOCKET_CONNECT_TIMEOUT_MS = 30000;
    private static final long ACTIVITY_TIMEOUT_MS = 5000;

    private final Context context;
    private final ConnectionConfiguration config;
    private final BluetoothAdapter btAdapter;
    private final BluetoothDevice btDevice;
    private final WearableImpl wearableImpl;
    private final ScheduledExecutorService executor;

    private final WakeLockManager wakeLockManager;
    private final RetryStrategy retryStrategy;
    private final AlarmManagerHelper alarmHelper;
    private final BleDeviceDiscoverer bleDiscoverer; // Nullable

    private final Lock lock = new ReentrantLock();
    private final Condition retryCondition = lock.newCondition();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean immediateRetry = new AtomicBoolean(false);

    private volatile boolean isConnected = false;
    private volatile long lastActivityTime = 0;
    private long lastAttemptTime = 0;

    private BluetoothSocket socket;
    private WearableConnection wearableConnection;

    private BroadcastReceiver retryReceiver;
    private boolean receiverRegistered = false;

    public BluetoothConnectionThread(Context context, ConnectionConfiguration config,
                                     BluetoothAdapter btAdapter, WearableImpl wearableImpl,
                                     ScheduledExecutorService executor, BleDeviceDiscoverer bleDiscoverer) {
        super("BtThread-" + config.address);
        this.context = context;
        this.config = config;
        this.btAdapter = btAdapter;
        this.wearableImpl = wearableImpl;

        this.executor = executor;
        this.bleDiscoverer = bleDiscoverer;

        this.btDevice = btAdapter.getRemoteDevice(config.address);

        this.wakeLockManager = new WakeLockManager(context,
                "BtConnect:" + config.address, executor);
        this.retryStrategy = RetryStrategy.fromPolicy(config.connectionRetryStrategy);
        this.alarmHelper = new AlarmManagerHelper(context);

        registerRetryReceiver();

    }

    private void registerRetryReceiver() {
        retryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && "com.google.android.gms.wearable.RETRY_CONNECTION".equals(intent.getAction())) {
                    String address = intent.getData() != null ? intent.getData().getAuthority() : null;
                    if (config.address.equals(address)) {
                        Log.d(TAG, "Alarm triggered retry for " + config.address);
                        signalRetry();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("com.google.android.gms.wearable.RETRY_CONNECTION");
        filter.addDataScheme("wearable");

        context.registerReceiver(retryReceiver, filter);
        receiverRegistered = true;
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
            try {
                enforceMinInterval();

                if (!running.get()) break;

                wakeLockManager.acquire("connect", SOCKET_CONNECT_TIMEOUT_MS + 5000);

                try {
                    connect();
                    retryStrategy.reset();

                } catch (IOException e) {
                    Log.w(TAG, "Connection failed: " + e.getMessage());
                } catch (InterruptedException e) {
                    Log.d(TAG, "Connection interrupted");
                    if (!running.get()) break;
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error", e);
                } finally {
                    closeSocket();
                    wakeLockManager.release("connect");
                }

                if (running.get() && !isInterrupted()) {
                    waitForRetry();
                }

            } catch (InterruptedException e) {
                Log.d(TAG, "Thread interrupted");
                if (!running.get()) break;
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error in main loop", e);
            }
        }

        Log.d(TAG, "Bluetooth connection thread stopped for " + config.address);
        cleanup();
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
        if (!running.get() || btAdapter == null || !btAdapter.isEnabled()) {
            throw new IOException("Bluetooth not available");
        }

        Log.d(TAG, "Connecting to " + config.address);

        socket = btDevice.createRfcommSocketToServiceRecord(WEAR_BT_UUID);

        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }

        connectSocketWithTimeout(socket);

        Log.d(TAG, "Socket connected to " + config.address);

        isConnected = true;
        markActivity();

        wearableConnection = new BluetoothWearableConnection(
                socket, config.nodeId,
                new ConnectionListener(context, config, wearableImpl, this)
        );
        wearableConnection.run();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void connectSocketWithTimeout(BluetoothSocket socket) throws IOException, InterruptedException {
        final AtomicBoolean connected = new AtomicBoolean(false);
        final AtomicBoolean timedOut = new AtomicBoolean(false);
        final Object connectLock = new Object();
        final IOException[] exception = new IOException[1];

        Thread connectThread = new Thread(() -> {
            try {
                synchronized (connectLock) {
                    if (timedOut.get()) return;
                }

                socket.connect();

                synchronized (connectLock) {
                    if (!timedOut.get()) {
                        connected.set(true);
                    } else {
                        try {
                            socket.close();
                        } catch (IOException ignored) {}
                    }
                }
            } catch (IOException e) {
                synchronized (connectLock) {
                    if (!timedOut.get()) {
                        exception[0] = e;
                    }
                }
            }
        }, "BtSocketConnect-" + config.address);

        connectThread.start();

        long startTime = System.currentTimeMillis();
        long endTime = startTime + SOCKET_CONNECT_TIMEOUT_MS;

        while (System.currentTimeMillis() < endTime && running.get()) {
            synchronized (connectLock) {
                if (connected.get()) {
                    return;
                }

                if (exception[0] != null) {
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

        synchronized (connectLock) {
            if (!connected.get()) {
                timedOut.set(true);
                Log.e(TAG, "Socket connect timed out after " + SOCKET_CONNECT_TIMEOUT_MS + "ms");

                try {
                    socket.close();
                } catch (IOException ignored) {}

                connectThread.interrupt();
                throw new IOException("Socket connect timed out");
            }
        }
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void waitForRetry() throws InterruptedException {
        if (!running.get()) return;

        long delayMs = retryStrategy.nextDelayMs();

        if (delayMs < 0) {
            Log.d(TAG, "Retry strategy OFF, waiting for external trigger");
            waitForExternalRetry();
            return;
        }

        if (immediateRetry.getAndSet(false)) {
            Log.d(TAG, "Immediate retry requested");
            wakeLockManager.acquire("retry", delayMs + 5000);
            return;
        }

        Log.d(TAG, String.format("Waiting %dms before retry", delayMs));

        if (delayMs > 60_000) {
            useAlarmManagerForRetry(delayMs);
        } else {
            useThreadSleepForRetry(delayMs);
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void useAlarmManagerForRetry(long delayMs) throws InterruptedException {
        Log.d(TAG, "Using AlarmManager for retry delay");

        if (bleDiscoverer != null && config.type == 5) {
            bleDiscoverer.addDevice(btDevice, device -> {
                Log.d(TAG, "BLE discovered device, triggering retry");
                signalRetry();
            });
        }

        long triggerTime = SystemClock.elapsedRealtime() + delayMs;
        PendingIntent pendingIntent = createRetryPendingIntent();

        alarmHelper.setExactAndAllowWhileIdle(
                "WearRetry",
                android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                pendingIntent
        );

        wakeLockManager.release("retry-wait");

        lock.lock();
        try {
            while (running.get() && !immediateRetry.get()) {
                retryCondition.await();
            }
            immediateRetry.set(false);
        } finally {
            lock.unlock();
        }

        alarmHelper.cancel(pendingIntent);

        wakeLockManager.acquire("retry", 60_000);
    }

    private void useThreadSleepForRetry(long delayMs) throws InterruptedException {
        lock.lock();
        try {
            long endTime = System.currentTimeMillis() + delayMs;

            while (running.get() && !immediateRetry.get()) {
                long remaining = endTime - System.currentTimeMillis();
                if (remaining <= 0) break;

                retryCondition.await(remaining, java.util.concurrent.TimeUnit.MILLISECONDS);
            }

            immediateRetry.set(false);
        } finally {
            lock.unlock();
        }

        wakeLockManager.acquire("retry", 60_000);
    }

    private void waitForExternalRetry() {
        Log.d(TAG, "Waiting for external retry trigger for " + config.address);

        wakeLockManager.release("wait-external");

        lock.lock();
        try {
            while (running.get() && !immediateRetry.get()) {
                retryCondition.await();
            }
            immediateRetry.set(false);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }

        wakeLockManager.acquire("external-retry", 60_000);
    }

    private void signalRetry() {
        lock.lock();
        try {
            immediateRetry.set(true);
            retryCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void resetBackoffAndRetryConnection() {
        Log.d(TAG, "Reset backoff and retry requested");
        retryStrategy.reset();
        signalRetry();
    }

    public void retryConnection(){
        Log.d(TAG, "Retry requested");
        signalRetry();
    }

    private PendingIntent createRetryPendingIntent() {
        Intent intent = new Intent("com.google.android.gms.wearable.RETRY_CONNECTION");
        intent.setData(new Uri.Builder()
                .scheme("wearable")
                .authority(config.address)
                .build());
        intent.setPackage(context.getPackageName());

        return AlarmManagerHelper.createPendingIntent(context, 1, intent);
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void cleanup() {
        closeSocket();

        if (bleDiscoverer != null) {
            bleDiscoverer.removeDevice(btDevice);
        }

        if (receiverRegistered) {
            try {
                context.unregisterReceiver(retryReceiver);
            } catch (Exception e) {
                Log.w(TAG, "Error unregistering receiver", e);
            }
            receiverRegistered = false;
        }

        wakeLockManager.shutdown();
    }

    @Override
    public void close(){
        Log.d(TAG, "Closing connection thread for " + config.address);
        running.set(false);
        signalRetry();
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
