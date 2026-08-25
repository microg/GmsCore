/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.location.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import org.microg.gms.location.ACTION_CONFIGURATION_REQUIRED
import org.microg.gms.location.CONFIGURATION_FIELD_ONLINE_SOURCE
import org.microg.gms.location.EXTRA_CONFIGURATION
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import org.microg.gms.location.core.R

class ConfigurationRequiredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CONFIGURATION_REQUIRED) {
            if (SDK_INT >= 23 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            val channel = NotificationChannelCompat.Builder("location", NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.service_name_location)).build()
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
            val notification = NotificationCompat.Builder(context, channel.id)
                .setContentTitle(context.getText(R.string.notification_config_required_title))
                .setSmallIcon(R.drawable.ic_location)
                .setAutoCancel(true)
            when (intent.getStringExtra(EXTRA_CONFIGURATION)) {
                CONFIGURATION_FIELD_ONLINE_SOURCE -> {
                    val notifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse("x-gms-settings://location"))
                        .apply {
                            `package` = context.packageName
                            putExtra(EXTRA_CONFIGURATION, CONFIGURATION_FIELD_ONLINE_SOURCE)
                        }
                    notification.setContentText(context.getText(R.string.notification_config_required_text_online_sources))
                        .setStyle(NotificationCompat.BigTextStyle())
                        .setContentIntent(PendingIntentCompat.getActivity(context, CONFIGURATION_FIELD_ONLINE_SOURCE.hashCode(), notifyIntent, 0, true))
                }
                else -> return
            }
            NotificationManagerCompat.from(context).notify(CONFIGURATION_FIELD_ONLINE_SOURCE.hashCode(), notification.build())
        }
    }
}
