/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory

import android.content.Context
import android.os.Parcel
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
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
import com.google.android.gms.location.reporting.ReportingState
import com.google.android.gms.semanticlocation.SemanticLocationState
import com.google.android.gms.semanticlocationhistory.db.OdlhStorageManager
import com.google.android.gms.semanticlocationhistory.db.backup.BackupRestoreHandler
import com.google.android.gms.semanticlocationhistory.db.backup.OdlhBackupProcessor
import com.google.android.gms.semanticlocationhistory.db.backup.OdlhBackupService
import com.google.android.gms.semanticlocationhistory.internal.ISemanticLocationHistoryCallbacks
import com.google.android.gms.semanticlocationhistory.internal.ISemanticLocationHistoryService
import com.google.android.gms.semanticlocationhistory.utils.SegmentEditHandler
import com.google.android.gms.semanticlocationhistory.utils.SegmentQueryHandler
import kotlinx.coroutines.launch
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.feature.GoogleFeaturePreferences
import org.microg.gms.utils.warnOnTransactionIssues

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
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        Log.d(TAG, "handleServiceRequest: packageName: $packageName")
        OdlhBackupService.scheduleBackup(this)
        callback.onPostInitCompleteWithConnectionInfo(
            ConnectionResult.SUCCESS, SemanticLocationHistoryServiceImpl(this, lifecycle).asBinder(), ConnectionInfo().apply {
                features = FEATURES
            })
    }
}

class SemanticLocationHistoryServiceImpl(val context: Context, override val lifecycle: Lifecycle) : ISemanticLocationHistoryService.Stub(), LifecycleOwner {
    private val storageManager by lazy { OdlhStorageManager.getInstance(context) }

