package org.microg.gms.common;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;

import com.mgoogle.android.gms.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StatusNotification {

    private static int notificationID;

    public static boolean Notify(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!powerManager.isIgnoringBatteryOptimizations(context.getPackageName())) {
                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                StatusBarNotification[] notifications = manager.getActiveNotifications();
                for (StatusBarNotification notification : notifications) {
                    if (notification.getId() == notificationID) {
                        return false;
                    }
                }

                buildStatusNotification(context);
            }
        }
        return true;
    }

    private static void buildStatusNotification(Context context) {
        notificationID = createNotificationID();

        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        String notificationChannelID = "foreground-service";

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationChannelID, context.getResources().getString(R.string.notification_service_name), NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(0);
            channel.setVibrationPattern(new long[0]);
            context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, notificationChannelID)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setContentTitle(context.getResources().getString(R.string.notification_service_title))
                .setContentText(context.getResources().getString(R.string.notification_service_content))
                .setSmallIcon(R.drawable.ic_foreground_notification);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify(notificationID, notification.build());
    }

    private static int createNotificationID() {
        Date currentDate = new Date();
        int id = Integer.parseInt(new SimpleDateFormat("ddHHmmss",  Locale.US).format(currentDate));

        return id;
    }
}
