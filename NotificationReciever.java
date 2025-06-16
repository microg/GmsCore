package org.microg.gms.gcm.WearOS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "WearNotificationReceiver";
    private static final String CHANNEL_ID = "wear_notification_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && "org.microg.WEAR_NOTIFICATION".equals(intent.getAction())) {
            String title = intent.getStringExtra("title");
            String content = intent.getStringExtra("content");

            Log.d(TAG, "Received Notification - Title: " + title + ", Content: " + content);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title != null ? title : "MicroG Wear")
                    .setContentText(content != null ? content : "No message")
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } else {
            Log.w(TAG, "Unexpected intent or null action received.");
        }
    }
}