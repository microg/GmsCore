/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.appset

import android.util.Log
import com.google.android.gms.appset.AppSetIdInfo
import com.google.android.gms.appset.AppSetIdRequestParams
import com.google.android.gms.appset.AppSetInfoParcel
import com.google.android.gms.appset.internal.IAppSetIdCallback
import com.google.android.gms.appset.internal.IAppSetService
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "AppSetService"
private val FEATURES = arrayOf(Feature("app_set_id", 1L))

class AppSetService : BaseService(TAG, GmsService.APP_SET) {

    override fun handleServiceRequest(callback: IGmsCallbacks?, request: GetServiceRequest?, service: GmsService?) {
        callback?.onPostInitCompleteWithConnectionInfo(
            ConnectionResult.SUCCESS,
            AppSetServiceImpl().asBinder(),
            ConnectionInfo().apply { features = FEATURES }
        )
    }
}

class AppSetServiceImpl : IAppSetService.Stub() {
    override fun getAppSetIdInfo(params: AppSetIdRequestParams?, callback: IAppSetIdCallback?) {
        Log.d(TAG, "AppSetServiceImp getAppSetIdInfo is called -> ${params?.toString()} ")
        callback?.onAppSetInfo(Status.SUCCESS, AppSetInfoParcel("00000000-0000-0000-0000-000000000000", AppSetIdInfo.SCOPE_APP))
    }
}
