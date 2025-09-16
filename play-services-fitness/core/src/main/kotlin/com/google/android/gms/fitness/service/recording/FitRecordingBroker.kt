/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.service.recording

import com.google.android.gms.fitness.service.FITNESS_FEATURES
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.fitness.data.Subscription
import com.google.android.gms.fitness.internal.IGoogleFitRecordingApi
import com.google.android.gms.fitness.request.ListSubscriptionsRequest
import com.google.android.gms.fitness.request.SubscribeRequest
import com.google.android.gms.fitness.request.UnsubscribeRequest
import com.google.android.gms.fitness.result.ListSubscriptionsResult
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "FitRecordingBroker"

class FitRecordingBroker : BaseService(TAG, GmsService.FITNESS_RECORDING) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        Log.d(TAG, "handleServiceRequest: account: ${request.account.name} packageName: ${request.packageName}")
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, FitRecordingBrokerImpl(),
            ConnectionInfo().apply {
                features = FITNESS_FEATURES
            })
    }

}

class FitRecordingBrokerImpl() : IGoogleFitRecordingApi.Stub() {

    override fun subscribe(request: SubscribeRequest) {
        Log.d(TAG, "Not yet implemented subscribe request: $request")
        return request.callback.onResult(Status.SUCCESS)
    }

    override fun unsubscribe(request: UnsubscribeRequest) {
        Log.d(TAG, "Not yet implemented unsubscribe request: $request")
        request.callback.onResult(Status.SUCCESS)
    }

    override fun listSubscriptions(request: ListSubscriptionsRequest) {
        Log.d(TAG, "Not yet implemented listSubscriptions request: $request")
        return request.callback.onListSubscriptions(ListSubscriptionsResult(emptyList<Subscription>(), Status(5008)))
    }

}