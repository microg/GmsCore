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
import com.android.vending.licensing.AUTH_TOKEN_SCOPE
import com.android.vending.licensing.getAuthToken
import com.android.vending.licensing.getLicenseRequestHeaders
import com.google.android.finsky.SyncResponse
import com.google.android.finsky.DeviceSyncInfo
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.microg.gms.auth.AuthConstants
import org.microg.gms.profile.ProfileManager
import org.microg.vending.billing.GServices
import org.microg.vending.billing.core.HttpClient
import java.lang.RuntimeException

private const val TAG = "AccountsChangedReceiver"

class AccountsChangedReceiver : BroadcastReceiver() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "onReceive: intent-> $intent")
        var accountName: String? = null
        if (intent?.let { accountName = it.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) } == null) {
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            runCatching {
                val account = AccountManager.get(context).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE).firstOrNull {
                    it.name == accountName
                } ?: throw RuntimeException("account is null")
                val oauthToken = account.let {
                    AccountManager.get(context).getAuthToken(it, AUTH_TOKEN_SCOPE, false).getString(AccountManager.KEY_AUTHTOKEN)
                } ?: throw RuntimeException("oauthToken is null")
                ProfileManager.ensureInitialized(context)
                val androidId = GServices.getString(context.contentResolver, "android_id", "0")?.toLong() ?: 1
                HttpClient(context).post(
                    url = "https://play-fe.googleapis.com/fdfe/sync",
                    headers = getLicenseRequestHeaders(oauthToken, androidId),
                    payload = DeviceSyncInfo.buildSyncRequest(context, androidId.toString(), account),
                    adapter = SyncResponse.ADAPTER
                )
                Log.d(TAG, "onReceive: sync success")
            }.onFailure {
                Log.d(TAG, "onReceive: sync error", it)
            }
        }
    }

}