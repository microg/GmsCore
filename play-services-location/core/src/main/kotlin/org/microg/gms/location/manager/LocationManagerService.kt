/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.manager

import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Process
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.location.EXTRA_LOCATION
import org.microg.gms.utils.IntentCacheManager
import java.io.FileDescriptor
import java.io.PrintWriter


class LocationManagerService : BaseService(TAG, GmsService.LOCATION_MANAGER) {
    private val locationManager = LocationManager(this, lifecycle)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        locationManager.start()
        if (Binder.getCallingUid() == Process.myUid() && intent?.action == ACTION_REPORT_LOCATION) {
            val location = intent.getParcelableExtra<Location>(EXTRA_LOCATION)
            if (location != null) {
                locationManager.updateNetworkLocation(location)
            }
        }
        if (intent != null && IntentCacheManager.isCache(intent)) {
            locationManager.handleCacheIntent(intent)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        locationManager.stop()
        super.onDestroy()
    }

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService?) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        locationManager.start()
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            LocationManagerInstance(this, locationManager, packageName, lifecycle).asBinder(),
            ConnectionInfo().apply { features = FEATURES }
        )
    }

    override fun dump(fd: FileDescriptor?, writer: PrintWriter, args: Array<out String>?) {
        super.dump(fd, writer, args)
        locationManager.dump(writer)
    }

    companion object {
        const val ACTION_REPORT_LOCATION = "org.microg.gms.location.manager.ACTION_REPORT_LOCATION"
    }
}