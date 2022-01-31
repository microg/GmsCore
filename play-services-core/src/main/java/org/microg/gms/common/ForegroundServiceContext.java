package org.microg.gms.common;

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

public class ForegroundServiceContext extends ContextWrapper {
    private static final String TAG = "ForegroundService";
    public static final String EXTRA_FOREGROUND = "foreground";

    public ForegroundServiceContext(Context base) {
        super(base);
    }

    @Override
    public ComponentName startService(Intent service) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !isIgnoringBatteryOptimizations()) {
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

    private static String getServiceName(Service service) {
        String serviceName = null;
        try {
            ForegroundServiceInfo annotation = service.getClass().getAnnotation(ForegroundServiceInfo.class);
            if (annotation != null) {
                if (annotation.res() != 0) {
                    try {
                        serviceName = service.getString(annotation.res());
                    } catch (Exception ignored) {
                    }
                }
                if (serviceName == null) {
                    serviceName = annotation.value();
                }
            }
        } catch (Exception ignored) {
        }
        if (serviceName == null) {
            serviceName = service.getClass().getSimpleName();
        }
        return serviceName;
    }

    public static void completeForegroundService(Service service, Intent intent, String tag) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && intent != null
                && intent.getBooleanExtra(EXTRA_FOREGROUND, false)) {
            String serviceName = getServiceName(service);
            Log.d(tag, "Started " + serviceName + " in foreground mode.");
            try {
                Notification notification = buildForegroundNotification(service, serviceName);
                service.startForeground(serviceName.hashCode(), notification);
                Log.d(tag, "Notification: " + notification);
            } catch (Exception e) {
                Log.w(tag, e);
            }
        }
    }

    private static Notification buildForegroundNotification(Context context, String serviceName) {
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
            Channel.setVibrationPattern(new long[]{0});
            Channel.setLockscreenVisibility(0);
            context.getSystemService(NotificationManager.class).createNotificationChannel(Channel);
        }
        return new NotificationCompat.Builder(context, "foreground-service")
                .setOngoing(true)
                .setContentIntent(notificationPendingIntent)
                .setSmallIcon(R.drawable.ic_foreground_notification)
                .setContentTitle(context.getResources().getString(R.string.small_notification_service_title))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(context.getResources().getString(R.string.big_notification_service_title))
                        .bigText(context.getResources().getString(R.string.notification_service_content)))
                .build();
    }
}
