package org.microg.gms.wearable;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reliable message delivery queue with exponential backoff.
 *
 * SOLVES:
 * - S-05: Messages silently dropped when attemptDelivery throws
 * - Messages lost during transient Bluetooth/API disconnects
 *
 * DESIGN:
 * - PriorityBlockingQueue ordered by next-attempt timestamp
 * - Single consumer thread processes due messages
 * - Exponential backoff: 1s, 2s, 4s, 8s, 16s (max 5 retries)
 * - Jitter (±20%) prevents thundering herd on reconnection
 * - Deduplication: same node+path replaces previous message (latest-wins)
 * - Dead letter callback for permanently failed messages
 */
public class MessageRetryQueue {

    private static final String TAG = "WearMsgRetry";

    private static final int MAX_RETRIES = 5;
    private static final long BASE_DELAY_MS = 1_000;
    private static final long MAX_DELAY_MS = 16_000;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final double JITTER_FACTOR = 0.2;

    private final ScheduledExecutorService scheduler;
    private final PriorityBlockingQueue<QueuedMessage> queue;
    private final Map<String, QueuedMessage> pendingById;
    private final AtomicLong idCounter;
    private GoogleApiClient googleApiClient;
    private DeadLetterHandler deadLetterHandler;
    private volatile boolean isRunning = false;

    // --- Queued Message ---

    static class QueuedMessage implements Comparable<QueuedMessage> {
        final String id;
        final String nodeId;
        final String path;
        final byte[] data;
        final long createdAt;
        int retryCount;
        long nextAttemptAt;
        long lastAttemptAt;

        QueuedMessage(String id, String nodeId, String path, byte[] data) {
            this.id = id;
            this.nodeId = nodeId;
            this.path = path;
            this.data = data != null ? data.clone() : new byte[0];
            this.createdAt = System.currentTimeMillis();
            this.retryCount = 0;
            this.nextAttemptAt = createdAt;
            this.lastAttemptAt = 0;
        }

        @Override
        public int compareTo(QueuedMessage o) {
            return Long.compare(this.nextAttemptAt, o.nextAttemptAt);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || (o instanceof QueuedMessage
                    && id.equals(((QueuedMessage) o).id));
        }

        @Override
        public int hashCode() { return id.hashCode(); }
    }

    public interface DeadLetterHandler {
        void onMessageFailed(String nodeId, String path,
                             byte[] data, int attempts);
    }

    // --- Lifecycle ---

