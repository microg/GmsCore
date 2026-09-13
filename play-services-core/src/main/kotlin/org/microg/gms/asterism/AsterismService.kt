/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.asterism

import android.os.Binder
import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "AsterismService"

class AsterismService : BaseService(TAG, GmsService.ASTERISM) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        Log.d(TAG, "handleServiceRequest from ${request.callingPackage}")
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            AsterismServiceImpl(),
            ConnectionInfo()
        )
    }
}

class AsterismServiceImpl : Binder() {
    init {
        attachInterface(null, "com.google.android.gms.asterism.internal.IAsterismService")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        Log.d(TAG, "onTransact code: $code")
        reply?.writeNoException()
        return true
    }
}
