/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.splitinstall

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.job.JobWorkItem
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.JobIntentService
import com.android.vending.VendingPreferences
import com.google.android.finsky.splitinstallservice.SplitInstallManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.microg.gms.profile.ProfileManager

private const val TAG = "SplitInstallDeferred"
private const val JOB_ID = 0x53504c54

@Suppress("DEPRECATION")
@RequiresApi(21)
class SplitInstallDeferredService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        if (!VendingPreferences.isSplitInstallEnabled(this)) return
        val packageName = intent.getStringExtra("package_name") ?: return
        try {
            val recovery = intent.hasExtra("session_id")
            val version = intent.getLongExtra("version", 0)
            val clientInstallTime = intent.getLongExtra("client_install_time", 0)
            val identified = intent.hasExtra("version") && intent.hasExtra("client_install_time")
            ProfileManager.ensureInitialized(this)
            runBlocking {
                val manager = SplitInstallManager(this@SplitInstallDeferredService)
                manager.restoreSessions(packageName.takeIf { !recovery && identified }, intent.getBooleanExtra("uninstall", false), version, clientInstallTime)
                if (recovery) manager.recoverSession(intent.getIntExtra("session_id", -1))
                else if (!identified) return@runBlocking
                else if (intent.getBooleanExtra("uninstall", false)) manager.uninstall(packageName, version, clientInstallTime)
                else manager.installDeferred(packageName, version, clientInstallTime)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Deferred split request failed (${e.javaClass.simpleName})")
        }
    }

    companion object {
        internal fun enqueueRecovery(context: Context, sessionId: Int, packageName: String) {
            enqueue(context, Intent().putExtra("package_name", packageName).putExtra("session_id", sessionId))
        }

        internal fun enqueue(context: Context, packageName: String, uninstall: Boolean, version: Long, clientInstallTime: Long) {
            enqueue(context, Intent().putExtra("package_name", packageName).putExtra("uninstall", uninstall)
                .putExtra("version", version).putExtra("client_install_time", clientInstallTime))
        }

        private fun enqueue(context: Context, intent: Intent) {
            if (SDK_INT >= 26) {
                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                val job = JobInfo.Builder(JOB_ID, ComponentName(context, SplitInstallDeferredService::class.java))
                    .setOverrideDeadline(0).build()
                check(scheduler.enqueue(job, JobWorkItem(intent)) == JobScheduler.RESULT_SUCCESS)
            } else {
                enqueueWork(context, SplitInstallDeferredService::class.java, JOB_ID, intent)
            }
        }
    }
}
