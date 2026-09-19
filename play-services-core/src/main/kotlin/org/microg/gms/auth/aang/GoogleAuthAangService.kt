/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.aang

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.PendingIntentCompat
import com.google.android.gms.auth.TokenData
import com.google.android.gms.auth.aang.AccountWithAppRestrictionState
import com.google.android.gms.auth.aang.AppRestriction
import com.google.android.gms.auth.aang.AppRestrictionState
import com.google.android.gms.auth.aang.FetchAppRestrictionRequest
import com.google.android.gms.auth.aang.GetAccountsRequest
import com.google.android.gms.auth.aang.GetAccountsResponse
import com.google.android.gms.auth.aang.GetTokenRequest
import com.google.android.gms.auth.aang.GetTokenResponse
import com.google.android.gms.auth.aang.GoogleAccount
import com.google.android.gms.auth.aang.HasCapabilitiesRequest
import com.google.android.gms.auth.aang.Oauth2TokenMetadata
import com.google.android.gms.auth.aang.internal.IGoogleAuthAangCallbacks
import com.google.android.gms.auth.aang.internal.IGoogleAuthAangService
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.AuthManagerServiceImpl
import org.microg.gms.auth.capabilities.HasCapabilitiesHandler
import org.microg.gms.auth.capabilities.HasCapabilitiesResult
import org.microg.gms.common.GmsService
import org.microg.gms.common.GooglePackagePermission
import org.microg.gms.common.PackageUtils

private const val TAG = "GoogleAuthAangService"
private const val TOKEN_DETAILS = "tokenDetails"
private const val TOKEN_DATA = "TokenData"

private val FEATURES = arrayOf(
    Feature("google_auth_api", 1),
)

class GoogleAuthAangService : BaseService(TAG, GmsService.GOOGLE_AUTH_AANG) {
    override fun handleServiceRequest(
        callback: IGmsCallbacks,
        request: GetServiceRequest,
        service: GmsService
    ) {
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            GoogleAuthAangServiceImpl(this).asBinder(),
            ConnectionInfo().apply { features = FEATURES }
        )
    }
}

