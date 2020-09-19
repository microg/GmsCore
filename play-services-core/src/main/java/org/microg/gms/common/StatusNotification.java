package org.microg.gms.common;

import android.app.Notification;
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

    private static Notification Notification;
    private static int notificationID = 1;
    private static String notificationChannelID = "foreground-service";
    private static boolean notificationExists = false;

    public static void Notify(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            boolean isChannelEnabled = true;

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel notificationChannel = notificationManager.getNotificationChannel(notificationChannelID);
                if (notificationChannel != null
                        && notificationChannel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                    isChannelEnabled = false;
                }
            }

            if (NotificationManagerCompat.from(context.getApplicationContext()).areNotificationsEnabled()
                    && isChannelEnabled) {
                if (!powerManager.isIgnoringBatteryOptimizations(context.getPackageName())) {
                        if (!notificationExists) {
                            buildStatusNotification(context);
                        } else {
                            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                            notificationManager.notify(notificationID, Notification);
                        }
                } else {
                    if (notificationExists) {
                        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notificationID);
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).deleteNotificationChannel(notificationChannelID);
                        }
                        notificationExists = false;
                    }
                }
            }
        }
    }

    private static void buildStatusNotification(Context context) {
        Intent notificationIntent = new Intent();
        notificationIntent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel Channel = new NotificationChannel(notificationChannelID,
                    context.getResources().getString(R.string.notification_service_name),
                    NotificationManager.IMPORTANCE_LOW);
            Channel.setShowBadge(false);
            Channel.setLockscreenVisibility(0);
            Channel.setVibrationPattern(new long[0]);
            context.getSystemService(NotificationManager.class).createNotificationChannel(Channel);
        }
        Notification = new NotificationCompat.Builder(context, notificationChannelID)
                .setOngoing(true)
                .setContentIntent(notificationPendingIntent)
                .setSmallIcon(R.drawable.ic_foreground_notification)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(context.getResources().getString(R.string.notification_service_title))
                        .bigText(context.getResources().getString(R.string.notification_service_content)))
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notificationID, Notification);

        notificationExists = true;
    }
}