    override fun getSegments(callback: ISemanticLocationHistoryCallbacks?, requestCredentials: RequestCredentials?, request: LocationHistorySegmentRequest?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "getSegments: requestCredentials=$requestCredentials, request=$request")
        val allowedMapsTimelineFeature = GoogleFeaturePreferences.allowedMapsTimelineFeature(context)
        if (!allowedMapsTimelineFeature) {
            Log.w(TAG, "Not Allowed!!")
            callback?.onGetSegmentsResponse(DataHolder.empty(CommonStatusCodes.SUCCESS), ApiMetadata.SKIP)
            return
        }
        val accountName = requestCredentials?.account?.name
        if (accountName.isNullOrEmpty()) {
            Log.w(TAG, "getSegments: accountName is null or empty")
            callback?.onGetSegmentsResponse(DataHolder.empty(CommonStatusCodes.ERROR), ApiMetadata.DEFAULT)
            return
        }
        lifecycleScope.launch {
            try {
                val gaiaId = context.getObfuscatedGaiaId(accountName)
                Log.d(TAG, "getSegments: gaiaId=${gaiaId.take(8)}...")
                val holder = SegmentQueryHandler.queryAndFilterSegments(storageManager, gaiaId, request)
                Log.d(TAG, "getSegments holder: $holder")
                callback?.onGetSegmentsResponse(holder, ApiMetadata.DEFAULT)
            } catch (e: Exception) {
                Log.e(TAG, "getSegments: failed", e)
                callback?.onGetSegmentsResponse(DataHolder.empty(CommonStatusCodes.ERROR), ApiMetadata.DEFAULT)
            }
        }
    }

    override fun onDemandBackup(callback: IStatusCallback?, requestCredentials: RequestCredentials?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "onDemandBackup: requestCredentials=$requestCredentials")
        val allowedMapsTimelineFeature = GoogleFeaturePreferences.allowedMapsTimelineFeature(context)
        if (!allowedMapsTimelineFeature) {
            Log.w(TAG, "Not Allowed!!")
            callback?.onResult(Status.SUCCESS)
            return
        }
        val accountName = requestCredentials?.account?.name
        if (accountName.isNullOrEmpty()) {
            Log.w(TAG, "onDemandBackup: accountName is null or empty")
            callback?.onResult(Status.SUCCESS)
            return
        }
        lifecycleScope.launch {
            try {
                val result = OdlhBackupProcessor.performBackup(context, accountName)
                Log.d(TAG, "onDemandBackup: result=$result")
                callback?.onResult(if (result.success) Status.SUCCESS else Status(CommonStatusCodes.ERROR, result.message))
            } catch (e: Exception) {
                Log.e(TAG, "onDemandBackup: failed", e)
                callback?.onResult(Status(CommonStatusCodes.ERROR, e.message))
            }
        }
    }

    override fun onDemandRestore(callback: IStatusCallback?, requestCredentials: RequestCredentials?, list: List<*>?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "onDemandRestore: requestCredentials=$requestCredentials")
        val allowedMapsTimelineFeature = GoogleFeaturePreferences.allowedMapsTimelineFeature(context)
        if (!allowedMapsTimelineFeature) {
            Log.w(TAG, "Not Allowed!!")
            callback?.onResult(Status.SUCCESS)
            return
        }
        val accountName = requestCredentials?.account?.name
        if (accountName.isNullOrEmpty()) {
            Log.w(TAG, "onDemandRestore: accountName is null or empty")
            callback?.onResult(Status(CommonStatusCodes.ERROR, "Invalid account"))
            return
        }
        val databaseIds = list?.filterIsInstance<Long>()
        if (databaseIds.isNullOrEmpty()) {
            Log.w(TAG, "onDemandRestore: no valid databaseIds")
            callback?.onResult(Status(CommonStatusCodes.ERROR, "No databaseIds"))
            return
        }
        lifecycleScope.launch {
            try {
                val gaiaId = context.getObfuscatedGaiaId(accountName)
                val result = BackupRestoreHandler.restoreBackups(context, accountName, gaiaId, databaseIds, storageManager)
                if (result.hasData) {
                    storageManager.removeTombstonesOverlapping(gaiaId, result.minStartSec, result.maxEndSec)
                }
                Log.d(TAG, "onDemandRestore: restored ${result.restoredCount} segments")
                callback?.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                Log.e(TAG, "onDemandRestore: failed", e)
                callback?.onResult(Status(CommonStatusCodes.ERROR, e.message))
            }
        }
    }

    override fun getInferredHome(callback: ISemanticLocationHistoryCallbacks?, requestCredentials: RequestCredentials?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "getInferredHome: requestCredentials=$requestCredentials")
        val allowedMapsTimelineFeature = GoogleFeaturePreferences.allowedMapsTimelineFeature(context)
        if (!allowedMapsTimelineFeature) {
            Log.w(TAG, "Not Allowed!!")
            callback?.onGetInferredHomeResponse(Status.SUCCESS, null, ApiMetadata.SKIP)
            return
        }
        val accountName = requestCredentials?.account?.name
        if (accountName.isNullOrEmpty()) {
            callback?.onGetInferredHomeResponse(Status.SUCCESS, null, ApiMetadata.DEFAULT)
            return
        }
        lifecycleScope.launch {
            val now = System.currentTimeMillis() / 1000
            val windowStart = now - SEARCH_WINDOW_DAYS * SECONDS_PER_DAY
            val visitSegments = storageManager.querySegments(
                gaiaId = context.getObfuscatedGaiaId(accountName), startTime = windowStart, endTime = now, segmentTypes = intArrayOf(SEGMENT_TYPE_VISIT)
            )
            val inferredPlace = getInferredPlace(visitSegments, SEMANTIC_TYPE_HOME)
            callback?.onGetInferredWorkResponse(Status.SUCCESS, inferredPlace, ApiMetadata.DEFAULT)
        }
    }

    override fun getInferredWork(callback: ISemanticLocationHistoryCallbacks?, requestCredentials: RequestCredentials?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "getInferredWork: requestCredentials=$requestCredentials")
        val allowedMapsTimelineFeature = GoogleFeaturePreferences.allowedMapsTimelineFeature(context)
        if (!allowedMapsTimelineFeature) {
            Log.w(TAG, "Not Allowed!!")
            callback?.onGetInferredWorkResponse(Status.SUCCESS, null, ApiMetadata.SKIP)
            return
        }
        val accountName = requestCredentials?.account?.name
        if (accountName.isNullOrEmpty()) {
            callback?.onGetInferredWorkResponse(Status.SUCCESS, null, ApiMetadata.DEFAULT)
            return
        }
        lifecycleScope.launch {
            val now = System.currentTimeMillis() / 1000
            val windowStart = now - SEARCH_WINDOW_DAYS * SECONDS_PER_DAY
            val visitSegments = storageManager.querySegments(
                gaiaId = context.getObfuscatedGaiaId(accountName), startTime = windowStart, endTime = now, segmentTypes = intArrayOf(SEGMENT_TYPE_VISIT)
            )
            val inferredPlace = getInferredPlace(visitSegments, SEMANTIC_TYPE_WORK)
            callback?.onGetInferredWorkResponse(Status.SUCCESS, inferredPlace, ApiMetadata.DEFAULT)
        }
    }

    override fun editSegments(callback: ISemanticLocationHistoryCallbacks?, requestCredentials: RequestCredentials?, list: List<LocationHistorySegment>?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "editSegments: requestCredentials=$requestCredentials")
        val allowedMapsTimelineFeature = GoogleFeaturePreferences.allowedMapsTimelineFeature(context)
        if (!allowedMapsTimelineFeature) {
            Log.w(TAG, "Not Allowed!!")
            callback?.onEditSegmentsResponse(Status.SUCCESS, ApiMetadata.SKIP)
            return
        }
        val accountName = requestCredentials?.account?.name
        if (accountName.isNullOrEmpty() || list.isNullOrEmpty()) {
            Log.w(TAG, "editSegments: invalid parameters")
            callback?.onEditSegmentsResponse(Status(CommonStatusCodes.ERROR, "Invalid parameters"), ApiMetadata.DEFAULT)
            return
        }
        lifecycleScope.launch {
            try {
                val gaiaId = context.getObfuscatedGaiaId(accountName)
                SegmentEditHandler.editSegments(storageManager, gaiaId, list)
                callback?.onEditSegmentsResponse(Status.SUCCESS, ApiMetadata.DEFAULT)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "editSegments: ${e.message}")
                callback?.onEditSegmentsResponse(Status(CommonStatusCodes.ERROR, "Invalid segment time range"), ApiMetadata.DEFAULT)
            } catch (e: Exception) {
                Log.e(TAG, "editSegments: failed", e)
                callback?.onEditSegmentsResponse(Status(CommonStatusCodes.ERROR, "Edit failed: ${e.message}"), ApiMetadata.DEFAULT)
            }
        }
    }

    override fun deleteHistory(callback: ISemanticLocationHistoryCallbacks?, requestCredentials: RequestCredentials, startTime: Long, endTime: Long, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "deleteHistory: requestCredentials=$requestCredentials startTime:$startTime endTime:$endTime")
        val allowedMapsTimelineFeature = GoogleFeaturePreferences.allowedMapsTimelineFeature(context)
        if (!allowedMapsTimelineFeature) {
            Log.w(TAG, "Not Allowed!!")
            callback?.onDeleteHistoryResponse(Status.SUCCESS, ApiMetadata.SKIP)
            return
        }
        val accountName = requestCredentials.account?.name
        if (accountName.isNullOrEmpty()) {
            Log.w(TAG, "deleteHistory: accountName is null or empty")
            callback?.onDeleteHistoryResponse(Status(CommonStatusCodes.ERROR, "Invalid account"), ApiMetadata.DEFAULT)
            return
        }
        lifecycleScope.launch {
            try {
                val gaiaId = context.getObfuscatedGaiaId(accountName)
                SegmentEditHandler.deleteHistory(storageManager, gaiaId, startTime, endTime)
                val tombstone = OdlhStorageManager.Tombstone(
                    createdMillis = System.currentTimeMillis(), startTimeSec = startTime, endTimeSec = endTime
                )
                storageManager.addTombstone(gaiaId, tombstone)
                Log.d(TAG, "deleteHistory: added tombstone [$startTime, $endTime)")
                callback?.onDeleteHistoryResponse(Status.SUCCESS, ApiMetadata.DEFAULT)
            } catch (e: Exception) {
                Log.e(TAG, "deleteHistory: failed", e)
                callback?.onDeleteHistoryResponse(Status(CommonStatusCodes.ERROR, "Delete failed: ${e.message}"), ApiMetadata.DEFAULT)
            }
        }
    }

    override fun getBackupSummary(callback: ISemanticLocationHistoryCallbacks?, requestCredentials: RequestCredentials?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "getBackupSummary: requestCredentials=$requestCredentials")
        val allowedMapsTimelineFeature = GoogleFeaturePreferences.allowedMapsTimelineFeature(context)
        if (!allowedMapsTimelineFeature) {
            Log.w(TAG, "Not Allowed!!")
            callback?.onGetBackupSummaryResponse(Status.SUCCESS, emptyList<OdlhBackupSummary>(), ApiMetadata.SKIP)
            return
        }
        val accountName = requestCredentials?.account?.name
        if (accountName.isNullOrEmpty()) {
            callback?.onGetBackupSummaryResponse(Status.SUCCESS, emptyList<OdlhBackupSummary>(), ApiMetadata.DEFAULT)
            return
        }
        lifecycleScope.launch {
            try {
                val summaries = BackupRestoreHandler.fetchBackupSummaries(context, accountName, storageManager)
                Log.d(TAG, "getBackupSummary: returning ${summaries.size} summaries")
                callback?.onGetBackupSummaryResponse(Status.SUCCESS, summaries, ApiMetadata.DEFAULT)
            } catch (e: Exception) {
                Log.e(TAG, "getBackupSummary: failed", e)
                callback?.onGetBackupSummaryResponse(
                    Status(CommonStatusCodes.ERROR, "Backup summary failed: ${e.message}"), emptyList<OdlhBackupSummary>(), ApiMetadata.DEFAULT
                )
            }
        }
    }

    override fun deleteBackups(callback: IStatusCallback?, requestCredentials: RequestCredentials?, list: List<*>?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "deleteBackups: requestCredentials=$requestCredentials")
        val allowedMapsTimelineFeature = GoogleFeaturePreferences.allowedMapsTimelineFeature(context)
        if (!allowedMapsTimelineFeature) {
            Log.w(TAG, "Not Allowed!!")
            callback?.onResult(Status.SUCCESS)
            return
        }
        val accountName = requestCredentials?.account?.name
        if (accountName.isNullOrEmpty()) {
            Log.w(TAG, "deleteBackups: accountName is null or empty")
            callback?.onResult(Status(CommonStatusCodes.ERROR, "Invalid account"))
            return
        }
        val databaseIds = list?.filterIsInstance<Long>()
        if (databaseIds.isNullOrEmpty()) {
            Log.w(TAG, "deleteBackups: no valid databaseIds")
            callback?.onResult(Status(CommonStatusCodes.ERROR, "No databaseIds"))
            return
        }
        lifecycleScope.launch {
            try {
                BackupRestoreHandler.deleteBackups(context, accountName, databaseIds, storageManager)
                Log.d(TAG, "deleteBackups: completed successfully")
                callback?.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                Log.e(TAG, "deleteBackups: failed", e)
                callback?.onResult(Status(CommonStatusCodes.ERROR, e.message))
            }
        }
    }

    override fun getUserLocationProfile(callback: ISemanticLocationHistoryCallbacks?, requestCredentials: RequestCredentials?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "getUserLocationProfile: requestCredentials=$requestCredentials")
        val allowedMapsTimelineFeature = GoogleFeaturePreferences.allowedMapsTimelineFeature(context)
        if (!allowedMapsTimelineFeature) {
            Log.w(TAG, "Not Allowed!!")
            callback?.onGetUserLocationProfileResponse(Status.SUCCESS, null, ApiMetadata.SKIP)
            return
        }
        val accountName = requestCredentials?.account?.name
        Log.d(TAG, "getUserLocationProfile: account=$accountName")
        callback?.onGetUserLocationProfileResponse(Status.SUCCESS, null, ApiMetadata.DEFAULT)
    }

    override fun getLocationHistorySettings(callback: ISemanticLocationHistoryCallbacks?, requestCredentials: RequestCredentials?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "getLocationHistorySettings: requestCredentials=$requestCredentials")
        val allowedMapsTimelineFeature = GoogleFeaturePreferences.allowedMapsTimelineFeature(context)
        if (!allowedMapsTimelineFeature) {
            Log.w(TAG, "Not Allowed!!")
            callback?.onLocationHistorySettings(
                Status.SUCCESS, LocationHistorySettings(false, 0, ReportingState(-1, -1, false, false, 1, 1, 0, false, true)), ApiMetadata.SKIP
            )
            return
        }
        val dbSize = storageManager.getDatabaseSize()
        val accountName = requestCredentials?.account?.name
        if (!accountName.isNullOrEmpty()) {
            val segmentCount = storageManager.getSegmentCount(accountName)
            Log.d(TAG, "getLocationHistorySettings: account=$accountName, segments=$segmentCount, dbSize=$dbSize")
        }
        val deviceTag = if (!accountName.isNullOrEmpty()) storageManager.getDeviceTag(accountName) else 0
        Log.d(TAG, "getLocationHistorySettings_deviceTag: $deviceTag")
        val reportingState = ReportingState(-1, 1, false, false, 1, 1, deviceTag, false, true)
        val settings = LocationHistorySettings(true, deviceTag, reportingState)
        callback?.onLocationHistorySettings(Status.SUCCESS, settings, ApiMetadata.DEFAULT)
    }

    override fun getExperimentVisits(callback: ISemanticLocationHistoryCallbacks?, requestCredentials: RequestCredentials?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "Not yet implemented: getExperimentVisits requestCredentials:$requestCredentials")
        val deviceMetadata = DeviceMetadata(listOf("0"), false, false, emptyList<DeletionRange>(), 0)
        val response = ExperimentVisitsResponse(emptyList<LocationHistorySegment>(), 0, deviceMetadata)
        callback?.onGetExperimentVisitsResponse(Status.SUCCESS, response, ApiMetadata.SKIP)
    }

    override fun editCsl(callback: IStatusCallback?, requestCredentials: RequestCredentials?, editInputs: SemanticLocationEditInputs?, state: SemanticLocationState?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "Not yet implemented: editCsl editInputs:$editInputs state:$state")
        callback?.onResult(Status.SUCCESS)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        return warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
    }

}