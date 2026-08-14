package org.microg.gms.common;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.PowerManager;
import android.provider.Settings;
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
        // Notification channel (high importance, mirrors MicroG-RE)
        String channelName = context.getString(R.string.foreground_service_notification_title);
        NotificationChannel channel = new NotificationChannel("foreground-service", channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        channel.setShowBadge(false);
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);

        // Title and text
        String appTitle = context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
        String notifyTitle = context.getString(R.string.foreground_service_notification_title);
        String firstLine = context.getString(R.string.foreground_service_notification_text, serviceName);
        String secondLine = context.getString(R.string.foreground_service_notification_big_text, appTitle);

        // Open battery optimizations settings
        Intent batteryOptimizationIntent = ForegroundServiceOemUtils.getBatteryOptimizationIntent(context);
        PendingIntent batteryPendingIntent = PendingIntent.getActivity(
                context, 0, batteryOptimizationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Open notification settings in foreground service category
        Intent notificationCategoryIntent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
                .putExtra(Settings.EXTRA_CHANNEL_ID, "foreground-service");
        PendingIntent notificationCategoryPendingIntent = PendingIntent.getActivity(
                context, 1, notificationCategoryIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Open settings activity when notification is tapped
        Intent mainSettingsIntent = new Intent();
        mainSettingsIntent.setClassName(context.getPackageName(), "org.microg.gms.ui.MainSettingsActivity");
        mainSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent mainSettingsPendingIntent = PendingIntent.getActivity(
                context, 2, mainSettingsIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Notification actions
        Action batteryAction = new Action.Builder(
                Icon.createWithResource(context, R.drawable.ic_battery_action),
                context.getString(R.string.foreground_action_battery_optimization),
                batteryPendingIntent
        ).build();

        Action notificationAction = new Action.Builder(
                Icon.createWithResource(context, R.drawable.ic_notification_action),
                context.getString(R.string.foreground_action_notification_settings),
                notificationCategoryPendingIntent
        ).build();

        Log.d(TAG, notifyTitle + " // " + firstLine + " // " + secondLine);

        return new Notification.Builder(context, channel.getId())
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_background_notify)
                .setContentTitle(notifyTitle)
                .setContentText(firstLine)
                .setStyle(new Notification.BigTextStyle().bigText(firstLine + "\n" + secondLine))
                .setPriority(Notification.PRIORITY_HIGH)
                .setShowWhen(false)
                .setContentIntent(mainSettingsPendingIntent)
                .addAction(batteryAction)
                .addAction(notificationAction)
                .build();
    }

}
