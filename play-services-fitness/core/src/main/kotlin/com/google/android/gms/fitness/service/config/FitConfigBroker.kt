/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.service.config

import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.fitness.internal.IGoogleFitConfigApi
import com.google.android.gms.fitness.request.DataTypeCreateRequest
import com.google.android.gms.fitness.request.DisableFitRequest
import com.google.android.gms.fitness.request.ReadDataTypeRequest
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "FitConfigBroker"

class FitConfigBroker : BaseService(TAG, GmsService.FITNESS_CONFIG) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, FitConfigBrokerImpl(), null)
    }
}

class FitConfigBrokerImpl : IGoogleFitConfigApi.Stub() {

    override fun createCustomDataType(request: DataTypeCreateRequest?) {
        Log.d(TAG, "Not implemented createCustomDataType: $request")
    }

    override fun readDataType(request: ReadDataTypeRequest?) {
        Log.d(TAG, "Not implemented readDataType: $request")
    }

    override fun disableFit(request: DisableFitRequest?) {
        Log.d(TAG, "Method <disableFit> Called: $request")
        try {
            request?.callback?.onResult(Status.SUCCESS)
        } catch (e: Exception) {
            Log.w(TAG, "disableFit Error $e")
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}