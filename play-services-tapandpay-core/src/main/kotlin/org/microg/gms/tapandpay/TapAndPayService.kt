/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.tapandpay

import android.os.Parcel
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.tapandpay.TapAndPayStatusCodes.TAP_AND_PAY_NO_ACTIVE_WALLET
import com.google.android.gms.tapandpay.internal.ITapAndPayService
import com.google.android.gms.tapandpay.internal.ITapAndPayServiceCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "GmsTapAndPay"

class TapAndPayService : BaseService(TAG, GmsService.TAP_AND_PAY) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, TapAndPayImpl(), ConnectionInfo().apply {
            features = arrayOf(
                Feature("tapandpay_token_listing", 3)
            )
        })
    }
}

class TapAndPayImpl : ITapAndPayService.Stub() {
    override fun registerDataChangedListener(callbacks: ITapAndPayServiceCallbacks) {
        Log.d(TAG, "registerDataChangedListener()")
        callbacks.onStatus9(Status.SUCCESS)
    }

    override fun getTokenStatus(tokenProvider: Int, issuerTokenId: String, callbacks: ITapAndPayServiceCallbacks) {
        Log.d(TAG, "getTokenStatus($tokenProvider, $issuerTokenId)")
        callbacks.onTokenStatus(Status(TAP_AND_PAY_NO_ACTIVE_WALLET), null)
    }

    override fun getStableHardwareId(callbacks: ITapAndPayServiceCallbacks) {
        Log.d(TAG, "getStableHardwareId()")
        callbacks.onGetStableHardwareIdResponse(Status.SUCCESS, "")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (super.onTransact(code, data, reply, flags)) return true
        Log.d(TAG, "onTransact [unknown]: $code, $data, $flags")
        return false
    }
}
