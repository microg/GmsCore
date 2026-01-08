/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.common

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.auth.signin.SignInConfigurationService
import org.microg.gms.auth.signin.performSignOut
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.PushRegisterManager

class PackageIntentOpWorker(
    val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "PackageIntentOpWorker"
        const val PACKAGE_NAME = "packageName"
    }

    override suspend fun doWork(): Result {
        val packageName = inputData.getString(PACKAGE_NAME) ?: return Result.failure()
        Log.d(TAG, "doWork: $packageName clearing.")

        clearGcmData(packageName)
        clearAuthInfo(packageName)

        Log.d(TAG, "doWork: $packageName cleared.")
        return Result.success()
    }

    private suspend fun clearGcmData(packageName: String) = withContext(Dispatchers.IO) {
        val database = GcmDatabase(appContext)
        val app = database.getApp(packageName)
        if (app != null) {
            val registrations = database.getRegistrationsByApp(packageName)
            var deletedAll = true
            for (registration in registrations) {
                deletedAll = deletedAll and (PushRegisterManager.unregister(appContext, registration.packageName, registration.signature, null, null).deleted != null)
            }
            if (deletedAll) {
                database.removeApp(packageName)
            }
            database.close()
        } else {
            database.close()
        }
    }

    private suspend fun clearAuthInfo(packageName: String) = withContext(Dispatchers.IO) {
        val authOptions = SignInConfigurationService.getAuthOptions(appContext, packageName)
        val authAccount = SignInConfigurationService.getDefaultAccount(appContext, packageName)
        if (authOptions.isNotEmpty() && authAccount != null) {
            authOptions.forEach {
                Log.d(TAG, "$packageName:clear authAccount: ${authAccount.name} authOption:($it)")
                performSignOut(appContext, packageName, it, authAccount)
            }
        }
        SignInConfigurationService.setAuthInfo(appContext, packageName, null, null)
        AccountUtils.get(appContext).removeSelectedAccount(packageName)
    }
}