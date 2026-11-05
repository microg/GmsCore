/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.service.recording

import com.google.android.gms.fitness.service.FITNESS_FEATURES
import android.content.Context
import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.internal.IGoogleFitRecordingApi
import com.google.android.gms.fitness.request.ListSubscriptionsRequest
import com.google.android.gms.fitness.request.SubscribeRequest
import com.google.android.gms.fitness.request.UnsubscribeRequest
import com.google.android.gms.fitness.service.FitnessStepRecorder
import com.google.android.gms.fitness.result.ListSubscriptionsResult
import com.google.android.gms.fitness.service.enforceActivityRecognitionPermission
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "FitRecordingBroker"

class FitRecordingBroker : BaseService(TAG, GmsService.FIT_RECORDING) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        val clientId = "${request.account?.name.orEmpty()}\n$packageName"
        Log.d(TAG, "handleServiceRequest: packageName: $packageName")
        FitnessStepRecorder.resume(this)
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, FitRecordingBrokerImpl(applicationContext, clientId, packageName),
            ConnectionInfo().apply {
                features = FITNESS_FEATURES
            })
    }

}

class FitRecordingBrokerImpl(
    private val context: Context,
    private val clientId: String,
    private val packageName: String
) : IGoogleFitRecordingApi.Stub() {

    override fun subscribe(request: SubscribeRequest) {
        Log.d(TAG, "subscribe request: $request")
        val dataType = request.subscription.dataType ?: request.subscription.dataSource?.dataType
        val success = if (dataType?.name == DataType.TYPE_STEP_COUNT_DELTA.name) {
            context.enforceActivityRecognitionPermission(packageName)
            FitnessStepRecorder.subscribe(context, clientId, request.subscription)
        } else {
            false
        }
        return request.callback.onResult(if (success) Status.SUCCESS else Status(5008))
    }

    override fun unsubscribe(request: UnsubscribeRequest) {
        Log.d(TAG, "unsubscribe request: $request")
        FitnessStepRecorder.unsubscribe(context, clientId, request.dataType, request.dataSource)
        request.callback.onResult(Status.SUCCESS)
    }

    override fun listSubscriptions(request: ListSubscriptionsRequest) {
        Log.d(TAG, "listSubscriptions request: $request")
        return request.callback.onListSubscriptions(ListSubscriptionsResult(
            FitnessStepRecorder.subscriptions(context, clientId, request.dataType), Status.SUCCESS
        ))
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
