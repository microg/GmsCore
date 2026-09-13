/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.os.Binder
import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "ConstellationService"

class ConstellationService : BaseService(TAG, GmsService.CONSTELLATION) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        Log.d(TAG, "handleServiceRequest from ${request.callingPackage}")
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            ConstellationServiceImpl(),
            ConnectionInfo()
        )
    }
}

class ConstellationServiceImpl : Binder() {
    init {
        attachInterface(null, "com.google.android.gms.constellation.internal.IConstellationService")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        Log.d(TAG, "onTransact code: $code")
        reply?.writeNoException()
        return true
    }
}
