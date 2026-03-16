package org.microg.gms.wearable.network;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.wearable.ConnectionConfiguration;

import org.microg.gms.profile.Build;
import org.microg.gms.wearable.ConnectHandshake;
import org.microg.gms.wearable.MessageHandler;
import org.microg.gms.wearable.SocketWearableConnection;
import org.microg.gms.wearable.TransportConnectionHandler;
import org.microg.gms.wearable.WearableConnection;
import org.microg.gms.wearable.WearableImpl;
import org.microg.gms.wearable.WearableReader;
import org.microg.gms.wearable.WearableWriter;
import org.microg.gms.wearable.bluetooth.RetryStrategy;
import org.microg.gms.wearable.proto.Connect;
import org.microg.gms.wearable.proto.MessagePiece;
import org.microg.gms.wearable.proto.RootMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NetworkConnectionThread extends Thread implements Cloneable{
    private static final String TAG = "GmsWearNetThread";

    private static final int CONNECT_TIMEOUT = 30000;
    private static final long MIN_ATTEMPT_INTERVAL = 3000;

    private final Context context;
    private final ConnectionConfiguration config;
    private final WearableImpl wearable;

    private final RetryStrategy retryStrategy;

    private final Lock lock = new ReentrantLock();

    private final Condition retryCondition = lock.newCondition();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean immediateRetry = new AtomicBoolean(false);

    private volatile Socket activeSocket;
    private volatile WearableWriter activeWriter;
    private long lastAttemptTime = 0;

    public NetworkConnectionThread(Context context, ConnectionConfiguration config, WearableImpl wearable) {
        super("NetThread-" + config.address);
        this.config = config;
        this.context = context;
        this.wearable = wearable;
        this.retryStrategy = RetryStrategy.fromPolicy(config.connectionRetryStrategy);
    }

    @Override
    public void run() {
        Log.d(TAG, "Started for " + config.address);

        while (running.get() && !isInterrupted()) {
            try {
                enforceMinInterval();
                if (!running.get()) break;

                connect();
                retryStrategy.reset();
            } catch (IOException e) {
                Log.w(TAG, "Connection failed to " + config.address + ": " + e.getMessage());
            } catch (InterruptedException e ) {
                if (!running.get()) break;
            } catch (Exception e){
                Log.e(TAG, "Unexpected error for " + config.address, e);
            } finally {
                teardown();
            }

            if (running.get() && !isInterrupted()) {
                try {
                    waitForRetry();
                } catch (InterruptedException e) {
                    if (!running.get()) break;
                }
            }
        }

        Log.d(TAG, "Stopped for " + config.address);
    }

    private void connect() throws IOException {
        Log.d(TAG, "Connecting to " + config.address + ":" + WearableImpl.WEAR_TCP_PORT);

        Socket socket = new Socket();
        socket.setTcpNoDelay(true);
        socket.connect(
                new InetSocketAddress(config.address, WearableImpl.WEAR_TCP_PORT),
                CONNECT_TIMEOUT
        );
        activeSocket = socket;

        Log.d(TAG, "Connected to " + config.address);

        SocketWearableConnection raw = new SocketWearableConnection(socket, null);
        ConnectHandshake.perform(raw, wearable.getLocalNodeId(), Build.MODEL, getAndroidId(), config.migrating, null);
        new TransportConnectionHandler(wearable, config).handle(raw);
        activeWriter = null;
    }

    private long getAndroidId() {
        try {
            String s = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            return s != null ? Long.parseUnsignedLong(s, 16) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void enforceMinInterval() throws InterruptedException {
        long elapsed = System.currentTimeMillis() - lastAttemptTime;
        if (lastAttemptTime > 0 && elapsed < MIN_ATTEMPT_INTERVAL) {
            Thread.sleep(MIN_ATTEMPT_INTERVAL - elapsed);
        }
        lastAttemptTime = System.currentTimeMillis();
    }

    private void waitForRetry() throws InterruptedException {
        long delayMs = retryStrategy.nextDelayMs();

        if (immediateRetry.getAndSet(false)) {
            Log.d(TAG, "Immediate retry for " + config.address);
            return;
        }

        if (delayMs < 0) {
            Log.d(TAG, "Retry OFF for " + config.address + ", waiting for external trigger");
            waitExternal();
            return;
        }

        Log.d(TAG, "Waiting " + delayMs + "ms before retry to " + config.address);
        lock.lock();
        try {
            long deadline = System.currentTimeMillis() + delayMs;
            while (running.get() && !immediateRetry.get()) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) break;
                retryCondition.await(remaining, TimeUnit.MILLISECONDS);
            }
            immediateRetry.set(false);
        } finally {
            lock.unlock();
        }
    }

    private void waitExternal() throws InterruptedException {
        lock.lock();
        try {
            while (running.get() && !immediateRetry.get()) {
                retryCondition.await();
            }
            immediateRetry.set(false);
        } finally {
            lock.unlock();
        }
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

    public void sendMessage(RootMessage message) {
        WearableWriter w = activeWriter;
        if (w != null && !w.isClosed()) {
            w.enqueue(message);
        } else {
            Log.w(TAG, "sendMessage() - no active writer for " + config.address);
        }
    }

    public void retryNow() {
        retryStrategy.reset();
        signalRetry();
    }

    public void triggerRetry() {
        signalRetry();
    }

    public boolean isConnected() {
        WearableWriter w = activeWriter;
        return w != null && !w.isClosed();
    }

    public void close() {
        Log.d(TAG, "Closing for " + config.address);
        running.set(false);
        signalRetry();
        interrupt();
        teardown();
    }

    private void teardown() {
        WearableWriter w = activeWriter;
        if (w != null) {
            w.close();
            activeWriter = null;
        }

        Socket s = activeSocket;
        if (s != null) {
            try {
                s.close();
            } catch (IOException e) {
                Log.w(TAG, "Error closing socket for " + config.address);
            }
            activeSocket = null;
        }
    }
}
