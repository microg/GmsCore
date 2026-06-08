/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory.db.backup

import android.accounts.AccountManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.microg.gms.auth.AuthConstants
import org.microg.gms.feature.GoogleFeaturePreferences

private const val TAG = "OdlhBackupService"

class OdlhBackupService : LifecycleService() {

    companion object {
        private const val ACTION_BACKUP = "com.google.android.gms.semanticlocationhistory.ACTION_PERIODIC_BACKUP"
        private const val BACKUP_INTERVAL_MS = 8 * 60 * 60 * 1000L // 8h

        fun scheduleBackup(context: Context) {
            val allowedMapsTimelineFeature = GoogleFeaturePreferences.allowedMapsTimelineFeature(context)
            if (!allowedMapsTimelineFeature) {
                Log.w(TAG, "scheduleBackup: not allowed report")
                return
            }
            val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, OdlhBackupService::class.java).setAction(ACTION_BACKUP)
            val pendingIntent = PendingIntent.getService(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + BACKUP_INTERVAL_MS,
                BACKUP_INTERVAL_MS,
                pendingIntent
            )
            Log.d(TAG, "scheduleBackup: periodic backup scheduled every ${BACKUP_INTERVAL_MS / 3600000}h")
        }

        fun cancelBackup(context: Context) {
            val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, OdlhBackupService::class.java).setAction(ACTION_BACKUP)
            val pendingIntent = PendingIntent.getService(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "cancelBackup: periodic backup cancelled")
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_BACKUP) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        Log.d(TAG, "onStartCommand: periodic backup triggered")

        lifecycleScope.launch {
            try {
                performBackupForAllAccounts()
            } catch (e: Exception) {
                Log.e(TAG, "onStartCommand: backup failed", e)
            } finally {
                stopSelf(startId)
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun performBackupForAllAccounts() {
        val accounts = AccountManager.get(this)
            .getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)

        if (accounts.isEmpty()) {
            Log.d(TAG, "performBackupForAllAccounts: no Google accounts found")
            return
        }

        Log.d(TAG, "performBackupForAllAccounts: ${accounts.size} account(s)")

        for (account in accounts) {
            try {
                Log.d(TAG, "performBackupForAllAccounts: starting for ${account.name}")
                val result = OdlhBackupProcessor.performIncrementalBackup(this, account.name)
                Log.d(TAG, "performBackupForAllAccounts: ${account.name} result=$result")
            } catch (e: Exception) {
                Log.e(TAG, "performBackupForAllAccounts: failed for ${account.name}", e)
            }
        }
    }
}

class OdlhBackupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "onReceive: ${intent.action}, re-scheduling backup")
                OdlhBackupService.scheduleBackup(context)
            }
        }
    }
}