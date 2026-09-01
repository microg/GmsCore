/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.aang

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.aang.GetAccountsRequest
import com.google.android.gms.auth.aang.GetAccountsResponse
import com.google.android.gms.auth.aang.GoogleAccount
import com.google.android.gms.auth.aang.HasCapabilitiesRequest
import com.google.android.gms.auth.aang.internal.IGoogleAuthAangCallbacks
import com.google.android.gms.auth.aang.internal.IGoogleAuthAangService
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.capabilities.HasCapabilitiesHandler
import org.microg.gms.auth.capabilities.HasCapabilitiesResult
import org.microg.gms.common.GmsService
import org.microg.gms.common.GooglePackagePermission
import org.microg.gms.common.PackageUtils

private const val TAG = "GoogleAuthAangService"

private val FEATURES = arrayOf(
    Feature("google_auth_api", 1),
    Feature("sync_account_state_api", 1),
    Feature("embedded_reauth", 1),
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
        val requestedNames = request?.accountNames?.toSet().orEmpty()
        val accounts = AccountManager.get(context).getAccountsByType(accountType)
            .asSequence()
            .filter { requestedNames.isEmpty() || it.name in requestedNames }
            .map { account ->
                GoogleAccount().apply {
                    obfuscatedGaiaId = ""
                    type = account.type
                    name = account.name
                }
            }
            .toList()
        Log.d(TAG, "getAccounts($accountType) = ${accounts.size}")
        callback?.onGetAccounts(Status.SUCCESS, GetAccountsResponse().apply {
            this.accounts = accounts
            restrictedAccounts = emptyList()
        })
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
        Log.d(TAG, "hasCapabilities(${account.name}, ${legacyRequest.capabilities.contentToString()}) = $result")
        callback?.onHasCapabilities(Status.SUCCESS, result)
    }
}
