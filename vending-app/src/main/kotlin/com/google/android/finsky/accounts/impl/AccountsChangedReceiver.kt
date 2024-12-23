/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.accounts.impl

import android.accounts.AccountManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.vending.VendingPreferences
import com.android.vending.AUTH_TOKEN_SCOPE
import com.android.vending.licensing.getAuthToken
import com.google.android.finsky.syncDeviceInfo
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.microg.gms.auth.AuthConstants
import org.microg.gms.profile.ProfileManager
import org.microg.vending.billing.GServices

private const val TAG = "AccountsChangedReceiver"

class AccountsChangedReceiver : BroadcastReceiver() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "onReceive: intent-> $intent")
        val deviceSyncEnabled = VendingPreferences.isDeviceSyncEnabled(context)
        if (!deviceSyncEnabled) {
            Log.d(TAG, "onReceive: deviceSyncEnabled is false")
            return
        }
        var accountName: String? = null
        if (intent?.let { accountName = it.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) } == null) {
            Log.d(TAG, "onReceive: accountName is empty")
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            val account = AccountManager.get(context).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE).firstOrNull {
                it.name == accountName
            } ?: throw RuntimeException("account is null")
            ProfileManager.ensureInitialized(context)
            val androidId = GServices.getString(context.contentResolver, "android_id", "1")?.toLong() ?: 1
            val authToken = account.let {
                AccountManager.get(context).getAuthToken(it, AUTH_TOKEN_SCOPE, false).getString(AccountManager.KEY_AUTHTOKEN)
            } ?: throw RuntimeException("oauthToken is null")
            syncDeviceInfo(context, account, authToken, androidId)
        }
    }

}