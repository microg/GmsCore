/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.service.history

import com.google.android.gms.fitness.service.FITNESS_FEATURES
import android.content.Context
import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.fitness.internal.IGoogleFitHistoryApi
import com.google.android.gms.common.api.Status
import com.google.android.gms.fitness.data.Bucket
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.RawBucket
import com.google.android.gms.fitness.data.RawDataSet
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
import com.google.android.gms.fitness.request.DataReadResult
import com.google.android.gms.fitness.service.FitnessStepRecorder
import com.google.android.gms.fitness.service.StepSample
import org.microg.gms.BaseService
import org.microg.gms.common.Constants
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues
import java.util.concurrent.TimeUnit

private const val TAG = "FitHistoryBroker"

class FitHistoryBroker : BaseService(TAG, GmsService.FIT_HISTORY) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, FitHistoryBrokerImpl(applicationContext).asBinder(),
            ConnectionInfo().apply {
                features = FITNESS_FEATURES
            })
    }
}

class FitHistoryBrokerImpl(private val context: Context) : IGoogleFitHistoryApi.Stub() {

    override fun readData(request: DataReadRequest?) {
        Log.d(TAG, "readData: $request")
        if (request == null) return
        val requestedTypes = request.dataTypes.orEmpty() + request.aggregatedDataTypes.orEmpty() +
                request.dataSources.orEmpty().map { it.dataType } +
                request.aggregatedDataSources.orEmpty().map { it.dataType }
        if (requestedTypes.none { it.name == DataType.TYPE_STEP_COUNT_DELTA.name }) {
            return request.callback.onPostResult(dataReadResult(Status(5008)))
        }

        FitnessStepRecorder.resume(context)
        val samples = FitnessStepRecorder.samples(context, request.startTimeMillis, request.endTimeMillis)
        val dataSources = mutableListOf<DataSource>()
        val result = dataReadResult(Status.SUCCESS).apply { uniqueDataSources = dataSources }
        if (request.bucketDurationMillis > 0) {
            if (request.bucketType != Bucket.TYPE_TIME) {
                return request.callback.onPostResult(dataReadResult(Status(5012)))
            }
            result.rawBuckets = buildBuckets(request, samples, dataSources)
        } else {
            result.rawDataSets = listOf(buildRawDataSet(samples, dataSources))
        }
        request.callback.onPostResult(result)
    }

    private fun dataReadResult(status: Status) = DataReadResult().apply {
        this.status = status
        rawDataSets = emptyList()
        rawBuckets = emptyList()
        uniqueDataSources = emptyList()
        batchCount = 1
    }

    private fun buildRawDataSet(samples: List<StepSample>, dataSources: MutableList<DataSource>) =
        RawDataSet(DataSet.builder(STEP_DATA_SOURCE).apply {
            samples.forEach { sample ->
                add(DataPoint.builder(STEP_DATA_SOURCE)
                    .setIntValues(sample.steps)
                    .setTimeInterval(sample.startTimeMillis, sample.endTimeMillis, TimeUnit.MILLISECONDS)
                    .build())
            }
        }.build(), dataSources)

    private fun buildBuckets(
        request: DataReadRequest,
        samples: List<StepSample>,
        dataSources: MutableList<DataSource>
    ): List<RawBucket> = buildList {
        var start = request.startTimeMillis
        while (start < request.endTimeMillis) {
            val end = if (start > Long.MAX_VALUE - request.bucketDurationMillis) {
                request.endTimeMillis
            } else {
                minOf(start + request.bucketDurationMillis, request.endTimeMillis)
            }
            val steps = samples.filter { it.endTimeMillis > start && it.endTimeMillis <= end }.sumOf { it.steps }
            if (steps > 0) {
                val dataSet = DataSet.builder(STEP_DATA_SOURCE)
                    .add(DataPoint.builder(STEP_DATA_SOURCE)
                        .setIntValues(steps)
                    .setTimeInterval(start, end, TimeUnit.MILLISECONDS)
                    .build())
                    .build()
                add(RawBucket(start, end, null, 4, listOf(RawDataSet(dataSet, dataSources)), Bucket.TYPE_TIME))
            }
            start = end
        }
    }

    override fun insertData(request: DataInsertRequest?) {
        Log.d(TAG, "Not implemented insertData: $request")
    }

    override fun deleteData(request: DataDeleteRequest?) {
        Log.d(TAG, "deleteData: $request")
        if (request == null) return
        val stepTypes = setOf(DataType.TYPE_STEP_COUNT_DELTA.name, DataType.TYPE_STEP_COUNT_CUMULATIVE.name)
        val deletesSteps = request.deleteAllData ||
                request.dataTypes.orEmpty().any { it.name in stepTypes } ||
                request.dataSources.orEmpty().any { it.dataType.name in stepTypes }
        if (deletesSteps) {
            FitnessStepRecorder.deleteSamples(context, request.startTimeMillis, request.endTimeMillis)
        }
        request.callback?.onResult(Status.SUCCESS)
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

    companion object {
        private val STEP_DATA_SOURCE = DataSource.Builder()
            .setAppPackageName(Constants.GMS_PACKAGE_NAME)
            .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .setType(DataSource.TYPE_DERIVED)
            .setStreamName("estimated_steps")
            .build()
    }
}
