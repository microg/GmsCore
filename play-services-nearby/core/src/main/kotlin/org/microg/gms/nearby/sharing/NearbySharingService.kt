/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import android.os.Binder
import android.util.Log
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils

class NearbySharingService : BaseService(
    NearbySharingConstants.TAG,
    GmsService.NEARBY,
    GmsService.NEARBY_SHARING,
    GmsService.NEARBY_CONNECTIONS
) {
    private var connectionsService: ConnectionsServiceImpl? = null

    override fun handleServiceRequest(
        callback: IGmsCallbacks,
        request: GetServiceRequest,
        service: GmsService
    ) {
        PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        Log.d(NearbySharingConstants.TAG, "handleServiceRequest: ${request.packageName} for $service")

        if (service == GmsService.NEARBY_CONNECTIONS) {
            // Real Connections binding: the AIDL service that drives the
            // mDNS/TCP/crypto engine, replacing the previous empty Binder
            // for this service id. The Share path below is unchanged.
            val impl = connectionsService ?: ConnectionsServiceImpl(this).also { connectionsService = it }
            val info = ConnectionInfo().apply {
                features = arrayOf(
                    Feature("nearby_connections", 1)
                )
            }
            callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, impl, info)
            return
        }

        val binder = object : Binder() {}
        val info = ConnectionInfo().apply {
            features = arrayOf(
                Feature("nearby_sharing", 1),
                Feature("nearby_sharing_get_phone_numbers", 1)
            )
        }
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, binder, info)
    }

    override fun onDestroy() {
        connectionsService?.shutdown()
        connectionsService = null
        super.onDestroy()
    }
}
