/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.account.data

import android.accounts.Account
import android.util.Log
import com.google.android.gms.auth.firstparty.dataservice.DeviceManagementInfoResponse
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "AccountDataService"

class AccountDataService : BaseService(TAG, GmsService.ACCOUNT_DATA) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        Log.d(TAG, "handleServiceRequest start ")
        val connectionInfo = ConnectionInfo()
        connectionInfo.features = arrayOf(
                Feature("account_data_service", 6L),
                Feature("account_data_service_legacy", 1L),
                Feature("account_data_service_token", 7L),
                Feature("account_data_service_visibility", 1L),
                Feature("gaiaid_primary_email_api", 1L))
        callback.onPostInitCompleteWithConnectionInfo(ConnectionResult.SUCCESS,
                AccountDataServiceImpl().asBinder(),
                connectionInfo)
    }

}

class AccountDataServiceImpl : IAccountDataService.Stub() {
    override fun requestDeviceManagementInfo(callback: IDeviceManagementInfoCallback, account: Account?) {
        Log.d(TAG, "requestDeviceManagementInfo is called ")
        callback.onResult(Status.SUCCESS, DeviceManagementInfoResponse(null, false))
    }

    override fun requestAccountInfo(callback: IStatusCallback, account: Account?, isPrimary: Boolean) {
        Log.d(TAG, "requestAccountInfo is called ")
        callback.onResult(Status.SUCCESS)
    }

    override fun requestProfileInfo(callback: IStatusCallback, profile: String?) {
        Log.d(TAG, "requestProfileInfo is called ")
        callback.onResult(Status.SUCCESS)
    }
}
