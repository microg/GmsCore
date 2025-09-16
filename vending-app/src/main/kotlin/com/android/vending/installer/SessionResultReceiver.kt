/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.vending.installer

import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import org.microg.vending.ui.notifyInstallPrompt

@RequiresApi(21)
internal class SessionResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        val notifyId = intent.getIntExtra(KEY_NOTIFY_ID, -1)
        Log.d(TAG, "onReceive status: $status sessionId: $sessionId")
        try {
            when (status) {
                PackageInstaller.STATUS_SUCCESS -> {
                    Log.d(TAG, "SessionResultReceiver received a successful transaction")
                    if (sessionId != -1) {
                        pendingSessions[sessionId]?.apply { onSuccess() }
                        pendingSessions.remove(sessionId)
                    }
                }

                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    if (keyguardManager.isKeyguardLocked) {
                        handleKeyguardLocked(sessionId, notifyId, intent, context)
                    } else {
                        val extraIntent = intent.extras?.getParcelable(Intent.EXTRA_INTENT) as Intent?
                        extraIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        extraIntent?.run { ContextCompat.startActivity(context, this, null) }
                    }
                }

                else -> {
                    val errorMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Log.w(TAG, "SessionResultReceiver received a failed transaction result: $errorMessage")
                    if (sessionId != -1) {
                        val onResult = pendingSessions[sessionId]
                        if (onResult != null) {
                            onResult.apply { onFailure(errorMessage) }
                            pendingSessions.remove(sessionId)
                        } else {
                            //Prevent notifications from being removed after the process is killed
                            Log.d(TAG, "onReceive onResult is null")
                            val notificationManager = NotificationManagerCompat.from(context)
                            notificationManager.cancel(notifyId)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "SessionResultReceiver encountered error while handling session result", e)
            if (sessionId != -1) {
                pendingSessions[sessionId]?.apply { onFailure(e.message) }
            }
        }
    }

    private fun handleKeyguardLocked(sessionId: Int, notifyId: Int, intent: Intent, context: Context) {
        val errorMsg = "The screen is locked and waiting for the user to click the notification to install"
        Log.d(TAG, errorMsg)
        if (sessionId != -1) {
            val onResult = pendingSessions[sessionId]
            if (onResult != null) {
                onResult.apply { onFailure(errorMsg) }
                pendingSessions.remove(sessionId)
            } else {
                Log.d(TAG, "onReceive onResult is null")
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.cancel(notifyId)
            }
            val pendingIntent = PendingIntentCompat.getBroadcast(
                    context, sessionId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT, true
            )
            val packageName = intent.getStringExtra(KEY_PACKAGE_NAME)
            Log.d(TAG, "handleKeyguardLocked: $packageName notifyId:$notifyId")
            context.notifyInstallPrompt(packageName!!, notifyId, createPendingIntent(context, VENDING_INSTALL_ACTION, sessionId, pendingIntent)
                    , createPendingIntent(context, VENDING_INSTALL_DELETE_ACTION, sessionId, null))
        }
    }

    @RequiresApi(21)
    private fun createPendingIntent(context: Context, action: String, sessionId: Int, pendingIntent: PendingIntent? = null): PendingIntent {
        val installIntent = Intent(context.applicationContext, InstallReceiver::class.java).apply {
            this.action = action
            putExtra(SESSION_ID, sessionId)
            if (pendingIntent != null) {
                putExtra(SESSION_RESULT_RECEIVER_INTENT, pendingIntent)
            }
        }

        val pendingInstallIntent = PendingIntentCompat.getBroadcast(
                context.applicationContext,
                0,
                installIntent,
                PendingIntent.FLAG_UPDATE_CURRENT, false
        )
        return pendingInstallIntent!!
    }

    data class OnResult(
            val onSuccess: () -> Unit,
            val onFailure: (message: String?) -> Unit
    )

    companion object {
        val pendingSessions: MutableMap<Int, OnResult> = mutableMapOf()
        const val KEY_NOTIFY_ID = "notify_id"
        const val KEY_PACKAGE_NAME = "package_name"
    }
}