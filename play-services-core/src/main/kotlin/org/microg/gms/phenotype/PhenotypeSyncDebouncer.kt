/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

internal object PhenotypeSyncDebouncer {
    private const val IMMEDIATE_WORK_NAME = "PhenotypeSyncImmediately"
    private const val PERIODIC_WORK_NAME = "PhenotypePeriodicSync"
    private const val RETRY_WORK_NAME = "PhenotypeRetryAfter"
    private const val PERIOD_SECONDS = 14400L
    private const val FLEX_SECONDS = 1800L
    private const val RETRY_MIN_SECONDS = 60L
    private const val RETRY_MAX_SECONDS = 86400L
    internal const val FETCH_REASON = "fetchReason"
    private val networkConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun trigger(context: Context, reason: FetchReason) {
        val request = OneTimeWorkRequest.Builder(PhenotypeSyncWorker::class.java)
            .setInputData(Data.Builder().putInt(FETCH_REASON, reason.value).build())
            .setInitialDelay(1500, TimeUnit.MILLISECONDS)
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequest.Builder(
            PhenotypeSyncWorker::class.java,
            PERIOD_SECONDS, TimeUnit.SECONDS, FLEX_SECONDS, TimeUnit.SECONDS
        )
            .setInputData(Data.Builder().putInt(FETCH_REASON, FetchReason.PERIODIC.value).build())
            .setConstraints(networkConstraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    fun scheduleRetryAfter(context: Context, delaySeconds: Int) {
        if (delaySeconds <= 0) return
        val request = OneTimeWorkRequest.Builder(PhenotypeSyncWorker::class.java)
            .setInputData(Data.Builder().putInt(FETCH_REASON, FetchReason.RETRY_AFTER.value).build())
            .setInitialDelay(delaySeconds.toLong().coerceIn(RETRY_MIN_SECONDS, RETRY_MAX_SECONDS), TimeUnit.SECONDS)
            .setConstraints(networkConstraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(RETRY_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}

class PhenotypeSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val reason = FetchReason.fromValue(inputData.getInt(PhenotypeSyncDebouncer.FETCH_REASON, 0))
            ?: return Result.failure()
        try {
            PhenotypeRegistrar.registerAll(applicationContext, triggerSync = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(PHENOTYPE_LOG_TAG, "Failed to refresh registrations before synchronization (${e.javaClass.simpleName})")
            return Result.retry()
        }
        val result = HeterodyneSyncer(applicationContext).syncAllPackages(fetchReason = reason)
        if (result.retryDelaySeconds > 0) {
            PhenotypeSyncDebouncer.scheduleRetryAfter(applicationContext, result.retryDelaySeconds)
            return Result.success()
        }
        return when (result.resultCode) {
            HeterodyneSyncer.RESULT_SUCCESS, HeterodyneSyncer.RESULT_NO_PACKAGES -> Result.success()
            else -> Result.retry()
        }
    }
}
