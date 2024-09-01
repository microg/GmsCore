package com.google.android.gms.auth.account.authenticator

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.workaccount.R

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
        if (!options.containsKey(KEY_ACCOUNT_CREATION_TOKEN)) {
            Log.w(TAG, "refusing to add account without creation token: was likely manually initiated by user")

            // TODO: The error message is not automatically displayed by the settings app as of now.
            // We can consider showing the error message through a popup instead.

            return Bundle().apply {
                putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION)
                putString(AccountManager.KEY_ERROR_MESSAGE,
                    context.getString(R.string.auth_work_authenticator_add_manual_error)
                )
            }
        }
        val name = "account${options.getInt(AccountManager.KEY_CALLER_UID)}"
        val password = options.getString(KEY_ACCOUNT_CREATION_TOKEN).also { Log.d(TAG, "read token $it") }

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

    /**
     * Prevent accidental deletion, unlike GMS. The account can only be removed through client apps;
     * ideally, it would only be removed by the app that requested it to be created / the DPC
     * manager, though this is not enforced. On API 21, the account can also be removed by hand
     * because `removeAccountExplicitly` is not available on API 21.
     */
    override fun getAccountRemovalAllowed(
        response: AccountAuthenticatorResponse?,
        account: Account?
    ): Bundle {
        return Bundle().apply {
            putBoolean(AccountManager.KEY_BOOLEAN_RESULT,
                Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1
            )
        }
    }

    companion object {
        const val TAG = "WorkAccAuthenticator"
        const val WORK_ACCOUNT_TYPE = "com.google.work"
        const val KEY_ACCOUNT_CREATION_TOKEN = "creationToken"
    }
}