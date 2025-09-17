/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.locationsharingreporter.service

import android.accounts.Account
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.locationsharingreporter.LocationUploadRequest
import com.google.android.gms.locationsharingreporter.NoticeAckedUpdateRequest
import com.google.android.gms.locationsharingreporter.PeriodicLocationReportingIssues
import com.google.android.gms.locationsharingreporter.PeriodicLocationUploadRequest
import com.google.android.gms.locationsharingreporter.StartLocationReportingRequest
import com.google.android.gms.locationsharingreporter.StopLocationReportingRequest
import com.google.android.gms.locationsharingreporter.internal.ILocationReportingIssuesCallback
import com.google.android.gms.locationsharingreporter.internal.ILocationReportingStatusCallbacks
import com.google.android.gms.locationsharingreporter.internal.ILocationSharingReporterService
import com.google.android.gms.locationsharingreporter.internal.ILocationUploadCallbacks
import com.google.android.gms.locationsharingreporter.service.LocationSharingUpdate.Companion.startUpdateLocation
import com.google.android.gms.locationsharingreporter.service.LocationSharingUpdate.Companion.stopUpdateLocation
import com.google.android.gms.locationsharingreporter.service.ReportingRequestStoreFile.isLocationSharingEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import java.util.concurrent.ExecutionException

private const val TAG = "LocationSharingReporter"
val FEATURES = arrayOf(
        Feature("LOCATION_SHARING_REPORTER", 2),
        Feature("LOCATION_SHARING_REPORTER_SYNC", 1),
        Feature("PERIODIC_LOCATION_SHARING_REPORTER", 1),
        Feature("START_LOCATION_REPORTING", 1),
        Feature("STOP_LOCATION_REPORTING", 1),
        Feature("GET_REPORTING_ISSUES", 1),
        Feature("UPDATE_NOTICE_STATE", 1),
        Feature("LOCATION_SHARING", 1),
)

class LocationSharingReporterApiService : BaseService(TAG, GmsService.LOCATION_SHARING_REPORTER) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService?) {
        Log.d(TAG, "handleServiceRequest: $request")
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
                ?: throw IllegalArgumentException("Missing package name")
        val callingPackageName = PackageUtils.getCallingPackage(this) ?: packageName
        callback.onPostInitCompleteWithConnectionInfo(
                CommonStatusCodes.SUCCESS,
                LocationSharingReporterApiServiceImpl(this, callingPackageName, lifecycle).asBinder(),
                ConnectionInfo().apply { features = FEATURES }
        )
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        super.onDestroy()
    }
}

