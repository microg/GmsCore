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
import com.google.android.gms.location.reporting.ReportingState
import com.google.android.gms.semanticlocation.SemanticLocationState
import com.google.android.gms.semanticlocationhistory.internal.ISemanticLocationHistoryCallbacks
import com.google.android.gms.semanticlocationhistory.internal.ISemanticLocationHistoryService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "LocationHistoryService"

private val FEATURES = arrayOf(
    Feature("semantic_location_history", 12),
    Feature("odlh_get_backup_summary", 2),
    Feature("odlh_delete_backups", 1),
    Feature("odlh_delete_history", 1),
    Feature("read_api_fprint_filter", 1),
    Feature("get_location_history_settings", 1),
    Feature("get_experiment_visits", 1),
    Feature("edit_csl", 1),
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
        requestCredentials: RequestCredentials?,
        request: LocationHistorySegmentRequest?,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not yet implemented: getSegments requestCredentials:$requestCredentials request:$request")
        val holder = DataHolder.empty(CommonStatusCodes.SUCCESS)
        callback?.onGetSegmentsResponse(holder, ApiMetadata.SKIP)
    }

    override fun onDemandBackup(
        callback: IStatusCallback?,
        requestCredentials: RequestCredentials?,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not yet implemented: onDemandBackup requestCredentials:$requestCredentials")
        callback?.onResult(Status.SUCCESS)
    }

    override fun onDemandRestore(
        callback: IStatusCallback?,
        requestCredentials: RequestCredentials?,
        list: List<*>?,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not yet implemented: onDemandRestore requestCredentials:$requestCredentials list:$list")
        callback?.onResult(Status.SUCCESS)
    }

    override fun getInferredHome(
        callback: ISemanticLocationHistoryCallbacks?,
        requestCredentials: RequestCredentials?,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not yet implemented: getInferredHome requestCredentials:$requestCredentials")
        callback?.onGetInferredHomeResponse(Status.SUCCESS, null, ApiMetadata.SKIP)
    }

    override fun getInferredWork(
        callback: ISemanticLocationHistoryCallbacks?,
        requestCredentials: RequestCredentials?,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not yet implemented: getInferredWork requestCredentials:$requestCredentials")
        callback?.onGetInferredWorkResponse(Status.SUCCESS, null, ApiMetadata.SKIP)
    }

    override fun editSegments(
        callback: ISemanticLocationHistoryCallbacks?,
        list: List<LocationHistorySegment>?,
        requestCredentials: RequestCredentials?,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not yet implemented: editSegments requestCredentials:$requestCredentials list:$list")
        callback?.onEditSegmentsResponse(Status.SUCCESS, ApiMetadata.SKIP)
    }

    override fun deleteHistory(
        callback: ISemanticLocationHistoryCallbacks?,
        requestCredentials: RequestCredentials,
        startTime: Long,
        endTime: Long,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not yet implemented: deleteHistory requestCredentials:$requestCredentials startTime:$startTime endTime:$endTime")
        callback?.onDeleteHistoryResponse(Status.SUCCESS, ApiMetadata.SKIP)
    }

    override fun getUserLocationProfile(
        callback: ISemanticLocationHistoryCallbacks?,
        requestCredentials: RequestCredentials?,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not yet implemented: getUserLocationProfile requestCredentials:$requestCredentials")
        callback?.onGetUserLocationProfileResponse(Status.SUCCESS, null, ApiMetadata.SKIP)
    }

    override fun getBackupSummary(
        callback: ISemanticLocationHistoryCallbacks?,
        requestCredentials: RequestCredentials?,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not yet implemented: getBackupSummary requestCredentials:$requestCredentials")
        callback?.onGetBackupSummaryResponse(Status.SUCCESS, emptyList<OdlhBackupSummary>(), ApiMetadata.SKIP)
    }

    override fun deleteBackups(
        callback: IStatusCallback?,
        requestCredentials: RequestCredentials?,
        list: List<*>?,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not yet implemented: deleteBackups requestCredentials:$requestCredentials list:$list")
        callback?.onResult(Status.SUCCESS)
    }

    override fun getLocationHistorySettings(
        callback: ISemanticLocationHistoryCallbacks?,
        requestCredentials: RequestCredentials?,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not yet implemented: getLocationHistorySettings requestCredentials:$requestCredentials")
        callback?.onLocationHistorySettings(
            Status.SUCCESS,
            LocationHistorySettings(false, 0, ReportingState(-1, -1, false, false, 1, 1, 0, false, true)),
            ApiMetadata.SKIP
        )
    }

    override fun getExperimentVisits(
        callback: ISemanticLocationHistoryCallbacks?,
        requestCredentials: RequestCredentials?,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not yet implemented: getExperimentVisits requestCredentials:$requestCredentials")
        val deviceMetadata = DeviceMetadata(listOf("0"), false, false, emptyList<DeletionRange>(), 0)
        val response = ExperimentVisitsResponse(emptyList<LocationHistorySegment>(), 0, deviceMetadata)
        callback?.onGetExperimentVisitsResponse(Status.SUCCESS, response, ApiMetadata.SKIP)
    }

    override fun editCsl(
        callback: IStatusCallback?,
        requestCredentials: RequestCredentials?,
        editInputs: SemanticLocationEditInputs?,
        state: SemanticLocationState?,
        apiMetadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not yet implemented: editCsl editInputs:$editInputs state:$state")
        callback?.onResult(Status.SUCCESS)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        return warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
    }

}