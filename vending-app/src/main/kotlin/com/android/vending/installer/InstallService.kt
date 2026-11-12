/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.vending.installer

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.runBlocking
import org.microg.vending.billing.core.AuthData
import org.microg.vending.enterprise.AppState
import org.microg.vending.enterprise.EnterpriseApp
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RequiresApi(21)
class InstallService : Service() {

    private val binder = LocalBinder()

    private val runningThreads = AtomicInteger(0)

    /**
     * Note: `isForeground` can be `false` even if the service is actually
     * running in the foreground while installing a dependency, see below.
     * State `false` means that the next notification must be designated as
     * foreground notification.
     */
    private val isForeground = AtomicBoolean(false)

    internal var auth: AuthData? = null
    internal lateinit var apps: MutableMap<EnterpriseApp, AppState>

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun installAsync(app: EnterpriseApp, isUpdate: Boolean) =
        Thread {
            runningThreads.incrementAndGet()
            runBlocking { install(app, isUpdate) }
            if (runningThreads.decrementAndGet() == 0) {
                // Demote ourselves explicitly – notification cannot be removed otherwise
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }.start()

    suspend fun install(app: EnterpriseApp, isUpdate: Boolean) {
        EnterpriseAppInstaller(this, auth!!, apps, ::promoteToForeground).install(app, isUpdate)
    }

    private fun promoteToForeground(sessionId: Int, notification: Notification, isDependency: Boolean) {
        val alreadyForeground = if (isDependency) isForeground.get() else isForeground.getAndSet(true)

        if (!alreadyForeground) {
            ServiceCompat.startForeground(
                this, sessionId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): InstallService = this@InstallService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

}