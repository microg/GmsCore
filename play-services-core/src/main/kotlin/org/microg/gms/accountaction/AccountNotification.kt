package org.microg.gms.accountaction

import android.Manifest
import android.accounts.Account
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.R

private const val CHANNEL_ID = "AccountNotification"


@RequiresApi(21)
fun Context.sendAccountActionNotification(account: Account, action: UserSatisfyRequirements) {

    registerAccountNotificationChannel()

    val intent: PendingIntent = AccountActionActivity.createIntent(this, account, action).let {
        PendingIntent.getActivity(
            this,
            account.hashCode(),
            it,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    val notification: Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_manage_accounts)
            .setSound(null)
            .setOnlyAlertOnce(true)
            .setContentTitle(getString(R.string.auth_action_notification_title))
            .setContentText(getString(R.string.auth_action_notification_content))
            .setSubText(account.name)
            .setOnlyAlertOnce(true)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()

    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    ) {
        NotificationManagerCompat.from(this).notify(account.hashCode(), notification)
    }

}

fun Context.registerAccountNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.auth_action_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.auth_action_notification_channel_description)
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}