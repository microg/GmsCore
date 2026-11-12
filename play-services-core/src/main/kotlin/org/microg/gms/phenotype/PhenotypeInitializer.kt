/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.startup.Initializer
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkManagerInitializer
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhenotypeInitializer : Initializer<Unit> {

    @SuppressLint("WrongConstant")
    override fun create(context: Context) {
        val app = context.applicationContext
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(app, PhenotypePackageReceiver(), filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(
            app, PhenotypeAccountReceiver(),
            IntentFilter(AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        PhenotypePackageWorker.enqueue(app)
        PhenotypeSyncDebouncer.schedulePeriodic(app)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(WorkManagerInitializer::class.java)
}

class PhenotypeAccountReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION) {
            PhenotypeSyncDebouncer.trigger(context, FetchReason.NEW_USER_SYNC)
        }
    }
}

class PhenotypePackageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            PhenotypePackageWorker.enqueue(context, action)
            return
        }
        if (action !in setOf(
                Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_FULLY_REMOVED, Intent.ACTION_PACKAGE_DATA_CLEARED
            )
        ) return
        if (intent.data?.scheme != "package") return
        val packageName = intent.data?.schemeSpecificPart?.takeIf { it.isNotEmpty() } ?: return
        if (action == Intent.ACTION_PACKAGE_REMOVED && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            && !intent.getBooleanExtra(Intent.EXTRA_DATA_REMOVED, true)
        ) return
        PhenotypePackageWorker.enqueue(context, action, packageName)
    }
}

class PhenotypePackageWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    companion object {
        private const val ACTION = "action"
        private const val PACKAGE = "package"

        fun enqueue(context: Context, action: String? = null, packageName: String? = null) {
            val request = OneTimeWorkRequestBuilder<PhenotypePackageWorker>()
                .setInputData(Data.Builder().putString(ACTION, action).putString(PACKAGE, packageName).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("PhenotypePackages", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val packageName = inputData.getString(PACKAGE)
            when (inputData.getString(ACTION)) {
                null, Intent.ACTION_BOOT_COMPLETED -> PhenotypeRegistrar.registerAll(applicationContext)
                Intent.ACTION_MY_PACKAGE_REPLACED -> {
                    PhenotypeRegistrar.registerAll(applicationContext)
                    PhenotypeDatabase.getInstance(applicationContext)
                        .republishSharedConfigurations(applicationContext)
                    PhenotypeSyncDebouncer.trigger(applicationContext, FetchReason.PERIODIC)
                }

                Intent.ACTION_PACKAGE_ADDED -> PhenotypeRegistrar.registerForPackage(
                    applicationContext,
                    requireNotNull(packageName), true
                )

                Intent.ACTION_PACKAGE_REMOVED, Intent.ACTION_PACKAGE_FULLY_REMOVED ->
                    PhenotypeRegistrar.unregisterForPackage(applicationContext, requireNotNull(packageName))

                Intent.ACTION_PACKAGE_DATA_CLEARED -> PhenotypeDatabase.getInstance(applicationContext)
                    .clearCommittedConfigurations(requireNotNull(packageName))

                else -> return@withContext Result.failure()
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("PhenotypePackages", "Package state reconciliation failed (${e.javaClass.simpleName})")
            Result.retry()
        }
    }
}
