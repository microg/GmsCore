/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.text.format.Formatter
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.microg.gms.vision.core.R
import java.io.File
import java.io.IOException

internal const val EXTRA_OCR_PACK = "org.microg.gms.vision.ocr.PACK"

internal fun Context.packLabel(pack: String): String? = when (pack) {
    "latin" -> getString(R.string.ocr_models_latin)
    "cjk" -> getString(R.string.ocr_models_cjk)
    "korean" -> getString(R.string.ocr_models_korean)
    "devanagari" -> getString(R.string.ocr_models_devanagari)
    else -> null
}

internal object OcrModelPrompt {
    private const val CHANNEL_ID = "ocr-models"

    // A restored preference does not prove a notification was shown on this device.
    private fun marker(context: Context, pack: String): File? =
        ContextCompat.getNoBackupFilesDir(context)?.let { File(it, "ocr-model-prompt-$pack") }

    fun blockedNotificationSettingsIntent(context: Context): Intent? {
        val action = blockedNotificationSettingsAction(context) ?: return null
        return Intent(action)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .apply {
                if (action == Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS) {
                    putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
                }
            }
    }

    private fun blockedNotificationSettingsAction(context: Context): String? {
        if (android.os.Build.VERSION.SDK_INT < 26) return null
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Settings.ACTION_APP_NOTIFICATION_SETTINGS
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return Settings.ACTION_APP_NOTIFICATION_SETTINGS
        }
        val channel = context.getSystemService(NotificationManager::class.java)?.getNotificationChannel(CHANNEL_ID)
        return if (channel?.importance == NotificationManager.IMPORTANCE_NONE) Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS else null
    }

    @Synchronized
    fun notifyMissing(context: Context, pack: String, remainingBytes: Long) {
        if (android.os.Build.VERSION.SDK_INT < 26) return
        val label = context.packLabel(pack) ?: return
        if (marker(context, pack)?.isFile == true) return
        // A first channel can trigger a system permission prompt for apps targeting Android 12L or earlier.
        if (blockedNotificationSettingsAction(context) != null) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.ocr_models_title), NotificationManager.IMPORTANCE_DEFAULT)
        )

        val intent = Intent(context, OcrModelsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_OCR_PACK, pack)
        val pendingIntent = PendingIntent.getActivity(context, pack.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.ocr_models_notice_title, label))
            .setContentText(context.getString(R.string.ocr_models_notice_text, Formatter.formatFileSize(context, remainingBytes)))
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .build()
        manager.notify(CHANNEL_ID, pack.hashCode(), notification)
        markHandled(context, pack)
    }

    @Synchronized
    private fun markHandled(context: Context, pack: String) {
        try {
            marker(context, pack)?.createNewFile()
        } catch (e: IOException) {
            Log.w("OcrModelPrompt", "Could not record model prompt", e)
        }
    }

    @Synchronized
    fun downloadFinished(context: Context, pack: String) {
        markHandled(context, pack)
        NotificationManagerCompat.from(context).cancel(CHANNEL_ID, pack.hashCode())
    }

    @Synchronized
    fun clear(context: Context, pack: String) {
        marker(context, pack)?.delete()
        NotificationManagerCompat.from(context).cancel(CHANNEL_ID, pack.hashCode())
    }
}
