/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.manager

import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.InternalIntentMessenger
import org.microg.gms.common.PackageUtils
import org.microg.gms.location.EXTRA_LOCATION
import org.microg.gms.utils.IntentCacheManager
import java.io.FileDescriptor
import java.io.PrintWriter


class GoogleLocationManagerService : BaseService(TAG, GmsService.GOOGLE_LOCATION_MANAGER) {
    private val locationManager = LocationManager(this, lifecycle)
    private val internalIntentHandler = InternalIntentMessenger(this::handleInternalIntent)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        locationManager.start()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun handleInternalIntent(intent: Intent) {
        locationManager.start()
        if (intent.action == LocationManagerService.ACTION_REPORT_LOCATION) {
            val location = intent.getParcelableExtra<Location>(EXTRA_LOCATION)
            if (location != null) {
                locationManager.updateNetworkLocation(location)
            }
        }
        if (IntentCacheManager.isCache(intent)) {
            locationManager.handleCacheIntent(intent)
        }
    }

    override fun onDestroy() {
        locationManager.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        if (intent.action == ACTION_BIND_INTERNAL) {
            return internalIntentHandler.asBinder()
        }
        return super.onBind(intent)
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
        const val ACTION_BIND_INTERNAL = "org.microg.gms.location.manager.BIND_INTERNAL"
    }
}