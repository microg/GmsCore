package org.microg.gms.wearable;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Phone-side notification forwarder for WearOS.
 *
 * SOLVES:
 * - A-02: Rate limiter bypass (all notifications now go through limiter)
 * - A-05: Flush callback protection (try/catch around forwarding)
 */
public class WearOSNotificationForwarder {

    private static final String TAG = "WearNotifFwd";
    private static final long DEBOUNCE_WINDOW_MS = 2_000;
    private static final int MAX_BATCH_SIZE = 5;

    private final Context context;
    private final ScheduledExecutorService scheduler;
    private GoogleApiClient googleApiClient;
    private MessageRetryQueue retryQueue;

    private final Map<String, SerializedNotification> pendingNotifications
            = new ConcurrentHashMap<>();
    private final List<SerializedNotification> batchBuffer
            = new CopyOnWriteArrayList<>();
    private ScheduledFuture<?> debounceFuture;
    private long lastFlushTime;
    private final Map<String, Node> connectedNodes = new ConcurrentHashMap<>();
    private volatile boolean isRunning = false;

    static class SerializedNotification {
        final String key;
        final String packageName;
        final String title;
        final String text;
        final long timestamp;

        SerializedNotification(String key, String packageName,
                               String title, String text, long timestamp) {
            this.key = key;
            this.packageName = packageName;
            this.title = title;
            this.text = text;
            this.timestamp = timestamp;
        }
    }

    public WearOSNotificationForwarder(Context context) {
        this.context = context.getApplicationContext();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WearOS-NotifFwd");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void start(GoogleApiClient apiClient, MessageRetryQueue retryQueue) {
        if (isRunning) return;
        this.googleApiClient = apiClient;
        this.retryQueue = retryQueue;
        refreshNodes();
        isRunning = true;
        Log.i(TAG, "Started");
    }

    public synchronized void stop() {
        if (!isRunning) return;
        if (debounceFuture != null) debounceFuture.cancel(false);
        pendingNotifications.clear();
        batchBuffer.clear();
        connectedNodes.clear();
        isRunning = false;
        Log.i(TAG, "Stopped");
    }

    public void processNotification(StatusBarNotification sbn) {
        if (!isRunning || sbn == null) return;
        Notification notification = sbn.getNotification();
        if (notification == null) return;

        String pkg = sbn.getPackageName();
        if (isSystemNotification(pkg)) return;

        Bundle extras = notification.extras;
        String title = extras != null
                ? extras.getString(Notification.EXTRA_TITLE, "") : "";
        CharSequence textCs = extras != null
                ? extras.getCharSequence(Notification.EXTRA_TEXT) : null;
        String text = textCs != null ? textCs.toString() : "";
        if (title.isEmpty() && text.isEmpty()) return;

        String key = sbn.getKey();
        SerializedNotification serialized = new SerializedNotification(
                key, pkg, title, text, sbn.getPostTime());

        // A-02 FIX: All notifications go through the rate limiter
        pendingNotifications.put(key, serialized);
        batchBuffer.add(serialized);
        scheduleFlush();
    }

    public void processNotificationRemoval(StatusBarNotification sbn) {
        if (!isRunning || sbn == null) return;
        String key = sbn.getKey();
        pendingNotifications.remove(key);
        batchBuffer.removeIf(n -> n.key.equals(key));
        forwardDismissal(key);
    }

    private void scheduleFlush() {
        if (debounceFuture != null && !debounceFuture.isDone()) {
            debounceFuture.cancel(false);
        }
        if (batchBuffer.size() >= MAX_BATCH_SIZE) {
            scheduler.execute(this::flushBatch);
            return;
        }
        long timeSinceLastFlush = System.currentTimeMillis() - lastFlushTime;
        long delay = Math.max(100, DEBOUNCE_WINDOW_MS - timeSinceLastFlush);
        debounceFuture = scheduler.schedule(this::flushBatch,
                delay, TimeUnit.MILLISECONDS);
    }

