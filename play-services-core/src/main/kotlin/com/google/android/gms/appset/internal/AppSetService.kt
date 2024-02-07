/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.appset.internal

import android.util.Log
import com.google.android.gms.appset.AppSetIdRequestParams
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "AppSetService"

class AppSetService : BaseService(TAG, GmsService.APP_SET) {

    override fun handleServiceRequest(callback: IGmsCallbacks?, request: GetServiceRequest?, service: GmsService?) {
        Log.d(TAG, "handleServiceRequest is start")
        val connectionInfo = ConnectionInfo()
        connectionInfo.features = arrayOf(Feature("app_set_id", 1L))
        callback?.onPostInitCompleteWithConnectionInfo(
                ConnectionResult.SUCCESS,
                AppSetServiceImp().asBinder(),
                connectionInfo
        )
    }
}

class AppSetServiceImp : IAppSetService.Stub() {
    override fun doRequest(appSetIdRequestParams: AppSetIdRequestParams?, callback: IAppSetIdCallback?) {
        Log.d(TAG, "AppSetServiceImp doRequest is called -> ${appSetIdRequestParams?.toString()} ")
        callback?.onResult(Status.SUCCESS, null)
    }
}