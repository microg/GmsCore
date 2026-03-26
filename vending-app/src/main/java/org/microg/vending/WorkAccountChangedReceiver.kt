/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending

import android.accounts.AccountManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import org.microg.gms.auth.AuthConstants
import org.microg.vending.ui.WorkAppsActivity

class WorkAccountChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val accountManager = AccountManager.get(context)
        val hasWorkAccounts = accountManager.getAccountsByType(AuthConstants.WORK_ACCOUNT_TYPE).isNotEmpty()


        if (android.os.Build.VERSION.SDK_INT >= 21) {
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
    }

    companion object {
        const val TAG = "GmsVendingWorkAccRcvr"
    }
}