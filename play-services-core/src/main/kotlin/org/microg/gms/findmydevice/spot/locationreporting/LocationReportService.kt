/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.findmydevice.spot.locationreporting

import android.content.Context
import android.os.Parcel
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.findmydevice.spot.DisableLocationReportingRequest
import com.google.android.gms.findmydevice.spot.GetLocationReportingStateRequest
import com.google.android.gms.findmydevice.spot.LocationReportRequest
import com.google.android.gms.findmydevice.spot.internal.ISpotLocationReportCallbacks
import com.google.android.gms.findmydevice.spot.internal.ISpotLocationReportService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.findmydevice.FEATURES
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "LocationReportService"

class LocationReportService : BaseService(TAG, GmsService.FIND_MY_DEVICE_SPOT) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        val spotManagementServiceImpl = SpotLocationReportServiceImpl(packageName, this, lifecycle)
        callback.onPostInitCompleteWithConnectionInfo(0, spotManagementServiceImpl.asBinder(), ConnectionInfo().apply {
            features = FEATURES
        })
    }
}

class SpotLocationReportServiceImpl(val packageName: String, val context: Context, override val lifecycle: Lifecycle) : ISpotLocationReportService.Stub(), LifecycleOwner {
    override fun locationReport(callbacks: ISpotLocationReportCallbacks?, request: LocationReportRequest?) {
        Log.d(TAG, "Unimplement locationReport: $request")
    }

    override fun getLocationReportingState(
        callbacks: ISpotLocationReportCallbacks?,
        request: GetLocationReportingStateRequest?
    ) {
        Log.d(TAG, "Unimplement getLocationReportingState: $request")
    }

    override fun disableLocationReporting(
        callbacks: ISpotLocationReportCallbacks?,
        request: DisableLocationReportingRequest?
    ) {
        Log.d(TAG, "Unimplement disableLocationReporting: $request")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}