package com.android.vending.licensing;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.vending.R;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class LicenseServiceNotificationRunnable implements Runnable {

    private final Context context;

    public String callerPackageName;
    public CharSequence callerAppName;
    public int callerUid;

    private static final String TAG = "FakeLicenseNotification";
    private static final String GMS_PACKAGE_NAME = "com.google.android.gms";
    private static final String GMS_AUTH_INTENT_ACTION = "com.google.android.gms.auth.login.LOGIN";

    private static final String PREFERENCES_KEY_IGNORE_PACKAGES_LIST = "ignorePackages";
    private static final String PREFERENCES_FILE_NAME = "licensing";

    private static final String INTENT_KEY_IGNORE_PACKAGE_NAME = "package";
    private static final String INTENT_KEY_NOTIFICATION_ID = "id";


    public LicenseServiceNotificationRunnable(Context context) {
        this.context = context;
    }

    private static final String CHANNEL_ID = "LicenseNotification";

    @Override
    public void run() {
        registerNotificationChannel();

        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);

        Set<String> ignoreList = preferences.getStringSet(PREFERENCES_KEY_IGNORE_PACKAGES_LIST, Collections.emptySet());
        for (String ignoredPackage : ignoreList) {
            if (callerPackageName.equals(ignoredPackage)) {
                Log.d(TAG, "Not notifying about license check, as user has ignored notifications for package " + ignoredPackage);
                return;
            }
        }

        Intent authIntent = new Intent(context, LicenseServiceNotificationRunnable.SignInReceiver.class);
        authIntent.putExtra(INTENT_KEY_NOTIFICATION_ID, callerUid);
        PendingIntent authPendingIntent = PendingIntent.getBroadcast(
            context, callerUid * 2, authIntent, PendingIntent.FLAG_IMMUTABLE
        );

        Intent ignoreIntent = new Intent(context, LicenseServiceNotificationRunnable.IgnoreReceiver.class);
        ignoreIntent.putExtra(INTENT_KEY_IGNORE_PACKAGE_NAME, callerPackageName);
        ignoreIntent.putExtra(INTENT_KEY_NOTIFICATION_ID, callerUid);
        PendingIntent ignorePendingIntent = PendingIntent.getBroadcast(
            context, callerUid * 2 + 1, ignoreIntent, PendingIntent.FLAG_MUTABLE
        );

        String contentText = context.getString(R.string.license_notification_body);
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setSound(null)
            .setContentTitle(context.getString(R.string.license_notification_title, callerAppName))
            .setContentText(contentText)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
            .addAction(
                new NotificationCompat.Action.Builder(
                    null, context.getString(R.string.license_notification_sign_in), authPendingIntent
                ).build()
            )
            .addAction(
                new NotificationCompat.Action.Builder(
                    null, context.getString(R.string.license_notification_ignore), ignorePendingIntent
                ).build()
            )
            .setAutoCancel(true)
            .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(callerUid, notification);
        }
    }

    private void registerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.license_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.license_notification_channel_description));
            channel.setSound(null, null);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

    }

    public static final class IgnoreReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            // Dismiss ignored notification
            NotificationManagerCompat.from(context)
                .cancel(intent.getIntExtra(INTENT_KEY_NOTIFICATION_ID, -1));

            SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);

            Set<String> ignoreList = new TreeSet<>(
                preferences.getStringSet(PREFERENCES_KEY_IGNORE_PACKAGES_LIST, Collections.emptySet())
            );

            String newIgnorePackage = intent.getStringExtra(INTENT_KEY_IGNORE_PACKAGE_NAME);
            if (newIgnorePackage == null) {
                Log.e(TAG, "Received no ignore package; can't add to ignore list.");
                return;
            }

            Log.d(TAG, "Adding package " + newIgnorePackage + " to ignore list");

            ignoreList.add(newIgnorePackage);
            preferences.edit().putStringSet(PREFERENCES_KEY_IGNORE_PACKAGES_LIST, ignoreList).apply();
        }
    }

    public static final class SignInReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            // Dismiss all notifications
            NotificationManagerCompat.from(context).cancelAll();

            Log.d(TAG, "Starting sign in activity");
            Intent authIntent = new Intent(GMS_AUTH_INTENT_ACTION);
            authIntent.setPackage(GMS_PACKAGE_NAME);
            authIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(authIntent);
        }
    }

}
