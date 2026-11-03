/*
 * SPDX-FileCopyrightText: 2026 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.enterprise

import android.accounts.AccountManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.microg.gms.auth.AuthConstants

class ManagingAppChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (android.os.Build.VERSION.SDK_INT < 21) {
            return
        }

        if (!hasWorkAccount(context)) {
            Log.i(TAG, "No work account on managing app change, nothing to sync")
            return
        }

        Log.i(TAG, "Syncing mandatory apps due to managing app change")
        MandatoryAppsJob.schedule(context)
    }

    private fun hasWorkAccount(context: Context): Boolean =
        AccountManager.get(context).getAccountsByType(AuthConstants.WORK_ACCOUNT_TYPE).isNotEmpty()

    companion object {
        private const val TAG = "GmsVendingManagingApp"
    }
}
