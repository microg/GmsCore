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

import androidx.core.app.NotificationCompat;

import com.mgoogle.android.gms.R;

import java.util.List;

public class ForegroundServiceContext extends ContextWrapper {
    private static final String TAG = "ForegroundService";
    public static final String EXTRA_FOREGROUND = "foreground";

    public ForegroundServiceContext(Context base) {
        super(base);
    }

    @Override
    public ComponentName startService(Intent service) {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        if (!powerManager.isIgnoringBatteryOptimizations(this.getPackageName()) && !isAppOnForeground()) {
            Log.d(TAG, "Starting in foreground mode.");
            service.putExtra(EXTRA_FOREGROUND, true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return super.startForegroundService(service);
            } else {
                return super.startService(service);
            }
        }
        return super.startService(service);
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
        if (intent.getBooleanExtra(EXTRA_FOREGROUND, false)) {
            Log.d(tag, "Started in foreground mode.");
            service.startForeground(tag.hashCode(), buildForegroundNotification(service));
        }
    }

    private static Notification buildForegroundNotification(Context context) {
        Intent mIntent = new Intent();
        mIntent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mIntent, 0);

        return new NotificationCompat.Builder(context, "foreground-service")
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setContentTitle(context.getResources().getString(R.string.notification_service_title))
                .setContentText(context.getResources().getString(R.string.notification_service_content))
                .setSmallIcon(R.drawable.ic_foreground_notification)
                .build();
    }
}
