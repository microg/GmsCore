/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending

import android.accounts.Account
import android.accounts.AccountManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresApi
import org.microg.gms.auth.AuthConstants
import org.microg.vending.enterprise.MandatoryAppsJob
import org.microg.vending.enterprise.WorkAccountCreationTimes
import org.microg.vending.ui.WorkAppsActivity

class WorkAccountChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val accountManager = AccountManager.get(context)
        val workAccounts = accountManager.getAccountsByType(AuthConstants.WORK_ACCOUNT_TYPE)
        val hasWorkAccounts = workAccounts.isNotEmpty()

        updateCreationTimes(context, workAccounts)

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            updateMandatoryAppsJob(context, hasWorkAccounts)
            updateWorkStoreVisibility(context, hasWorkAccounts)
        }
    }

    private fun updateCreationTimes(context: Context, workAccounts: Array<Account>) {
        val creationTimes = WorkAccountCreationTimes(context)

        if (workAccounts.isEmpty()) {
            creationTimes.clear()
        } else {
            workAccounts.forEach { creationTimes.recordIfAbsent(it) }
        }
    }

    @RequiresApi(21)
    private fun updateWorkStoreVisibility(context: Context, hasWorkAccounts: Boolean) {
        Log.d(TAG, "setting VendingActivity state to enabled = $hasWorkAccounts")

        val componentName = ComponentName(
            context,
            WorkAppsActivity::class.java
        )
        context.packageManager.setComponentEnabledSetting(
            componentName,
            if (hasWorkAccounts) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
            0
        )
    }

    @RequiresApi(21)
    private fun updateMandatoryAppsJob(context: Context, hasWorkAccounts: Boolean) {
        if (hasWorkAccounts) {
            MandatoryAppsJob.schedule(context)
        } else {
            MandatoryAppsJob.cancel(context)
        }
    }

    companion object {
        const val TAG = "GmsVendingWorkAccRcvr"
    }
}