/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.service.history

import android.os.Bundle
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.fitness.internal.IGoogleFitHistoryApi
import com.google.android.gms.fitness.request.DataDeleteRequest
import com.google.android.gms.fitness.request.DataInsertRequest
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.GetSyncInfoRequest
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

const val TAG = "FitHistoryBroker"

class FitHistoryBroker : BaseService(TAG, GmsService.FITNESS_HISTORY) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, FitHistoryBrokerImpl().asBinder(), Bundle())
    }
}

class FitHistoryBrokerImpl : IGoogleFitHistoryApi.Stub() {
    override fun getDeleteData(dataDeleteRequest: DataDeleteRequest) {
        Log.d(TAG, "Not implemented getDeleteData: $dataDeleteRequest")
    }

    override fun getSyncInfo(getSyncInfoRequest: GetSyncInfoRequest) {
        Log.d(TAG, "Not implemented getSyncInfo: $getSyncInfoRequest")
    }

    override fun getInsertData(dataInsertRequest: DataInsertRequest) {
        Log.d(TAG, "Not implemented getInsertData: $dataInsertRequest")
    }

    override fun getReadData(dataReadRequest: DataReadRequest) {
        Log.d(TAG, "Not implemented getReadData: $dataReadRequest")
    }
}
