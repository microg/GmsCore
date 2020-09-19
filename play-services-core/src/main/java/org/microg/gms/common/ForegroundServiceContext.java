package org.microg.gms.common;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.mgoogle.android.gms.R;

import java.util.List;

public class ForegroundServiceContext extends ContextWrapper {
    private static final String TAG = "ForegroundService";
    public static final String EXTRA_FOREGROUND = "foreground";

    public ForegroundServiceContext(Context base) {
        super(base);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public ComponentName startService(Intent service) {
        if (!isIgnoringBatteryOptimizations() && !isAppOnForeground()) {
            Log.d(TAG, "Starting in foreground mode.");
            service.putExtra(EXTRA_FOREGROUND, true);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return super.startForegroundService(service);
            } else {
                return super.startService(service);
            }
        }
        return super.startService(service);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean isIgnoringBatteryOptimizations() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        return powerManager.isIgnoringBatteryOptimizations(getPackageName());
    }

    private boolean isAppOnForeground() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public static void completeForegroundService(Service service, Intent intent, String tag) {
        if (intent != null && intent.getBooleanExtra(EXTRA_FOREGROUND, false)) {
            Log.d(tag, "Started in foreground mode.");
            service.startForeground(tag.hashCode(), buildForegroundNotification(service));
        }
    }

    private static Notification buildForegroundNotification(Context context) {
        Intent notificationIntent = new Intent();
        notificationIntent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(context,
                0,
                notificationIntent,
                0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel Channel = new NotificationChannel("foreground-service",
                    context.getResources().getString(R.string.notification_service_name),
                    NotificationManager.IMPORTANCE_LOW);
            Channel.setShowBadge(false);
            Channel.setLockscreenVisibility(0);
            Channel.setVibrationPattern(new long[0]);
            context.getSystemService(NotificationManager.class).createNotificationChannel(Channel);
        }
        return new NotificationCompat.Builder(context, "foreground-service")
                .setOngoing(true)
                .setContentIntent(notificationPendingIntent)
                .setSmallIcon(R.drawable.ic_foreground_notification)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(context.getResources().getString(R.string.notification_service_title))
                        .bigText(context.getResources().getString(R.string.notification_service_content)))
                .build();
    }
}
