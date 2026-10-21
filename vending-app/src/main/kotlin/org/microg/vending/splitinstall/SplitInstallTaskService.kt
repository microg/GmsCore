/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.splitinstall

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.finsky.splitinstallservice.SplitInstallManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.microg.vending.enterprise.Downloading
import org.microg.vending.ui.notifyInstallProgress
import org.microg.gms.profile.ProfileManager

private const val TAG = "SplitInstallTaskService"
private const val NOTIFICATION_ID = 0x53504c55

@RequiresApi(21)
class SplitInstallTaskService : LifecycleService() {
    private val workMutex = Mutex()
    private var activeTasks = 0
    private var latestStartId = 0

    override fun onCreate() {
        super.onCreate()
        ProfileManager.ensureInitialized(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        latestStartId = startId
        if (intent == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val label = runCatching {
            packageManager.getApplicationInfo(intent.getStringExtra("package_name")!!, 0).loadLabel(packageManager).toString()
        }.getOrElse { applicationInfo.loadLabel(packageManager).toString() }
        try {
            val notification = notifyInstallProgress(label, NOTIFICATION_ID, Downloading(0, 1))!!
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } catch (e: Exception) {
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val manager = SplitInstallManager(this@SplitInstallTaskService)
                        manager.restoreSessions()
                        manager.failInstall(intent.getIntExtra("session_id", -1))
                    }
                } finally {
                    if (activeTasks == 0) stopSelf(startId)
                }
            }
            return START_NOT_STICKY
        }
        activeTasks++
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    workMutex.withLock {
                        val manager = SplitInstallManager(this@SplitInstallTaskService)
                        manager.restoreSessions()
                        manager.install(intent.getIntExtra("session_id", -1))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Split install work failed (${e.javaClass.simpleName})")
            } finally {
                if (--activeTasks == 0) {
                    ServiceCompat.stopForeground(this@SplitInstallTaskService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf(latestStartId)
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    companion object {
        internal fun enqueueInstall(context: Context, id: Int, packageName: String) {
            val intent = Intent(context, SplitInstallTaskService::class.java)
                .putExtra("session_id", id).putExtra("package_name", packageName)
            checkNotNull(if (SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent))
        }

        internal fun enqueueDeferred(context: Context, packageName: String) = SplitInstallDeferredService.enqueue(context, packageName, false)

        internal fun enqueueRemoval(context: Context, packageName: String) = SplitInstallDeferredService.enqueue(context, packageName, true)
    }
}
