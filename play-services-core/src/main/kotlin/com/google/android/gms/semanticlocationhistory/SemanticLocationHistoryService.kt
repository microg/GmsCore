/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory

import android.content.ContentValues
import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.data.DataHolder
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.semanticlocationhistory.internal.ISemanticLocationHistoryCallbacks
import com.google.android.gms.semanticlocationhistory.internal.ISemanticLocationHistoryService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "LocationHistoryService"

private val FEATURES = arrayOf(
    Feature("semantic_location_history", 12L),
    Feature("odlh_get_backup_summary", 2L),
    Feature("odlh_delete_backups", 1L),
    Feature("odlh_delete_history", 1L),
)

class SemanticLocationHistoryService : BaseService(TAG, GmsService.SEMANTIC_LOCATION_HISTORY) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        Log.d(TAG, "handleServiceRequest: packageName: ${request.packageName}")
        callback.onPostInitCompleteWithConnectionInfo(
            ConnectionResult.SUCCESS, SemanticLocationHistoryServiceImpl().asBinder(), ConnectionInfo().apply {
                features = FEATURES
            }
        )
    }
}

class SemanticLocationHistoryServiceImpl : ISemanticLocationHistoryService.Stub() {

    override fun getSegments(
        callback: ISemanticLocationHistoryCallbacks?,
        apiMetadata: ApiMetadata?,
        requestCredentials: RequestCredentials?,
        request: LocationHistorySegmentRequest?
    ) {
        Log.d(TAG, "Not implemented getSegments apiMetadata:$apiMetadata requestCredentials:$requestCredentials request:$request")
        val segment = LocationHistorySegment().apply {
            function = requestCredentials?.function
        }
        val contentValues = ContentValues()
        contentValues.put("data", SafeParcelableSerializer.serializeToBytes(segment))
        val holder = DataHolder.builder(arrayOf("data")).withRow(contentValues).build(CommonStatusCodes.SUCCESS)
        callback?.onSegmentsReturn(holder)
    }

    override fun onDemandBackupRestore(
        callback: IStatusCallback?,
        apiMetadata: ApiMetadata?,
        requestCredentials: RequestCredentials?
    ) {
        Log.d(TAG, "Not implemented onDemandBackupRestore apiMetadata:$apiMetadata requestCredentials:$requestCredentials")
        callback?.onResult(Status.SUCCESS)
    }

    override fun onDemandBackupRestoreV2(
        callback: IStatusCallback?,
        requestCredentials: RequestCredentials?,
        list: List<*>?,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented onDemandBackupRestoreV2 apiMetadata:$apiMetadata requestCredentials:$requestCredentials list:$list")
        callback?.onResult(Status.SUCCESS)
    }

    override fun getInferredHome(
        callback: ISemanticLocationHistoryCallbacks?,
        requestCredentials: RequestCredentials?,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented getInferredHome apiMetadata:$apiMetadata requestCredentials:$requestCredentials")
        callback?.onGetInferredHomeReturn(Status.SUCCESS, null)
    }

    override fun getInferredWork(
        callback: ISemanticLocationHistoryCallbacks?,
        requestCredentials: RequestCredentials?,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented getInferredWork apiMetadata:$apiMetadata requestCredentials:$requestCredentials")
        callback?.onGetInferredWorkReturn(Status.SUCCESS, null)
    }

    override fun editSegments(
        callback: ISemanticLocationHistoryCallbacks?,
        list: List<*>?,
        apiMetadata: ApiMetadata?,
        requestCredentials: RequestCredentials?
    ) {
        Log.d(TAG, "Not implemented editSegments apiMetadata:$apiMetadata requestCredentials:$requestCredentials list:$list")
        callback?.onEditSegmentsReturn(Status.SUCCESS)
    }

    override fun deleteHistory(
        callback: ISemanticLocationHistoryCallbacks?,
        requestCredentials: RequestCredentials,
        startTime: Long,
        endTime: Long,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented deleteHistory requestCredentials:$requestCredentials startTime:$startTime endTime:$endTime apiMetadata:$apiMetadata")
        callback?.onDeleteHistoryReturn(Status.SUCCESS)
    }

    override fun getUserLocationProfile(
        callback: IStatusCallback?,
        apiMetadata: ApiMetadata?,
        requestCredentials: RequestCredentials?
    ) {
        Log.d(TAG, "Not implemented getUserLocationProfile apiMetadata:$apiMetadata requestCredentials:$requestCredentials")
        callback?.onResult(Status.SUCCESS)
    }

    override fun getBackupSummary(
        callback: IStatusCallback?,
        apiMetadata: ApiMetadata?,
        requestCredentials: RequestCredentials?
    ) {
        Log.d(TAG, "Not implemented getBackupSummary apiMetadata:$apiMetadata requestCredentials:$requestCredentials")
        callback?.onResult(Status.SUCCESS)
    }

    override fun deleteBackups(
        callback: IStatusCallback?,
        requestCredentials: RequestCredentials?,
        list: List<*>?,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented deleteBackups apiMetadata:$apiMetadata requestCredentials:$requestCredentials list:$list")
        callback?.onResult(Status.SUCCESS)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        return warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
    }

}