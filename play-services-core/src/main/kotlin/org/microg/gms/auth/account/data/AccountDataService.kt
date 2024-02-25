/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.account.data

import android.accounts.Account
import android.util.Log
import com.google.android.gms.auth.account.data.IAccountDataService
import com.google.android.gms.auth.account.data.IDeviceManagementInfoCallback
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
        callback.onPostInitCompleteWithConnectionInfo(
            ConnectionResult.SUCCESS,
            AccountDataServiceImpl().asBinder(),
            ConnectionInfo().apply { features = FEATURES }
        )
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
