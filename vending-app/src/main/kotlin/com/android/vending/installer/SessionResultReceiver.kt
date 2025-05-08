package com.android.vending.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.io.File

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
                    val tempFiles = intent.getStringArrayListExtra(KEY_TEMP_FILES)
                    if (!tempFiles.isNullOrEmpty()) {
                        for (filePath in tempFiles) {
                            val file = File(filePath)
                            if (file.exists()) {
                                val deleted = file.delete()
                                Log.d(TAG, "Deleted temp file: $filePath, success: $deleted")
                            }
                        }
                    }
                    if (sessionId != -1) {
                        pendingSessions[sessionId]?.apply { onSuccess() }
                        pendingSessions.remove(sessionId)
                    }
                }

                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    val extraIntent = intent.extras?.getParcelable(Intent.EXTRA_INTENT) as Intent?
                    extraIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    extraIntent?.run { ContextCompat.startActivity(context, this, null) }
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

    data class OnResult(
            val onSuccess: () -> Unit,
            val onFailure: (message: String?) -> Unit
    )

    companion object {
        val pendingSessions: MutableMap<Int, OnResult> = mutableMapOf()
        const val KEY_TEMP_FILES = "temp_files"
        const val KEY_NOTIFY_ID = "notify_id"
    }
}