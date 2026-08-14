/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.annotation.TargetApi
import android.app.*
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import android.util.TypedValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.LifecycleService
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import org.microg.gms.common.ForegroundServiceContext
import org.microg.gms.common.ForegroundServiceInfo
import org.microg.gms.nearby.core.R

@ForegroundServiceInfo("Exposure Notification")
class NotifyService : LifecycleService() {
    private val notificationId = NotifyService::class.java.name.hashCode()
    private val trigger = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            updateNotification()
        }
    }

    @TargetApi(26)
    private fun createNotificationChannel(): String {
        val channel = NotificationChannel("exposure-notifications", "Exposure Notifications", NotificationManager.IMPORTANCE_HIGH)
        channel.setSound(null, null)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        channel.setShowBadge(true)
        if (SDK_INT >= 29) {
            channel.setAllowBubbles(false)
        }
        channel.vibrationPattern = longArrayOf(0)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return channel.id
    }

    @TargetApi(21)
    private fun updateNotification() {
        val location = !LocationManagerCompat.isLocationEnabled(getSystemService(Context.LOCATION_SERVICE) as LocationManager)
        val bluetooth = BluetoothAdapter.getDefaultAdapter()?.state.let { it != BluetoothAdapter.STATE_ON && it != BluetoothAdapter.STATE_TURNING_ON }
        val nearbyPermissions = arrayOf("android.permission.BLUETOOTH_ADVERTISE", "android.permission.BLUETOOTH_SCAN")
        val permissionNeedsHandling = SDK_INT >= 31 && nearbyPermissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        Log.d( TAG,"notify: location: $location, bluetooth: $bluetooth, permissionNeedsHandling: $permissionNeedsHandling")

        val text: String = when {
            permissionNeedsHandling -> getString(R.string.exposure_notify_off_nearby)
            location && bluetooth -> getString(R.string.exposure_notify_off_bluetooth_location)
            location -> getString(R.string.exposure_notify_off_location)
            bluetooth -> getString(R.string.exposure_notify_off_bluetooth)
            else -> {
                NotificationManagerCompat.from(this).cancel(notificationId)
                return
            }
        }

        if (SDK_INT >= 26) {
            NotificationCompat.Builder(this, createNotificationChannel())
        } else {
            NotificationCompat.Builder(this)
        }.apply {
            val typedValue = TypedValue()
            try {
                var resolved = theme.resolveAttribute(androidx.appcompat.R.attr.colorError, typedValue, true)
                if (!resolved && SDK_INT >= 26) resolved = theme.resolveAttribute(android.R.attr.colorError, typedValue, true)
                color = if (resolved) {
                    ContextCompat.getColor(this@NotifyService, typedValue.resourceId)
                } else {
                    Color.RED
                }
                if (SDK_INT >= 26) setColorized(true)
            } catch (e: Exception) {
                // Ignore
            }
            setSmallIcon(R.drawable.ic_virus_outline)
            setContentTitle(getString(R.string.exposure_notify_off_title))
            setContentText(text)
            setStyle(NotificationCompat.BigTextStyle())
            try {
                val intent = Intent(ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS).apply { `package` = packageName }
                intent.resolveActivity(packageManager)
                setContentIntent(PendingIntentCompat.getActivity(this@NotifyService, notificationId, Intent(ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS).apply { `package` = packageName }, FLAG_UPDATE_CURRENT, false))
            } catch (e: Exception) {
                // Ignore
            }
        }.let {
            NotificationManagerCompat.from(this).notify(notificationId, it.build())
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(trigger, IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            if (SDK_INT >= 19) addAction(LocationManager.MODE_CHANGED_ACTION)
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(NOTIFICATION_UPDATE_ACTION)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ForegroundServiceContext.completeForegroundService(this, intent, TAG)
        Log.d(TAG, "NotifyService.start: $intent")
        super.onStartCommand(intent, flags, startId)
        updateNotification()
        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        NotificationManagerCompat.from(this).cancel(notificationId)
        unregisterReceiver(trigger)
    }

    companion object {
        fun isNeeded(context: Context): Boolean {
            return ExposurePreferences(context).let { it.enabled }
        }
    }
}
