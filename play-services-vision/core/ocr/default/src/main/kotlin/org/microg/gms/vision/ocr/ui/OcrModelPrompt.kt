/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.ocr.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.microg.gms.vision.ocr.R
import androidx.core.content.edit

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
    private const val PREFERENCES = "ocr-model-prompts"

    @Synchronized
    fun notifyMissing(context: Context, pack: String, remainingBytes: Long) {
        if (android.os.Build.VERSION.SDK_INT < 26) return
        val label = context.packLabel(pack) ?: return
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        if (preferences.getBoolean(pack, false)) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.ocr_models_title), NotificationManager.IMPORTANCE_DEFAULT)
        )
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) return

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
        preferences.edit { putBoolean(pack, true) }
    }

    @Synchronized
    fun markHandled(context: Context, pack: String) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit { putBoolean(pack, true) }
    }

    @Synchronized
    fun downloadFinished(context: Context, pack: String) {
        markHandled(context, pack)
        NotificationManagerCompat.from(context).cancel(CHANNEL_ID, pack.hashCode())
    }

    @Synchronized
    fun clear(context: Context, pack: String) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit { remove(pack) }
        NotificationManagerCompat.from(context).cancel(CHANNEL_ID, pack.hashCode())
    }
}