internal class GoogleAuthAangServiceImpl(private val context: Context) : IGoogleAuthAangService.Stub() {
    override fun getAccounts(
        callback: IGoogleAuthAangCallbacks?,
        request: GetAccountsRequest?
    ) {
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.ACCOUNT)
        val accountType = request?.accountType?.takeIf { it.isNotBlank() } ?: AuthConstants.DEFAULT_ACCOUNT_TYPE
        val accounts = AccountManager.get(context).getAccountsByType(accountType).map { account ->
            GoogleAccount().apply {
                obfuscatedGaiaId = ""
                type = account.type
                name = account.name
            }
        }
        val restrictedAccounts = if (request?.includeRestrictedAccounts == true) {
            accounts.map { account ->
                AccountWithAppRestrictionState().apply {
                    this.account = account
                    restrictionState = AppRestrictionState().apply {
                        restricted = false
                        accountHidden = false
                    }
                }
            }
        } else {
            emptyList()
        }
        Log.d(TAG, "getAccounts(includeRestricted=${request?.includeRestrictedAccounts == true}) = ${accounts.size}")
        callback?.onGetAccounts(Status.SUCCESS, GetAccountsResponse().apply {
            this.accounts = accounts
            this.restrictedAccounts = restrictedAccounts
        })
    }

    override fun getToken(
        callback: IGoogleAuthAangCallbacks?,
        request: GetTokenRequest?
    ) {
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.ACCOUNT)
        val safeRequest = request
            ?: return callback.sendTokenError(CommonStatusCodes.DEVELOPER_ERROR, "Missing request")
        val account = safeRequest.account
        if (account?.name.isNullOrBlank() || account?.type.isNullOrBlank()) {
            return callback.sendTokenError(CommonStatusCodes.INVALID_ACCOUNT, "Missing account")
        }
        val scope = safeRequest.toLegacyScope()
            ?: return callback.sendTokenError(CommonStatusCodes.DEVELOPER_ERROR, "Unsupported token request")
        val packageName = try {
            PackageUtils.getAndCheckCallingPackage(context, safeRequest.packageName)
        } catch (e: SecurityException) {
            Log.w(TAG, "getToken rejected caller package")
            return callback.sendTokenError(CommonStatusCodes.DEVELOPER_ERROR, e.message)
        } ?: return callback.sendTokenError(CommonStatusCodes.DEVELOPER_ERROR, "Missing caller package")

        val extras = Bundle().apply {
            putString(AuthManagerServiceImpl.KEY_ANDROID_PACKAGE_NAME, packageName)
            putBoolean(AuthManagerServiceImpl.KEY_HANDLE_NOTIFICATION, safeRequest.handleNotification)
            putBoolean(AuthManagerServiceImpl.KEY_SUPPRESS_PROGRESS_SCREEN, safeRequest.suppressProgressScreen)
            if (safeRequest.delegationType != 0) {
                putInt(AuthManagerServiceImpl.KEY_DELEGATION_TYPE, safeRequest.delegationType)
                safeRequest.delegateeUserId?.let { putString(AuthManagerServiceImpl.KEY_DELEGATEE_USER_ID, it) }
            }
        }
        val result = AuthManagerServiceImpl(context).getTokenWithAccount(
            Account(account.name, account.type),
            scope,
            extras
        )
        val token = result.getString(AccountManager.KEY_AUTHTOKEN)
        if (!token.isNullOrBlank()) {
            @Suppress("DEPRECATION")
            val tokenData = result.getBundle(TOKEN_DETAILS)?.getParcelable(TOKEN_DATA) as? TokenData
            Log.d(TAG, "getToken = success")
            callback?.onGetToken(Status.SUCCESS, GetTokenResponse().apply {
                this.token = token
                oauth2TokenMetadata = tokenData?.let {
                    Oauth2TokenMetadata().apply {
                        expiry = it.expiry
                        scopes = it.scopes
                    }
                }
            })
            return
        }

        val error = result.getString(AuthManagerServiceImpl.KEY_ERROR)
        @Suppress("DEPRECATION")
        val recoveryIntent = result.getParcelable(AuthManagerServiceImpl.KEY_USER_RECOVERY_INTENT) as? Intent
        Log.w(TAG, "getToken failed")
        when (error) {
            "NeedPermission" -> {
                val resolution = recoveryIntent?.let { PendingIntentCompat.getActivity(context, 0, it, 0, false) }
                callback?.onGetToken(Status(CommonStatusCodes.SIGN_IN_REQUIRED, error, resolution), null)
            }
            "NetworkError" -> callback.sendTokenError(CommonStatusCodes.INTERNAL_ERROR, error)
            else -> callback.sendTokenError(CommonStatusCodes.ERROR, error ?: "Token unavailable")
        }
    }

    override fun clearToken(callback: IStatusCallback?, token: String?) {
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.ACCOUNT)
        token?.takeIf { it.isNotBlank() }?.let {
            AccountManager.get(context).invalidateAuthToken(AuthConstants.DEFAULT_ACCOUNT_TYPE, it)
        }
        callback?.onResult(Status.SUCCESS)
    }

    override fun hasCapabilities(
        callback: IGoogleAuthAangCallbacks?,
        request: HasCapabilitiesRequest?
    ) {
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.ACCOUNT)
        val safeRequest = request ?: run {
            callback?.onHasCapabilities(Status.INTERNAL_ERROR, HasCapabilitiesResult.NOT_IN_CACHE)
            return
        }
        val account = safeRequest.account
        if (account?.name.isNullOrBlank() || account?.type.isNullOrBlank()) {
            callback?.onHasCapabilities(Status.INTERNAL_ERROR, HasCapabilitiesResult.NOT_IN_CACHE)
            return
        }
        val legacyRequest = com.google.android.gms.auth.HasCapabilitiesRequest().apply {
            this.account = Account(account.name, account.type)
            capabilities = safeRequest.capabilities?.toTypedArray() ?: emptyArray()
        }
        val result = HasCapabilitiesHandler(context).handle(legacyRequest)
        Log.d(TAG, "hasCapabilities = $result")
        callback?.onHasCapabilities(Status.SUCCESS, result)
    }

    override fun fetchAppRestriction(
        callback: IGoogleAuthAangCallbacks?,
        request: FetchAppRestrictionRequest?
    ) {
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.ACCOUNT)
        val account = request?.account
        if (account?.name.isNullOrBlank() || account?.type.isNullOrBlank()) {
            callback?.onFetchAppRestriction(Status(CommonStatusCodes.INVALID_ACCOUNT), null)
            return
        }
        Log.d(TAG, "fetchAppRestriction = unrestricted")
        callback?.onFetchAppRestriction(Status.SUCCESS, AppRestriction().apply {
            restrictionState = AppRestrictionState().apply {
                restricted = false
                accountHidden = false
            }
            restrictionInfo = null
        })
    }
}

private fun GetTokenRequest.toLegacyScope(): String? {
    val tokenTypes = buildList {
        oauth2Scopes?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?.let { add("oauth2:${it.joinToString(" ")}") }
        webLoginUrls?.filter { it.isNotBlank() }
            ?.singleOrNull()
            ?.let { add("weblogin:$it") }
        clientLoginScopes?.filter { it.isNotBlank() }
            ?.singleOrNull()
            ?.let { add(it) }
        oauth2TokenIdScopes?.filter { it.isNotBlank() }
            ?.singleOrNull()
            ?.let { add("audience:server:client_id:$it") }
    }
    return tokenTypes.singleOrNull()
}

private fun IGoogleAuthAangCallbacks?.sendTokenError(statusCode: Int, message: String?) {
    this?.onGetToken(Status(statusCode, message), null)
}
