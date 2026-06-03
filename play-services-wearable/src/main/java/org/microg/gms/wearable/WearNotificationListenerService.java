package org.microg.gms.wearable;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Watch-side service that receives forwarded phone notifications.
 *
 * SOLVES:
 * - A-01: Missing watch-side notification receiver
 */
public class WearNotificationListenerService extends WearableListenerService {

    private static final String TAG = "WearNotifListener";
    private static final String CHANNEL_ID = "microg_phone_notifs";

    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        createChannel();
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        if (event == null) return;
        String path = event.getPath();

        if (WearableDataPaths.NOTIF_FORWARD.equals(path)
                || path.startsWith(WearableDataPaths.NOTIF_BATCH)) {
            handleNotification(event.getData());
        } else if (path.startsWith(WearableDataPaths.NOTIF_DISMISS)) {
            handleDismiss(event.getData());
        }
    }

    private void handleNotification(byte[] data) {
        Bundle bundle = BundleUtil.fromByteArray(data);
        if (bundle == null) return;

        String title = bundle.getString("title", "");
        String text = bundle.getString("text", "");
        String key = bundle.getString("key", "");

        if (!key.isEmpty()) {
            postNotification(key.hashCode(), title, text);
            return;
        }

        int count = bundle.getInt("count", 0);
        for (int i = 0; i < count; i++) {
            String bTitle = bundle.getString("title_" + i, "");
            String bText = bundle.getString("text_" + i, "");
            String bKey = bundle.getString("key_" + i, "");
            if (!bKey.isEmpty()) postNotification(bKey.hashCode(), bTitle, bText);
        }
    }

    private void postNotification(int id, String title, String text) {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        builder.setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis());
        notificationManager.notify(id, builder.build());
        Log.d(TAG, "Posted: " + title + " id=" + id);
    }

    private void handleDismiss(byte[] data) {
        Bundle bundle = BundleUtil.fromByteArray(data);
        if (bundle == null) return;
        String key = bundle.getString("key", "");
        if (!key.isEmpty()) {
            notificationManager.cancel(key.hashCode());
            Log.d(TAG, "Dismissed: " + key);
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Phone Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
