package org.microg.gms.wearable;

import android.util.Log;

import org.microg.gms.wearable.proto.RootMessage;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class WearableReader {
    private static final String TAG = "GmsWearReader";

    private final String nodeId;

    private final WearableConnection source;
    private final WearableConnection listenerView;
    private final WearableConnection.Listener listener;

    private final CountDownLatch finishedLatch = new CountDownLatch(1);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile Thread thread;

    public WearableReader(String nodeId, WearableConnection source,
                          WearableConnection listenerView, WearableConnection.Listener listener) {
        this.nodeId = nodeId;
        this.source = source;
        this.listenerView = listenerView;
        this.listener = listener;
    }

    public void start() {
        thread = new Thread(this::loop, "WearReader-" + nodeId);
        thread.start();
        Log.d(TAG, "Reader started for node "+ nodeId);
    }

    public void close() {
        if (closed.compareAndSet(false, true)) {
            Log.d(TAG, "Closing reader for node " + nodeId);
            Thread t = thread;
            if (t != null) t.interrupt();
            try {
                source.close();
            } catch (IOException ignore) {}
        }
    }

    public boolean awaitFinished(long timeout, TimeUnit unit) throws InterruptedException {
        return finishedLatch.await(timeout, unit);
    }

    public void awaitFinished() {
        boolean interrupted = false;
        while (true) {
            try {
                finishedLatch.await();
                break;
            } catch (InterruptedException e){
                interrupted = true;
            }
        }
        if (interrupted) Thread.currentThread().interrupt();
    }

    public boolean isClosed() {
        return closed.get();
    }

    private void loop() {
        try {
            listener.onConnected(listenerView);

            while (!Thread.currentThread().isInterrupted()) {
                RootMessage message;
                try {
                    message = source.readMessage();
                } catch (IOException e) {
                    if (!closed.get()) {
                        Log.w(TAG, "Read error from node " + nodeId + ": " + e.getMessage());
                    }
                    break;
                }

                if (message == null) break;

                try {
                    listener.onMessage(listenerView, message);
                } catch (Exception e) {
                    Log.e(TAG, "Error dispatching message from node " + nodeId, e);
                }
            }
        } catch (Exception e) {
            if (!closed.get()) {
                Log.e(TAG, "Unexpected reader error for node" + nodeId, e);
            }
        } finally {
            Log.d(TAG, "Reader finished for node " + nodeId);
            try {
                listener.onDisconnected();
            } catch (Exception e) {
                Log.e(TAG, "Error in onDisconnected() for node " + nodeId);
            }
            finishedLatch.countDown();
        }
    }
}
