/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.carrierauth

import android.util.Log
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils

private const val TAG = "GmsCarrierAuthSvc"

class CarrierAuthService : BaseService(TAG, GmsService.CARRIER_AUTH) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest?, service: GmsService?) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request?.packageName)
        Log.d(TAG, "handleServiceRequest from $packageName")
        callback.onPostInitComplete(0, CarrierAuthServiceImpl(this).asBinder(), null)
    }
}
