package org.microg.gms.wearable;

import android.util.Log;

import org.microg.gms.wearable.proto.Heartbeat;
import org.microg.gms.wearable.proto.RootMessage;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class WearableWriter {
    private static final String TAG = "GmsWearWriter";

    private static final RootMessage STOP = new RootMessage.Builder().build();

    private final String nodeId;
    private final WearableConnection connection;
    private final LinkedBlockingDeque<RootMessage> queue = new LinkedBlockingDeque<>();
    private final CountDownLatch finishedLatch = new CountDownLatch(1);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile Thread thread;

    private static final RootMessage HEARTBEAT_REQUEST = new RootMessage.Builder().build();
    private static final RootMessage HEARTBEAT_OUTGOING = new RootMessage.Builder()
            .heartbeat(new Heartbeat.Builder().build()).build();
    private final AtomicBoolean heartbeatPending = new AtomicBoolean(false);

    public WearableWriter(String nodeId, WearableConnection connection) {
        this.nodeId = nodeId;
        this.connection = connection;
    }

    public void start() {
        thread = new Thread(this::loop, "WearWriter-" + nodeId);
        thread.start();
        Log.d(TAG, "Writer started for node " + nodeId);
    }

    public void close() {
        if (closed.compareAndSet(false, true)) {
            Log.d(TAG, "Closing writer for node " + nodeId);
            queue.clear();
            queue.offer(STOP);
            Thread t = thread;
            if (t != null) t.interrupt();
        }
    }

    public boolean enqueue(RootMessage message) {
        if (!closed.get()) {
            return queue.offer(message);
        }
        return false;
    }

    public void requestHeartbeat() {
        if (closed.get())
            return;

        if (heartbeatPending.compareAndSet(false, true))
            queue.offer(HEARTBEAT_REQUEST);
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
            while (!Thread.currentThread().isInterrupted()) {
                RootMessage message;
                try {
                    message = queue.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (message == STOP) break;

                if (message == HEARTBEAT_REQUEST) {
                    heartbeatPending.set(false);
                    if (queue.isEmpty()) {
                        try {
                            connection.writeMessage(HEARTBEAT_OUTGOING);
                        } catch (IOException e) {
                            if (!closed.get()) {
                                Log.w(TAG, "Heartbeat write failed for node " + nodeId + ": " + e.getMessage());
                                try {
                                    connection.close();
                                } catch (IOException ignore) {}
                            }
                            break;
                        }
                    }
                    continue;
                }

                try {
                    connection.writeMessage(message);
                } catch (IOException e) {
                    if (!closed.get()) {
                        Log.w(TAG, "Write failed for node " + nodeId + ": " + e.getMessage());
                        try {
                            connection.close();
                        } catch (IOException ignore) {}
                    }
                    break;
                }
            }
        } catch (Exception e) {
            if (!closed.get()) {
                Log.e(TAG, "Unexpected writer error for node " + nodeId, e);
            }
        } finally {
            Log.d(TAG, "Writer finished for node "+ nodeId);
            finishedLatch.countDown();
        }
    }
}
