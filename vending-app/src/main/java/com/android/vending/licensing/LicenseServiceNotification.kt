package com.android.vending.licensing

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.android.vending.R
import java.util.TreeSet


private const val TAG = "FakeLicenseNotification"
private const val GMS_PACKAGE_NAME = "com.google.android.gms"
private const val GMS_AUTH_INTENT_ACTION = "com.google.android.gms.auth.login.LOGIN"

private const val PREFERENCES_KEY_IGNORE_PACKAGES_LIST = "ignorePackages"
private const val PREFERENCES_FILE_NAME = "licensing"

private const val INTENT_KEY_IGNORE_PACKAGE_NAME = "package"
private const val INTENT_KEY_NOTIFICATION_ID = "id"

private const val CHANNEL_ID = "LicenseNotification"

fun Context.sendLicenseServiceNotification(
    callerPackageName: String,
    callerAppName: CharSequence,
    callerUid: Int
) {
    registerLicenseServiceNotificationChannel()

    val preferences = getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

    val ignoreList = preferences.getStringSet(PREFERENCES_KEY_IGNORE_PACKAGES_LIST, emptySet())
    for (ignoredPackage in ignoreList!!) {
        if (callerPackageName == ignoredPackage) {
            Log.d(TAG, "Not notifying about license check, as user has ignored " +
                    "notifications for package $ignoredPackage"
            )
            return
        }
    }

    val authIntent = Intent(this, SignInReceiver::class.java).apply {
        putExtra(INTENT_KEY_NOTIFICATION_ID, callerUid)
    }.let {
        PendingIntent.getBroadcast(
            this, callerUid * 2, it, PendingIntent.FLAG_IMMUTABLE
        )
    }

    val ignoreIntent = Intent(this, IgnoreReceiver::class.java).apply {
        putExtra(INTENT_KEY_IGNORE_PACKAGE_NAME, callerPackageName)
        putExtra(INTENT_KEY_NOTIFICATION_ID, callerUid)
    }.let {
        PendingIntent.getBroadcast(
            this, callerUid * 2 + 1, it, PendingIntent.FLAG_MUTABLE
        )
    }

    val contentText = getString(R.string.license_notification_body)
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setSound(null)
        .setContentTitle(getString(R.string.license_notification_title, callerAppName))
        .setContentText(contentText)
        .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
        .addAction(
            NotificationCompat.Action.Builder(
                null,
                getString(R.string.license_notification_sign_in),
                authIntent
            ).build()
        )
        .addAction(
            NotificationCompat.Action.Builder(
                null,
                getString(R.string.license_notification_ignore),
                ignoreIntent
            ).build()
        )
        .setAutoCancel(true)
        .build()

    val notificationManager = NotificationManagerCompat.from(this)
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    ) {
        notificationManager.notify(callerUid, notification)
    }
}

private fun Context.registerLicenseServiceNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.license_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description =
            getString(R.string.license_notification_channel_description)
        channel.setSound(null, null)

        val notificationManager = getSystemService(
            NotificationManager::class.java
        )
        notificationManager.createNotificationChannel(channel)
    }
}

class IgnoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Dismiss ignored notification

        NotificationManagerCompat.from(context)
            .cancel(intent.getIntExtra(INTENT_KEY_NOTIFICATION_ID, -1))

        val preferences =
            context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

        val ignoreList: MutableSet<String> = TreeSet(
            preferences.getStringSet(PREFERENCES_KEY_IGNORE_PACKAGES_LIST, emptySet())
        )

        val newIgnorePackage = intent.getStringExtra(INTENT_KEY_IGNORE_PACKAGE_NAME)
        if (newIgnorePackage == null) {
            Log.e(TAG, "Received no ignore package; can't add to ignore list.")
            return
        }

        Log.d(TAG, "Adding package $newIgnorePackage to ignore list")

        ignoreList.add(newIgnorePackage)
        preferences.edit()
            .putStringSet(PREFERENCES_KEY_IGNORE_PACKAGES_LIST, ignoreList)
            .apply()
    }
}

class SignInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        // Dismiss all notifications
        NotificationManagerCompat.from(context).cancelAll()

        Log.d(TAG, "Starting sign in activity")
        Intent(GMS_AUTH_INTENT_ACTION).apply {
            setPackage(GMS_PACKAGE_NAME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.let { context.startActivity(it) }
    }
}