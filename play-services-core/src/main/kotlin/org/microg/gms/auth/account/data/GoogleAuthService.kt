/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.account.data

import android.accounts.Account
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import com.google.android.gms.auth.*
import com.google.android.gms.auth.account.data.*
import com.google.android.gms.auth.firstparty.dataservice.ClearTokenRequest
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "GoogleAuthService"

val FEATURES = arrayOf(
    Feature("auth_suw", 224516000),
    Feature("account_capability_api", 1),
    Feature("account_data_service", 6),
    Feature("account_data_service_legacy", 1),
    Feature("account_data_service_token", 8),
    Feature("account_data_service_visibility", 1),
    Feature("config_sync", 1),
    Feature("device_account_api", 1),
    Feature("device_account_jwt_creation", 1),
    Feature("gaiaid_primary_email_api", 1),
    Feature("google_auth_service_accounts", 2),
    Feature("google_auth_service_token", 3),
    Feature("hub_mode_api", 1),
    Feature("user_service_account_management", 1),
    Feature("work_account_client_is_whitelisted", 1),
)

class GoogleAuthService : BaseService(TAG, GmsService.GOOGLE_AUTH){
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val binder = GoogleAuthServiceImpl().asBinder()
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, binder, ConnectionInfo().apply { features = FEATURES })
    }
}

class GoogleAuthServiceImpl : IGoogleAuthService.Stub() {
    override fun getTokenWithDetails(callback: IGetTokenWithDetailsCallback?, account: Account?, service: String?, extras: Bundle?) {
        Log.d(TAG, "Not yet implemented: getTokenWithDetails($account, $service, $extras)")
        callback?.onTokenResults(Status.INTERNAL_ERROR, Bundle.EMPTY)
    }

    override fun clearToken(callback: IStatusCallback?, request: ClearTokenRequest?) {
        Log.d(TAG, "Not yet implemented: clearToken($request)")
        callback?.onResult(Status.INTERNAL_ERROR)
    }

    override fun requestAccountsAccess(callback: IBundleCallback?, str: String?) {
        Log.d(TAG, "Not yet implemented: requestAccountsAccess($str)")
        callback?.onBundle(Status.INTERNAL_ERROR, Bundle.EMPTY)
    }

    override fun getAccountChangeEvents(callback: IGetAccountChangeEventsCallback?, request: AccountChangeEventsRequest?) {
        Log.d(TAG, "Not yet implemented: getAccountChangeEvents($request)")
        callback?.onAccountChangeEventsResponse(Status.INTERNAL_ERROR, AccountChangeEventsResponse())
    }

    override fun getAccounts(callback: IGetAccountsCallback?, request: GetAccountsRequest?) {
        Log.d(TAG, "Not yet implemented: getAccounts($request)")
        callback?.onBundle(Status.INTERNAL_ERROR, emptyList())
    }

    override fun removeAccount(callback: IBundleCallback?, account: Account?) {
        Log.d(TAG, "Not yet implemented: removeAccount($account)")
        callback?.onBundle(Status.INTERNAL_ERROR, Bundle.EMPTY)
    }

    override fun hasCapabilities(callback: IHasCapabilitiesCallback?, request: HasCapabilitiesRequest?) {
        Log.d(TAG, "Not yet implemented: hasCapabilities($request)")
        callback?.onHasCapabilities(Status.INTERNAL_ERROR, 1)
    }

    override fun getHubToken(callback: IGetHubTokenCallback?, request: GetHubTokenRequest?) {
        Log.d(TAG, "Not yet implemented: getHubToken($request)")
        callback?.onGetHubTokenResponse(Status.INTERNAL_ERROR, GetHubTokenInternalResponse())
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) {
        super.onTransact(code, data, reply, flags)
    }
}