class LocationSharingReporterApiServiceImpl(
        val context: Context,
        val callingPackageName: String,
        override val lifecycle: Lifecycle
) : ILocationSharingReporterService.Stub(), LifecycleOwner {

    override fun updateLocation(callback: ILocationUploadCallbacks, account: Account, request: LocationUploadRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "Not yet implemented: updateLocation called with account: ${account.name}")
    }

    override fun getReportingStatus(callback: ILocationReportingStatusCallbacks, account: Account, apiMetadata: ApiMetadata) {
        Log.d(TAG, "Not yet implemented: getReportingStatus called with account: ${account.name}")
    }

    override fun syncReportingStatus(callback: IStatusCallback, account: Account, apiMetadata: ApiMetadata) {
        Log.d(TAG, "Not yet implemented: syncReportingStatus called with account: ${account.name}")
    }

    override fun periodicLocationUpload(callback: IStatusCallback, account: Account, request: PeriodicLocationUploadRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "Not yet implemented: periodicLocationUpload called with account: ${account.name}, request: $request")
    }

    override fun startLocationReporting(callback: IStatusCallback, account: Account, request: StartLocationReportingRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "startLocationReporting called with account: ${account.name}, request: $request")
        try {
            validateGoogleAccount(account)
            validateMakePrimaryOption(request.makePrimary)
            validateReportingType(request.reportingType)

            //Start location sharing, location sharing is enabled by default
            sendLocationSharingEnable(true, account, context)


            if (request.reportingType == ReportingType.SINGLE_SHARE_REPORTING_ENABLED.value) {
                val locationShare = request.locationShare
                        ?: return callback.onResult(Status.INTERNAL_ERROR.also {
                            Log.w(TAG, "Location share is null for SINGLE_SHARE_REPORTING")
                        })
                validateLocationShare(locationShare)
                require(request.requestDurationMs > 0L) {
                    "Request duration must be non-zero for SINGLE_SHARE_REPORTING"
                }
            } else {
                require(request.locationShare == null) {
                    "Location share must not be specified for ONGOING_REPORTING"
                }
                require(request.requestDurationMs == 0L) {
                    "Request duration must not be specified for ONGOING_REPORTING"
                }
            }

            Log.d(TAG, "Adding new start reporting request")
            if (request.noticeAckedUpdateRequest == null) {
                Log.w(TAG, "Notice acked update request is null")
                callback.onResult(Status.SUCCESS)
                return
            }
            startUpdateLocation(account, context)
            lifecycleScope.launch {
                val reportingRequestStore = withContext(Dispatchers.IO) {
                    val readSharesResponse = requestReadShares(context, account)
                    readSharesResponseDetail(readSharesResponse, context, account)
                }
                if (reportingRequestStore.accountLocationSharingMap.isNotEmpty()) {
                    startUpdateLocation(account, context)
                }
                callback.onResult(Status.SUCCESS)
            }
        } catch (e: NullPointerException) {
            Log.w(TAG, "Internal error while handling new start location reporting request.", e)
            callback.onResult(Status.INTERNAL_ERROR)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Error while handling new start location reporting request.", e)
            callback.onResult(Status(CommonStatusCodes.ERROR, "Error while handling new start location reporting request."))
        }
    }

    override fun stopLocationReporting(callback: IStatusCallback?, account: Account, request: StopLocationReportingRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "stopLocationReporting called with account: ${account.name}, request: $request")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val readSharesResponse = requestReadShares(context, account)
                val reportingRequestStore = readSharesResponseDetail(readSharesResponse, context, account)
                val locationShareListEmpty = reportingRequestStore.accountLocationSharingMap.isEmpty()
                if (locationShareListEmpty) {
                    stopUpdateLocation()
                }
                callback?.onResult(Status.SUCCESS)
            } catch (e: NullPointerException) {
                Log.w(TAG, "Internal error while handling stop location reporting request.", e)
                callback?.onResult(Status.INTERNAL_ERROR)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Error while handling stop location reporting request.", e)
                callback?.onResult(Status.INTERNAL_ERROR)
            }
        }
    }

    override fun updateNoticeState(callback: IStatusCallback?, account: Account, request: NoticeAckedUpdateRequest, apiMetadata: ApiMetadata) {
        Log.d(TAG, "updateNoticeState called with account: ${account.name}, request: $request")
        try {
            callback?.onResult(Status.SUCCESS)
        } catch (e: NullPointerException) {
            Log.w(TAG, "Internal error while handling update notice state request.")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Error while handling update notice state request.")
        } catch (e: Exception) {
            Log.w(TAG, "Error while updateNoticeState $e")
        }
    }

    override fun getReportingIssues(callback: ILocationReportingIssuesCallback, account: Account, apiMetadata: ApiMetadata) {
        Log.d(TAG, "getReportingIssues called with account: ${account.name}")
        try {
            updateDeviceLocationSettingState(context)
            updateDeviceBatterySaverState(context)
            getLocationReportingStatus(context)

            val item = ReportingObject.issuesByAccount[account.name] ?: emptySet()

            val issuesByAccount = if (item.isNotEmpty()) {
                Bundle().apply {
                    putIntArray(account.name, item.toIntArray())
                }
            } else {
                val isLocationSharingEnabled = isLocationSharingEnabled(context, account.name)
                val issues = if (!isLocationSharingEnabled) {
                    mutableSetOf(LocationShareIssue.SHARING_DISABLED.code)
                } else {
                    emptySet()
                }
                Bundle().apply {
                    putIntArray(account.name, issues.toIntArray())
                }
            }

            lifecycleScope.launch {
                val reportingRequestStore = withContext(Dispatchers.IO) {
                    val readSharesResponse = requestReadShares(context, account)
                    readSharesResponseDetail(readSharesResponse, context, account)
                }
                val locationSharingEnabled = isLocationSharingEnabled(context, account.name)
                if (reportingRequestStore.accountLocationSharingMap.isNotEmpty() && locationSharingEnabled) {
                    startUpdateLocation(account, context)
                }
            }

            val issues = PeriodicLocationReportingIssues(
                    ReportingObject.generalIssues.toIntArray(),
                    issuesByAccount,
                    true)
            Log.d(TAG, "getReportingIssues called with account: ${account.name}, issues: $issues")
            callback.onResult(Status.SUCCESS, issues, ApiMetadata(null))
        } catch (e: NullPointerException) {
            Log.w(TAG, "Internal error while handling getReportingIssues request.", e)
            callback.onResult(Status.INTERNAL_ERROR, null, null)
        } catch (e: ExecutionException) {
            Log.w(TAG, "Error while handling getReportingIssues request.", e)
            callback.onResult(Status(CommonStatusCodes.ERROR), null, null)
        } catch (e: Exception) {
            Log.w(TAG, "Error while handling getReportingIssues request.1", e)
            callback.onResult(Status.INTERNAL_ERROR, null, null)
        }
    }
}
