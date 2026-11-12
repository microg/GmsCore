package com.android.vending.installer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(21)
class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive: $action")
        if (action != VENDING_INSTALL_ACTION && action != VENDING_INSTALL_DELETE_ACTION) return
        val sessionId = intent.getIntExtra(SESSION_ID, -1)
        Log.d(TAG, "onReceive sessionId: $sessionId")
        if (sessionId == -1) return
        try {
            val packageInstaller = context.packageManager.packageInstaller
            if (packageInstaller.getSessionInfo(sessionId) == null) return
            packageInstaller.openSession(sessionId).use { session ->
                if (action == VENDING_INSTALL_ACTION) {
                    intent.getParcelableExtra<PendingIntent>(SESSION_RESULT_RECEIVER_INTENT)?.let {
                        session.commit(it.intentSender)
                    }
                } else {
                    session.abandon()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to handle installer session $sessionId (${e.javaClass.simpleName})")
        }
    }
}