    // A-05 FIX: Wraps forwarding in try/catch
    private void flushBatch() {
        if (batchBuffer.isEmpty()) return;
        List<SerializedNotification> toSend = new ArrayList<>(batchBuffer);
        batchBuffer.clear();
        lastFlushTime = System.currentTimeMillis();

        if (toSend.size() == 1) {
            try { forwardSingleNotification(toSend.get(0)); }
            catch (Exception e) {
                Log.e(TAG, "Forward failed, re-queuing", e);
                batchBuffer.add(toSend.get(0));
            }
        } else {
            try { forwardBatchNotifications(toSend); }
            catch (Exception e) {
                Log.e(TAG, "Batch forward failed, re-queuing", e);
                batchBuffer.addAll(toSend);
            }
        }
    }

    private void forwardSingleNotification(SerializedNotification notif) {
        Bundle data = new Bundle();
        data.putString("title", notif.title);
        data.putString("text", notif.text);
        data.putString("package", notif.packageName);
        data.putString("key", notif.key);
        data.putLong("timestamp", notif.timestamp);
        sendMessageToNodes(WearableDataPaths.NOTIF_FORWARD,
                BundleUtil.toByteArray(data));
    }

    private void forwardBatchNotifications(List<SerializedNotification> notifs) {
        Bundle batchData = new Bundle();
        batchData.putInt("count", notifs.size());
        for (int i = 0; i < notifs.size(); i++) {
            SerializedNotification n = notifs.get(i);
            batchData.putString("title_" + i, n.title);
            batchData.putString("text_" + i, n.text);
            batchData.putString("package_" + i, n.packageName);
            batchData.putString("key_" + i, n.key);
            batchData.putLong("timestamp_" + i, n.timestamp);
        }
        sendMessageToNodes(WearableDataPaths.NOTIF_BATCH,
                BundleUtil.toByteArray(batchData));
    }

    private void forwardDismissal(String notifKey) {
        Bundle data = new Bundle();
        data.putString("key", notifKey);
        sendMessageToNodes(WearableDataPaths.NOTIF_DISMISS,
                BundleUtil.toByteArray(data));
    }

    public void handleDismissMessage(MessageEvent event) {
        Bundle bundle = BundleUtil.fromByteArray(event.getData());
        if (bundle == null) return;
        String key = bundle.getString("key", "");
        if (!key.isEmpty()) Log.d(TAG, "Watch dismissed: " + key);
    }

    public void handleReplyMessage(MessageEvent event) {
        Bundle bundle = BundleUtil.fromByteArray(event.getData());
        if (bundle == null) return;
        String key = bundle.getString("key", "");
        String replyText = bundle.getString("reply_text", "");
        Log.d(TAG, "Watch reply for " + key + ": " + replyText);
    }

    private void refreshNodes() {
        if (googleApiClient == null || !googleApiClient.isConnected()) return;
        Wearable.NodeApi.getConnectedNodes(googleApiClient)
                .setResultCallback(result -> {
                    connectedNodes.clear();
                    if (result.getNodes() != null)
                        for (Node node : result.getNodes())
                            connectedNodes.put(node.getId(), node);
                });
    }

    private void sendMessageToNodes(String path, byte[] data) {
        if (googleApiClient == null || !googleApiClient.isConnected()) return;
        if (connectedNodes.isEmpty()) refreshNodes();
        for (Node node : connectedNodes.values()) {
            if (retryQueue != null) {
                retryQueue.enqueue(node.getId(), path, data);
            } else {
                Wearable.MessageApi.sendMessage(
                        googleApiClient, node.getId(), path, data);
            }
        }
    }

    private boolean isSystemNotification(String packageName) {
        return packageName.startsWith("com.android.systemui")
                || packageName.equals("android")
                || packageName.equals(context.getPackageName());
    }
}