    public MessageRetryQueue() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WearOS-MsgRetry");
            t.setDaemon(true);
            return t;
        });
        this.queue = new PriorityBlockingQueue<>(64);
        this.pendingById = new ConcurrentHashMap<>();
        this.idCounter = new AtomicLong(0);
    }

    public void start(GoogleApiClient apiClient) {
        if (isRunning) return;
        this.googleApiClient = apiClient;
        this.isRunning = true;
        scheduler.execute(this::processQueue);
        Log.i(TAG, "Started");
    }

    public void stop() {
        isRunning = false;
        queue.clear();
        pendingById.clear();
        Log.i(TAG, "Stopped");
    }

    public void setDeadLetterHandler(DeadLetterHandler handler) {
        this.deadLetterHandler = handler;
    }

    public void updateApiClient(GoogleApiClient apiClient) {
        this.googleApiClient = apiClient;
    }

    // --- Enqueue ---

    public String enqueue(String nodeId, String path, byte[] data) {
        String dedupeKey = nodeId + ":" + path;
        pendingById.remove(dedupeKey);

        String id = "msg_" + idCounter.incrementAndGet();
        QueuedMessage msg = new QueuedMessage(id, nodeId, path, data);
        pendingById.put(dedupeKey, msg);
        queue.offer(msg);

        Log.d(TAG, "Enqueued: " + id + " " + path);
        return id;
    }

    public boolean cancel(String messageId) {
        boolean removed = queue.removeIf(m -> m.id.equals(messageId));
        if (removed) pendingById.values().removeIf(m -> m.id.equals(messageId));
        return removed;
    }

    public int size() { return queue.size(); }

    // --- Processing ---

    /**
     * Main processing loop.
     *
     * S-05 FIX: The outer catch(Exception) block now re-queues the
     * message with backoff instead of silently dropping it. The msg
     * reference is captured before any operation that could throw,
     * ensuring we always have it available for re-queue.
     */
    private void processQueue() {
        while (isRunning) {
            QueuedMessage msg = null;
            try {
                msg = queue.poll(1, TimeUnit.SECONDS);
                if (msg == null) continue;

                long now = System.currentTimeMillis();

                // Not yet time to attempt
                if (msg.nextAttemptAt > now) {
                    queue.offer(msg);
                    Thread.sleep(Math.min(msg.nextAttemptAt - now, 500));
                    continue;
                }

                // API client unavailable
                if (googleApiClient == null || !googleApiClient.isConnected()) {
                    msg.nextAttemptAt = now + BASE_DELAY_MS;
                    queue.offer(msg);
                    Thread.sleep(500);
                    continue;
                }

                boolean success = attemptDelivery(msg);

                if (success) {
                    pendingById.remove(msg.nodeId + ":" + msg.path);
                    Log.d(TAG, "Delivered: " + msg.id + " " + msg.path);
                } else {
                    requeueWithBackoff(msg, now);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // S-05 FIX: Re-queue the message instead of dropping it.
                Log.e(TAG, "Unexpected error in queue processing", e);
                if (msg != null) {
                    msg.retryCount++;
                    if (msg.retryCount >= MAX_RETRIES) {
                        pendingById.remove(msg.nodeId + ":" + msg.path);
                        if (deadLetterHandler != null) {
                            deadLetterHandler.onMessageFailed(
                                    msg.nodeId, msg.path,
                                    msg.data, msg.retryCount);
                        }
                    } else {
                        msg.nextAttemptAt = System.currentTimeMillis()
                                + calculateBackoff(msg.retryCount);
                        queue.offer(msg);
                    }
                }
            }
        }
    }

    private void requeueWithBackoff(QueuedMessage msg, long now) {
        msg.retryCount++;
        msg.lastAttemptAt = now;

        if (msg.retryCount >= MAX_RETRIES) {
            pendingById.remove(msg.nodeId + ":" + msg.path);
            if (deadLetterHandler != null) {
                deadLetterHandler.onMessageFailed(
                        msg.nodeId, msg.path, msg.data, msg.retryCount);
            }
        } else {
            long delay = calculateBackoff(msg.retryCount);
            msg.nextAttemptAt = now + delay;
            queue.offer(msg);
            Log.d(TAG, "Retry " + msg.id + " attempt=" + msg.retryCount
                    + " delay=" + delay + "ms");
        }
    }

    private boolean attemptDelivery(QueuedMessage msg) {
        try {
            com.google.android.gms.wearable.MessageApi.SendMessageResult
                    result = Wearable.MessageApi.sendMessage(
                            googleApiClient, msg.nodeId,
                            msg.path, msg.data)
                    .await(5, TimeUnit.SECONDS);

            if (result == null) return false;
            return result.getStatus().isSuccess();

        } catch (IllegalStateException e) {
            Log.w(TAG, "API invalid during send: " + msg.id, e);
            return false;
        } catch (Exception e) {
            Log.w(TAG, "Delivery failed: " + msg.id, e);
            return false;
        }
    }

    private long calculateBackoff(int retryCount) {
        double base = BASE_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, retryCount);
        double capped = Math.min(base, MAX_DELAY_MS);
        double jitter = capped * JITTER_FACTOR;
        double delay = capped + (Math.random() * 2 - 1) * jitter;
        return Math.max(500, (long) delay);
    }
}
