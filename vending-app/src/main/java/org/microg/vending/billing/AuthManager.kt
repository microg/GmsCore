/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.util.Log
import org.microg.vending.billing.core.AuthData
import java.util.concurrent.TimeUnit

object AuthManager {
    private const val TOKEN_TYPE = "oauth2:https://www.googleapis.com/auth/googleplay https://www.googleapis.com/auth/accounts.reauth"
    fun getAuthData(context: Context, account: Account): AuthData? {
        val deviceCheckInConsistencyToken = CheckinServiceClient.getConsistencyToken(context)
        val gsfId = GServices.getString(context.contentResolver, "android_id", "0")!!.toBigInteger().toString(16)
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "gsfId: $gsfId, deviceDataVersionInfo: $deviceCheckInConsistencyToken")
        val accountManager: AccountManager = AccountManager.get(context)
        val future = accountManager.getAuthToken(account, TOKEN_TYPE, false, null, null)
        val bundle = future.getResult(15, TimeUnit.SECONDS)
        val launch = bundle.getParcelable(AccountManager.KEY_INTENT) as Intent?
        return if (launch != null) {
            Log.e(TAG, "[getAuthData]need start activity by intent: $launch")
            null
        } else {
            bundle.getString(AccountManager.KEY_AUTHTOKEN)?.let {
                AuthData(account.name, it, gsfId, deviceCheckInConsistencyToken)
            }
        }
    }
}