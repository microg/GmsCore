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
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import com.google.android.gms.R
import org.microg.gms.auth.login.LoginActivity

private const val CHANNEL_ID = "AccountNotification"

@RequiresApi(21)
fun Context.sendAccountReAuthNotification(account: Account) {
    Log.d(TAG, "sendAccountReAuthNotification: account: ${account.name}")

    registerAccountNotificationChannel()

    val intent = Intent(this, LoginActivity::class.java).apply {
        putExtra(LoginActivity.EXTRA_RE_AUTH_ACCOUNT, account)
    }.let {
        PendingIntentCompat.getActivity(
            this, account.hashCode(), it, PendingIntent.FLAG_CANCEL_CURRENT, false
        )
    }

    val notification: Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_manage_accounts)
            .setSound(null)
            .setContentTitle(getString(R.string.auth_action_reauth_notification_title))
            .setContentText(account.name)
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

@RequiresApi(21)
fun Context.sendAccountActionNotification(account: Account, action: UserSatisfyRequirements) {

    registerAccountNotificationChannel()

    val intent: PendingIntent? = AccountActionActivity.createIntent(this, account, action).let {
        PendingIntentCompat.getActivity(
            this,
            account.hashCode(),
            it,
            PendingIntent.FLAG_CANCEL_CURRENT,
            false
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
    if (SDK_INT >= 26) {
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

fun Context.cancelAccountNotificationChannel(account: Account) {
    NotificationManagerCompat.from(this).cancel(account.hashCode())
}