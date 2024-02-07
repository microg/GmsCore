/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.pseudonymous

import android.util.Log
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.pseudonymous.internal.IPseudonymousIdCallbacks
import com.google.android.gms.pseudonymous.internal.IPseudonymousIdService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "PseudonymousIdService"

class PseudonymousIdService : BaseService(TAG, GmsService.PSEUDONYMOUS_ID) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        Log.e(TAG, " PseudonymousIdService handleServiceRequest is start")
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS,
                PseudonymousIdServiceImpl().asBinder(),
                ConnectionInfo().apply {
                    features = arrayOf(Feature("get_last_reset_time_api", 1L))
                })
    }
}

class PseudonymousIdServiceImpl : IPseudonymousIdService.Stub() {

    override fun onResponse(call: IPseudonymousIdCallbacks) {
        Log.d(TAG, "PseudonymousIdServiceImpl onResponse is called")
        call.onResponseToken(Status.SUCCESS, PseudonymousIdToken())
    }

    override fun onResponseToken(call: IPseudonymousIdCallbacks, token: PseudonymousIdToken?) {
        Log.d(TAG, "PseudonymousIdServiceImpl onResponseToken is called")
        call.onResponseToken(Status.SUCCESS, null)
    }

}
