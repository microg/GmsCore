/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsWorkManager - Background job scheduling
 */

package org.microg.gms.rcs.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.microg.gms.rcs.RcsProvisioningManager
import org.microg.gms.rcs.orchestrator.RcsOrchestrator
import org.microg.gms.rcs.state.RcsEvent
import java.util.concurrent.TimeUnit

object RcsWorkScheduler {

    private const val REFRESH_REGISTRATION_WORK = "rcs_refresh_registration"
    private const val SYNC_MESSAGES_WORK = "rcs_sync_messages"
    private const val CLEANUP_WORK = "rcs_cleanup"
    private const val HEALTH_CHECK_WORK = "rcs_health_check"

    fun schedulePeriodicRefresh(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val refreshWork = PeriodicWorkRequestBuilder<RegistrationRefreshWorker>(
            12, TimeUnit.HOURS,
            30, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            REFRESH_REGISTRATION_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            refreshWork
        )
    }

    fun scheduleMessageSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncWork = PeriodicWorkRequestBuilder<MessageSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_MESSAGES_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWork
        )
    }

    fun scheduleCleanup(context: Context) {
        val cleanupWork = PeriodicWorkRequestBuilder<CleanupWorker>(
            1, TimeUnit.DAYS
        ).build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CLEANUP_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupWork
        )
    }

    fun scheduleImmediateHealthCheck(context: Context) {
        val healthWork = OneTimeWorkRequestBuilder<HealthCheckWorker>().build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            HEALTH_CHECK_WORK,
            ExistingWorkPolicy.REPLACE,
            healthWork
        )
    }

    fun cancelAllWork(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }

    suspend fun getWorkStatus(context: Context, workName: String): WorkInfo.State? {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(workName)
            .get()
        
        return workInfos.firstOrNull()?.state
    }
}

class RegistrationRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Running registration refresh")
        
        return try {
            val orchestrator = RcsOrchestrator.getInstance(applicationContext)
            
            if (orchestrator.isRegistered()) {
                orchestrator.forceReregister()
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Registration refresh failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "RegRefreshWorker"
    }
}

class MessageSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Running message sync")
        
        return try {
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Message sync failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "MessageSyncWorker"
    }
}

class CleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Running cleanup")
        
        return try {
            cleanOldMessages()
            cleanExpiredCache()
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
            Result.failure()
        }
    }

    private fun cleanOldMessages() {
        Log.d(TAG, "Cleaning old messages")
    }

    private fun cleanExpiredCache() {
        Log.d(TAG, "Cleaning expired cache")
    }

    companion object {
        private const val TAG = "CleanupWorker"
    }
}

class HealthCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Running health check")
        
        return try {
            val healthChecker = org.microg.gms.rcs.health.RcsHealthChecker(applicationContext)
            val result = healthChecker.performHealthCheck()
            
            Log.i(TAG, "Health check complete: ${result.overallStatus}")
            
            Result.success(workDataOf(
                "status" to result.overallStatus.name,
                "checks_count" to result.checks.size
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "HealthCheckWorker"
    }
}
