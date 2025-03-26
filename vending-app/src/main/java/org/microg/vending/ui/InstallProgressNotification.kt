/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.android.vending.R
import org.microg.gms.ui.TAG
import org.microg.vending.enterprise.CommitingSession
import org.microg.vending.enterprise.Downloading
import org.microg.vending.enterprise.InstallComplete
import org.microg.vending.enterprise.InstallError
import org.microg.vending.enterprise.InstallProgress

private const val INSTALL_NOTIFICATION_CHANNEL_ID = "packageInstall"

internal fun Context.notifySplitInstallProgress(packageName: String, sessionId: Int, progress: InstallProgress) {

    val label = try {
        packageManager.getPackageInfo(packageName, 0).applicationInfo
            .loadLabel(packageManager)
    } catch (e: NameNotFoundException) {
        Log.e(TAG, "Couldn't load label for $packageName (${e.message}). Is it not installed?")
        return
    }

    createNotificationChannel()

    val notificationManager = NotificationManagerCompat.from(this)

    when (progress) {
        is Downloading -> getDownloadNotificationBuilder().apply {
            setContentTitle(getString(R.string.installer_notification_progress_splitinstall_downloading, label))
            setProgress(progress.bytesDownloaded.toInt(), progress.bytesTotal.toInt(), false)
        }
        CommitingSession -> getDownloadNotificationBuilder().apply {
            setContentTitle(getString(R.string.installer_notification_progress_splitinstall_commiting, label))
            setProgress(0, 1, true)
        }
        else -> null.also { notificationManager.cancel(sessionId) }
    }?.apply {
        setOngoing(true)

        notificationManager.notify(sessionId, this.build())
    }

}

/**
 * @return The notification after it had been posted _if_ it is an ongoing notification.
 */
internal fun Context.notifyInstallProgress(
    displayName: String,
    sessionId: Int,
    progress: InstallProgress,
    isDependency: Boolean = false
): Notification? {

    createNotificationChannel()
    getDownloadNotificationBuilder().apply {

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (progress) {
            is Downloading -> {
                setContentTitle(
                    getString(
                        if (isDependency) R.string.installer_notification_progress_splitinstall_downloading
                        else R.string.installer_notification_progress_downloading,
                        displayName
                    )
                )
                setProgress(progress.bytesTotal.toInt(), progress.bytesDownloaded.toInt(), false)
                setOngoing(true)
                return this.build().also { notificationManager.notify(sessionId, it) }
            }
            CommitingSession -> {
                setContentTitle(
                    getString(
                        if (isDependency) R.string.installer_notification_progress_splitinstall_commiting
                        else R.string.installer_notification_progress_commiting,
                        displayName
                    )
                )
                setProgress(0, 0, true)
                setOngoing(true)
                return this.build().also { notificationManager.notify(sessionId, it) }
            }
            InstallComplete -> {
                if (!isDependency) {
                    setContentTitle(
                        getString(
                            R.string.installer_notification_progress_complete,
                            displayName
                        )
                    )
                    setSmallIcon(android.R.drawable.stat_sys_download_done)
                    notificationManager.notify(sessionId, this.build())
                } else {
                    notificationManager.cancel(sessionId)
                }
                return null
            }
            is InstallError -> {
                if (!isDependency) {
                    setContentTitle(
                        getString(
                            R.string.installer_notification_progress_failed,
                            displayName
                        )
                    )
                    setSmallIcon(android.R.drawable.stat_notify_error)
                    // see `InstallComplete` case
                    notificationManager.notify(sessionId, this.build())
                } else {
                    notificationManager.cancel(sessionId)
                }
                return null
            }
        }
    }

}

private fun Context.getDownloadNotificationBuilder() =
    NotificationCompat.Builder(this, INSTALL_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setLocalOnly(true)

private fun Context.createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                INSTALL_NOTIFICATION_CHANNEL_ID,
                getString(R.string.installer_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.installer_notification_channel_description)
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
            }
        )
    }
}