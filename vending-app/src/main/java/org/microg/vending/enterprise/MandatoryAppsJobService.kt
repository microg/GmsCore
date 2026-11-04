/*
 * SPDX-FileCopyrightText: 2026 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.enterprise

import android.accounts.Account
import android.accounts.AccountManager
import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import androidx.annotation.RequiresApi
import com.android.vending.installer.EnterpriseAppInstaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.microg.gms.auth.AuthConstants
import org.microg.vending.billing.core.AuthData
import java.util.concurrent.TimeUnit

@RequiresApi(21)
class MandatoryAppsJobService : JobService() {

    private var installJob: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val account = findWorkAccount()

        if (account == null) {
            Log.i(TAG, "No work account, nothing to install")
            return false
        }

        val constraints = MandatoryAppsJob.constraintsOf(params.extras)

        installJob = CoroutineScope(Dispatchers.IO).launch {
            val succeeded = runCatching { installMandatoryApps(account, constraints) }
                .onFailure { Log.w(TAG, "Unable to install mandatory apps", it) }
                .getOrDefault(false)

            ensureActive()
            finish(params, account, succeeded)
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        installJob?.cancel()
        return true
    }

    private fun finish(params: JobParameters, account: Account, succeeded: Boolean) {
        if (!succeeded && isEnrolling(account)) {
            jobFinished(params, /* wantsReschedule */ false)
            MandatoryAppsJob.scheduleImmediate(this)
        } else {
            jobFinished(params, /* wantsReschedule */ !succeeded)
        }
    }

    private fun isEnrolling(account: Account): Boolean {
        val creationTimeMillis = WorkAccountCreationTimes(this).creationTimeMillisOf(account) ?: return false

        return System.currentTimeMillis() - creationTimeMillis < ENROLLMENT_WINDOW_MILLIS
    }

    private fun findWorkAccount(): Account? =
        AccountManager.get(this).getAccountsByType(AuthConstants.WORK_ACCOUNT_TYPE).firstOrNull()

    private suspend fun installMandatoryApps(account: Account, constraints: InstallConstraints): Boolean {
        val enterpriseApps = awaitEnterprisePolicy(account) ?: return false
        val states = enterpriseApps.states.toMutableMap()
        val pendingApps = states.filter { (app, state) -> app.isRequired && state.needsInstall() }
        val (installableApps, deferredApps) = pendingApps.keys.partition { it.canBeInstalledUnder(constraints) }

        deferInstalls(deferredApps)

        Log.i(TAG, "Mandatory apps to install: ${installableApps.joinToString { it.packageName }}")

        val installer = EnterpriseAppInstaller(this, enterpriseApps.auth, states) { _, _, _ -> }

        installableApps.forEach { app ->
            installer.install(app, /* isUpdate */ pendingApps[app] == UpdateAvailable)

            if (states[app] == Installed) {
                applyManagedConfiguration(enterpriseApps.auth, app)
            }
        }

        return installableApps.all { states[it] == Installed }
    }

    private suspend fun awaitEnterprisePolicy(account: Account): EnterpriseApps? {
        repeat(POLICY_POLL_ATTEMPTS) { attempt ->
            if (attempt > 0) {
                delay(POLICY_POLL_INTERVAL_MILLIS)
            }

            val enterpriseApps = EnterpriseAppsRepository(this).fetchApps(account) ?: return null
            if (enterpriseApps.states.isNotEmpty()) {
                return enterpriseApps
            }

            Log.i(TAG, "Enterprise policy not available yet")
        }

        return null
    }

    private suspend fun applyManagedConfiguration(auth: AuthData, app: EnterpriseApp) {
        val configuration = runCatching { ManagedConfigurationsRepository().fetchConfiguration(auth, app) }
            .onFailure { Log.w(TAG, "Unable to fetch the managed configuration of ${app.packageName}", it) }
            .getOrNull()

        if (configuration != null) {
            ManagedConfigurationsProxyClient(this).applyRestrictions(app.packageName, configuration.toBundle())
        }
    }

    private fun deferInstalls(apps: List<EnterpriseApp>) {
        apps.map { it.installConstraints.first() }.distinct().forEach { constraints ->
            Log.i(TAG, "Deferring mandatory apps install until $constraints")
            MandatoryAppsJob.scheduleUnder(this, constraints)
        }
    }

    private fun AppState.needsInstall() = this == NotInstalled || this == UpdateAvailable

    companion object {
        private const val TAG = "GmsVendingMandatoryApps"
        private const val POLICY_POLL_ATTEMPTS = 8
        private val POLICY_POLL_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(15)
        private val ENROLLMENT_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(15)
    }
}
