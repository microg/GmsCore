/*
 * SPDX-FileCopyrightText: 2024 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.account.authenticator

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import org.microg.gms.auth.workaccount.R
import org.microg.gms.auth.AuthConstants
import org.microg.gms.common.PackageUtils
import org.microg.gms.auth.AuthRequest
import org.microg.gms.auth.AuthResponse
import org.microg.gms.auth.workaccount.WorkProfileSettings
import java.io.IOException
import kotlin.jvm.Throws

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
    ): Bundle? {

        if (!WorkProfileSettings(context).allowCreateWorkAccount) {
            return Bundle().apply {
                putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION)
                putString(AccountManager.KEY_ERROR_MESSAGE, context.getString(R.string.auth_work_authenticator_disabled_error)
                )
            }
        } else if (
            !options.containsKey(KEY_ACCOUNT_CREATION_TOKEN)
            || options.getString(KEY_ACCOUNT_CREATION_TOKEN) == null
            || options.getInt(AccountManager.KEY_CALLER_UID) != android.os.Process.myUid()) {
            Log.e(TAG,
                "refusing to add account without creation token or from external app: " +
                        "could have been manually initiated by user (not supported) " +
                        "or by unauthorized app (not allowed)"
            )

            // TODO: The error message is not automatically displayed by the settings app as of now.
            // We can consider showing the error message through a popup instead.

            return Bundle().apply {
                putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION)
                putString(AccountManager.KEY_ERROR_MESSAGE, context.getString(R.string.auth_work_authenticator_add_manual_error)
                )
            }
        }

        val oauthToken: String = options.getString(KEY_ACCOUNT_CREATION_TOKEN)!!

        try {
            tryAddAccount(oauthToken, response)
        } catch (exception: Exception) {
            response.onResult(Bundle().apply {
                putInt(
                    AccountManager.KEY_ERROR_CODE,
                    AccountManager.ERROR_CODE_NETWORK_ERROR
                )
                putString(AccountManager.KEY_ERROR_MESSAGE, exception.message)
            })
        }

        /* Note: as is not documented, `null` must only be returned after `response.onResult` was
         * already called, hence forcing the requests to be synchronous. They are still async to
         * the caller's main thread because AccountManager forces potentially blocking operations,
         * like waiting for a response upon `addAccount`, not to be on the main thread.
         */
        return null
    }

    @Throws(Exception::class)
    private fun tryAddAccount(
        oauthToken: String,
        response: AccountAuthenticatorResponse
    ) {
        val authResponse = AuthRequest().fromContext(context)
            .appIsGms()
            .callerIsGms()
            .service("ac2dm")
            .token(oauthToken).isAccessToken()
            .addAccount()
            .getAccountId()
            .droidguardResults(null)
            .response

        val accountManager = AccountManager.get(context)
        if (accountManager.addAccountExplicitly(
                Account(authResponse.email, AuthConstants.WORK_ACCOUNT_TYPE),
                authResponse.token, Bundle().apply {
                    // Work accounts have no SID / LSID ("BAD_COOKIE") and no first/last name.
                    if (authResponse.accountId.isNotBlank()) {
                        putString(KEY_GOOGLE_USER_ID, authResponse.accountId)
                    }
                    putString(AuthConstants.KEY_ACCOUNT_CAPABILITIES, authResponse.capabilities)
                    putString(AuthConstants.KEY_ACCOUNT_SERVICES, authResponse.services)
                    if (authResponse.services != "android") {
                        Log.i(
                            TAG,
                            "unexpected 'services' value ${authResponse.services} (usually 'android')"
                        )
                    }
                }
            )
        ) {

            // Notify vending package
            context.sendBroadcast(
                Intent(WORK_ACCOUNT_CHANGED_BOARDCAST).setPackage("com.android.vending")
            )

            // Report successful creation to caller
            response.onResult(Bundle().apply {
                putString(AccountManager.KEY_ACCOUNT_NAME, authResponse.email)
                putString(AccountManager.KEY_ACCOUNT_TYPE, AuthConstants.WORK_ACCOUNT_TYPE)
            })
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
        account: Account,
        authTokenType: String?,
        options: Bundle?
    ): Bundle {
        try {
            val authResponse: AuthResponse =
                AuthRequest().fromContext(context)
                    .source("android")
                    .app(
                        context.packageName,
                        PackageUtils.firstSignatureDigest(context, context.packageName)
                    )
                    .email(account.name)
                    .token(AccountManager.get(context).getPassword(account))
                    .service(authTokenType)
                    .delegation(0, null)
//                .oauth2Foreground(oauth2Foreground)
//                .oauth2Prompt(oauth2Prompt)
//                .oauth2IncludeProfile(includeProfile)
//                .oauth2IncludeEmail(includeEmail)
//                .itCaveatTypes(itCaveatTypes)
//                .tokenRequestOptions(tokenRequestOptions)
                    .systemPartition(true)
                    .hasPermission(true)
//                .putDynamicFiledMap(dynamicFields)
                    .appIsGms()
                    .callerIsApp()
                    .response

            return Bundle().apply {
                putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
                putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
                putString(AccountManager.KEY_AUTHTOKEN, authResponse.auth)
            }
        } catch (e: IOException) {
            return Bundle().apply {
                putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_NETWORK_ERROR)
                putString(AccountManager.KEY_ERROR_MESSAGE, e.message)
            }
        }
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

        const val WORK_ACCOUNT_CHANGED_BOARDCAST = "org.microg.vending.WORK_ACCOUNT_CHANGED"

        const val KEY_ACCOUNT_CREATION_TOKEN = "creationToken"
        private const val KEY_GOOGLE_USER_ID = AuthConstants.GOOGLE_USER_ID
    }
}