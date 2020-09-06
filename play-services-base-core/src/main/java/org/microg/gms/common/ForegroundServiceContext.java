package org.microg.gms.common;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.List;

public class ForegroundServiceContext extends ContextWrapper {
    private static final String TAG = "ForegroundService";
    public static final String EXTRA_FOREGROUND = "foreground";

    public ForegroundServiceContext(Context base) {
        super(base);
    }

    @Override
    public ComponentName startService(Intent service) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isIgnoringBatteryOptimizations() && !isAppOnForeground()) {
            Log.d(TAG, "Starting in foreground mode.");
            service.putExtra(EXTRA_FOREGROUND, true);
            return super.startForegroundService(service);
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
        if (intent != null && intent.getBooleanExtra(EXTRA_FOREGROUND, false) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(tag, "Started in foreground mode.");
            service.startForeground(tag.hashCode(), buildForegroundNotification(service));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static Notification buildForegroundNotification(Context context) {
        NotificationChannel channel = new NotificationChannel("foreground-service", "Foreground Service", NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        channel.setShowBadge(false);
        channel.setVibrationPattern(new long[0]);
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        return new Notification.Builder(context, channel.getId())
                .setOngoing(true)
                .setContentTitle("Running in background")
                //.setSmallIcon(R.drawable.ic_cloud_bell)
                .build();
    }
}
