package org.microg.gms.common;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.microg.gms.base.core.R;

import static android.os.Build.VERSION.SDK_INT;

public class ForegroundServiceContext extends ContextWrapper {
    private static final String TAG = "ForegroundService";
    public static final String EXTRA_FOREGROUND = "foreground";

    public ForegroundServiceContext(Context base) {
        super(base);
    }

    @Override
    public ComponentName startService(Intent service) {
        if (SDK_INT >= 26 && !isIgnoringBatteryOptimizations()) {
            Log.d(TAG, "Starting in foreground mode.");
            service.putExtra(EXTRA_FOREGROUND, true);
            return super.startForegroundService(service);
        }
        return super.startService(service);
    }

    @RequiresApi(23)
    private boolean isIgnoringBatteryOptimizations() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        return powerManager.isIgnoringBatteryOptimizations(getPackageName());
    }

    private static String getServiceName(Service service) {
        String serviceName = null;
        try {
            ForegroundServiceInfo annotation = service.getClass().getAnnotation(ForegroundServiceInfo.class);
            if (annotation != null) {
                serviceName = annotation.value();
                if (annotation.res() != 0) {
                    try {
                        serviceName = service.getString(annotation.res());
                    } catch (Exception ignored) {
                    }
                }
                if (!annotation.resName().isEmpty() && !annotation.resPackage().isEmpty()) {
                    try {
                        serviceName = service.getString(service.getResources().getIdentifier(annotation.resName(), "string", annotation.resPackage()));
                    } catch (Exception ignored) {
                    }
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
        if (intent != null && intent.getBooleanExtra(EXTRA_FOREGROUND, false) && SDK_INT >= 26) {
            String serviceName = getServiceName(service);
            Log.d(tag, "Started " + serviceName + " in foreground mode.");
            try {
                Notification notification = buildForegroundNotification(service, serviceName);
                service.startForeground(serviceName.hashCode(), notification);
                Log.d(tag, "Notification: " + notification.toString());
            } catch (Exception e) {
                Log.w(tag, e);
            }
        }
    }

    @RequiresApi(26)
    private static Notification buildForegroundNotification(Context context, String serviceName) {
        NotificationChannel channel = new NotificationChannel("foreground-service", "Foreground Service", NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        channel.setShowBadge(false);
        channel.setVibrationPattern(new long[]{0});
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        String appTitle = context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
        String notifyTitle = context.getString(R.string.foreground_service_notification_title);
        String firstLine = context.getString(R.string.foreground_service_notification_text, serviceName);
        String secondLine = context.getString(R.string.foreground_service_notification_big_text, appTitle);
        Log.d(TAG, notifyTitle + " // " + firstLine + " // " + secondLine);
        return new Notification.Builder(context, channel.getId())
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_background_notify)
                .setContentTitle(notifyTitle)
                .setContentText(firstLine)
                .setStyle(new Notification.BigTextStyle().bigText(firstLine + "\n" + secondLine))
                .build();
    }

}
