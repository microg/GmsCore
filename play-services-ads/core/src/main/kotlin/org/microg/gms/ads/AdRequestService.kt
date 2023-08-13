/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ads

import android.os.Bundle
import android.os.Parcel
import android.util.Log
import com.google.android.gms.ads.internal.ExceptionParcel
import com.google.android.gms.ads.internal.NonagonRequestParcel
import com.google.android.gms.ads.internal.request.IAdRequestService
import com.google.android.gms.ads.internal.request.INonagonStreamingResponseListener
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "AdRequestService"

class AdRequestService : BaseService(TAG, GmsService.ADREQUEST) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        val binder = AdRequestServiceImpl().asBinder()
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, binder, Bundle())
    }
}

class AdRequestServiceImpl : IAdRequestService.Stub() {
    override fun getAdRequest(request: NonagonRequestParcel, listener: INonagonStreamingResponseListener) {
        Log.d(TAG, "getAdRequest")
        listener.onException(ExceptionParcel().apply {
            message = "Not supported"
            code = CommonStatusCodes.INTERNAL_ERROR
        })
    }

    override fun getSignals(request: NonagonRequestParcel, listener: INonagonStreamingResponseListener) {
        Log.d(TAG, "getSignals")
        listener.onException(ExceptionParcel().apply {
            message = "Not supported"
            code = CommonStatusCodes.INTERNAL_ERROR
        })
    }

    override fun getUrlAndCacheKey(request: NonagonRequestParcel, listener: INonagonStreamingResponseListener) {
        Log.d(TAG, "getUrlAndCacheKey")
        listener.onException(ExceptionParcel().apply {
            message = "Not supported"
            code = CommonStatusCodes.INTERNAL_ERROR
        })
    }

    override fun removeCacheUrl(key: String, listener: INonagonStreamingResponseListener) {
        Log.d(TAG, "removeCacheUrl")
        listener.onException(ExceptionParcel().apply {
            message = "Not supported"
            code = CommonStatusCodes.INTERNAL_ERROR
        })
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}