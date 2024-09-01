package com.google.android.gms.auth.account.authenticator

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.os.Bundle
import android.util.Log

class WorkAccountAuthenticator(val context: Context) : AbstractAccountAuthenticator(context) {

    override fun editProperties(
        response: AccountAuthenticatorResponse,
        accountType: String?
    ): Bundle {
        TODO("Not yet implemented: editProperties")
    }

    override fun addAccount(
        response: AccountAuthenticatorResponse,
        accountType: String,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle
    ): Bundle {
        val name = "account${options.getInt(AccountManager.KEY_CALLER_UID)}"
        val password = if (options.containsKey(KEY_ACCOUNT_CREATION_TOKEN)) {
            options.getString(KEY_ACCOUNT_CREATION_TOKEN).also { Log.d(TAG, "read token $it") }
        } else null

        AccountManager.get(context).addAccountExplicitly(
            Account(name, WORK_ACCOUNT_TYPE), password, Bundle()
        )
        return Bundle().apply {
            putString(AccountManager.KEY_ACCOUNT_NAME, name)
            putString(AccountManager.KEY_ACCOUNT_TYPE, WORK_ACCOUNT_TYPE)
        }
    }

    override fun confirmCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        options: Bundle?
    ): Bundle {
        return Bundle().apply {
            putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true)
        }
    }

    override fun getAuthToken(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle {
        TODO("Not yet implemented: getAuthToken")
    }

    override fun getAuthTokenLabel(authTokenType: String?): String {
        TODO("Not yet implemented: getAuthTokenLabel")
    }

    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle {
        TODO("Not yet implemented: updateCredentials")
    }

    override fun hasFeatures(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        features: Array<out String>
    ): Bundle {
        Log.i(TAG, "Queried features: " + features.joinToString(", "))
        return Bundle().apply {
            putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false)
        }
    }

    companion object {
        const val TAG = "WorkAccAuthenticator"
        const val WORK_ACCOUNT_TYPE = "com.google.work"
        const val KEY_ACCOUNT_CREATION_TOKEN = "creationToken"
    }
}