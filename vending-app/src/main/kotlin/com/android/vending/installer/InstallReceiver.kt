package com.android.vending.installer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(21)
class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: " + intent.action)
        val sessionId = intent.getIntExtra(SESSION_ID, -1)
        Log.d(TAG, "onReceive sessionId: $sessionId")
        if (intent.action == VENDING_INSTALL_ACTION) {
            if (sessionId != -1) {
                val packageInstaller = context.packageManager.packageInstaller
                var session: PackageInstaller.Session? = null
                session = packageInstaller.openSession(sessionId)
                Log.d(TAG, "onReceive: $session")
                val pendingIntent = intent.getParcelableExtra<PendingIntent>(SESSION_RESULT_RECEIVER_INTENT)
                if (pendingIntent != null) {
                    session.commit(pendingIntent.intentSender)
                }
            }
        } else if (intent.action == VENDING_INSTALL_DELETE_ACTION) {
            if (sessionId != -1) {
                val packageInstaller = context.packageManager.packageInstaller
                val session = packageInstaller.openSession(sessionId)
                session.abandon()
            }
        }
    }
}