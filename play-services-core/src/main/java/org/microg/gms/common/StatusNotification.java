package org.microg.gms.common;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.mgoogle.android.gms.R;

public class StatusNotification {

    private static int notificationID;
    private static String notificationChannelID = "";
    private static boolean notificationExists = false;

    public static void Notify(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && NotificationManagerCompat.from(context.getApplicationContext()).areNotificationsEnabled()) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            boolean isChannelEnabled = true;

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationChannelID != "") {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel notificationChannel = notificationManager.getNotificationChannel(notificationChannelID);
                if (notificationChannel.getImportance() != NotificationManager.IMPORTANCE_NONE) {
                    isChannelEnabled = true;
                } else {
                    isChannelEnabled = false;
                }
            }

            if (isChannelEnabled) {
                if (!powerManager.isIgnoringBatteryOptimizations(context.getPackageName())) {
                    if (!notificationExists) {
                        buildStatusNotification(context);
                    }
                } else {
                    if (notificationExists) {
                        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notificationID);
                        notificationExists = false;
                    }
                }
            }
        }
    }

    private static void buildStatusNotification(Context context) {
        notificationID = 1;

        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannelID = "foreground-service";

            NotificationChannel channel = new NotificationChannel(notificationChannelID,
                    context.getResources().getString(R.string.notification_service_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(0);
            channel.setVibrationPattern(new long[0]);
            context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, notificationChannelID)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_foreground_notification)
                .setSubText(context.getResources().getString(R.string.notification_service_title))
                .setContentTitle(context.getResources().getString(R.string.notification_service_content))
                .setContentText(context.getResources().getString(R.string.notification_service_subcontent));

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify(notificationID, notification.build());

        notificationExists = true;
    }
}
