/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.splitinstall

import android.Manifest
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.google.android.finsky.splitinstallservice.SplitInstallManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.gms.profile.ProfileManager

private const val TAG = "SplitInstallCompletion"
private const val JOB_ID = 0x53504c56

@RequiresApi(21)
class SplitInstallCompletionService : JobService(), LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private var activeParameters: JobParameters? = null
    private var work: Job? = null
    private var rescheduleRequested = false

    override val lifecycle: Lifecycle get() = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onStartJob(params: JobParameters): Boolean = synchronized(Companion) {
        work?.cancel()
        activeParameters = params
        activeService = this
        jobFinishing = false
        rescheduleRequested = false
        work = lifecycleScope.launch {
            val retry = try {
                withContext(Dispatchers.IO) {
                    ProfileManager.ensureInitialized(this@SplitInstallCompletionService)
                    val manager = SplitInstallManager(this@SplitInstallCompletionService)
                    manager.restoreSessions()
                    manager.completeDownloadedSessions()
                }
            } catch (e: CancellationException) {
                // Removing a session cancels the IO work without stopping the scheduled job.
                currentCoroutineContext().ensureActive()
                true
            } catch (e: Exception) {
                Log.w(TAG, "Split completion work failed (${e.javaClass.simpleName})")
                true
            }
            synchronized(Companion) {
                if (activeParameters === params) {
                    activeParameters = null
                    activeService = null
                    work = null
                    jobFinishing = true
                    jobFinished(params, retry || rescheduleRequested)
                }
            }
        }
        true
    }

    override fun onStopJob(params: JobParameters): Boolean = synchronized(Companion) {
        if (activeParameters?.jobId == params.jobId) {
            activeParameters = null
            if (activeService === this) activeService = null
            work?.cancel()
            work = null
        }
        canInstallSilently(this)
    }

    override fun onDestroy() {
        synchronized(Companion) {
            activeParameters = null
            if (activeService === this) activeService = null
            work?.cancel()
            work = null
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    companion object {
        private var activeService: SplitInstallCompletionService? = null
        private var jobFinishing = false

        internal fun canInstallSilently(context: Context): Boolean =
            context.packageManager.checkPermission(Manifest.permission.INSTALL_PACKAGES, context.packageName) == PackageManager.PERMISSION_GRANTED

        @Synchronized
        internal fun schedule(context: Context) {
            if (SDK_INT < 21) return
            try {
                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                if (!canInstallSilently(context)) {
                    scheduler.cancel(JOB_ID)
                    return
                }
                activeService?.let {
                    it.rescheduleRequested = true
                    return
                }
                val pending = if (SDK_INT >= 24) scheduler.getPendingJob(JOB_ID)
                else scheduler.allPendingJobs.firstOrNull { it.id == JOB_ID }
                // A new request must survive the asynchronous jobFinished acknowledgement.
                if (pending != null && !jobFinishing) return
                val job = JobInfo.Builder(JOB_ID, ComponentName(context, SplitInstallCompletionService::class.java))
                    .setMinimumLatency(60_000L)
                    .setRequiresDeviceIdle(true)
                    .setPersisted(true)
                    .build()
                if (scheduler.schedule(job) == JobScheduler.RESULT_SUCCESS) {
                    jobFinishing = false
                    Log.d(TAG, "Scheduled split completion job $JOB_ID")
                } else {
                    Log.w(TAG, "Unable to schedule split completion job $JOB_ID")
                }
            } catch (e: RuntimeException) {
                Log.w(TAG, "Unable to schedule split completion job (${e.javaClass.simpleName})")
            }
        }
    }
}
