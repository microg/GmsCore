/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.service.history

import android.os.Bundle
import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.fitness.internal.IGoogleFitHistoryApi
import com.google.android.gms.fitness.request.DailyTotalRequest
import com.google.android.gms.fitness.request.DataDeleteRequest
import com.google.android.gms.fitness.request.DataInsertRequest
import com.google.android.gms.fitness.request.DataPointChangesRequest
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.DataUpdateListenerRegistrationRequest
import com.google.android.gms.fitness.request.DataUpdateListenerUnregistrationRequest
import com.google.android.gms.fitness.request.DataUpdateRequest
import com.google.android.gms.fitness.request.DebugInfoRequest
import com.google.android.gms.fitness.request.GetFileUriRequest
import com.google.android.gms.fitness.request.GetSyncInfoRequest
import com.google.android.gms.fitness.request.ReadRawRequest
import com.google.android.gms.fitness.request.ReadStatsRequest
import com.google.android.gms.fitness.request.SessionChangesRequest
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "FitHistoryBroker"

class FitHistoryBroker : BaseService(TAG, GmsService.FITNESS_HISTORY) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, FitHistoryBrokerImpl().asBinder(), Bundle())
    }
}

class FitHistoryBrokerImpl : IGoogleFitHistoryApi.Stub() {

    override fun readData(request: DataReadRequest?) {
        Log.d(TAG, "Not implemented readData: $request")
    }

    override fun insertData(request: DataInsertRequest?) {
        Log.d(TAG, "Not implemented insertData: $request")
    }

    override fun deleteData(request: DataDeleteRequest?) {
        Log.d(TAG, "Not implemented deleteData: $request")
    }

    override fun getSyncInfo(request: GetSyncInfoRequest) {
        Log.d(TAG, "Not implemented getSyncInfo: $request")
    }

    override fun readStats(request: ReadStatsRequest?) {
        Log.d(TAG, "Not implemented readStats: $request")
    }

    override fun readRaw(request: ReadRawRequest?) {
        Log.d(TAG, "Not implemented readRaw: $request")
    }

    override fun getDailyTotal(request: DailyTotalRequest?) {
        Log.d(TAG, "Not implemented getDailyTotal: $request")
    }

    override fun insertDataPrivileged(request: DataInsertRequest?) {
        Log.d(TAG, "Not implemented insertDataPrivileged: $request")
    }

    override fun updateData(request: DataUpdateRequest?) {
        Log.d(TAG, "Not implemented updateData: $request")
    }

    override fun registerDataUpdateListener(request: DataUpdateListenerRegistrationRequest?) {
        Log.d(TAG, "Not implemented registerDataUpdateListener: $request")
    }

    override fun unregisterDataUpdateListener(request: DataUpdateListenerUnregistrationRequest?) {
        Log.d(TAG, "Not implemented unregisterDataUpdateListener: $request")
    }

    override fun getFileUri(request: GetFileUriRequest?) {
        Log.d(TAG, "Not implemented getFileUri: $request")
    }

    override fun getDebugInfo(request: DebugInfoRequest?) {
        Log.d(TAG, "Not implemented getDebugInfo: $request")
    }

    override fun getDataPointChanges(request: DataPointChangesRequest?) {
        Log.d(TAG, "Not implemented getDataPointChanges: $request")
    }

    override fun getSessionChanges(request: SessionChangesRequest?) {
        Log.d(TAG, "Not implemented getSessionChanges: $request")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
