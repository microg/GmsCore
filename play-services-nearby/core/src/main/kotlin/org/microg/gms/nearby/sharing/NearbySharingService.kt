/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import android.os.Binder
import android.os.IBinder
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
    GmsService.NEARBY_SHARING
) {
    override fun handleServiceRequest(
        callback: IGmsCallbacks,
        request: GetServiceRequest,
        service: GmsService
    ) {
        PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        Log.d(NearbySharingConstants.TAG, "handleServiceRequest: ${request.packageName} for $service")

        val binder = object : Binder() {}
        val info = ConnectionInfo().apply {
            features = arrayOf(
                Feature("nearby_sharing", 1),
                Feature("nearby_sharing_get_phone_numbers", 1)
            )
        }
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, binder, info)
    }
}
