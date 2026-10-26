/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.carrierauth

import android.content.Context
import android.os.Parcel
import android.util.Log
import com.google.android.gms.carrierauth.CarrierAuthRequest
import com.google.android.gms.carrierauth.CarrierAuthResult
import com.google.android.gms.carrierauth.internal.ICarrierAuthApiService
import com.google.android.gms.carrierauth.internal.ICarrierAuthCallbacks
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "CarrierAuthService"

class CarrierAuthService : BaseService(TAG, GmsService.CARRIER_AUTH) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        Log.d(TAG, "handleServiceRequest from $packageName")
        callback.onPostInitComplete(ConnectionResult.SUCCESS, CarrierAuthServiceImpl(this, packageName).asBinder(), null)
    }
}

class CarrierAuthServiceImpl(private val context: Context, private val packageName: String?) : ICarrierAuthApiService.Stub() {

    override fun getCarrierAuthToken(request: CarrierAuthRequest?, callbacks: ICarrierAuthCallbacks) {
        Log.d(TAG, "getCarrierAuthToken: $request")
        val result = CarrierAuthResult(
            1, // SUCCESS
            "carrier_auth_token_microg_${System.currentTimeMillis()}",
            byteArrayOf(0x01)
        )
        try {
            callbacks.onCarrierAuthResult(Status.SUCCESS, result)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to deliver onCarrierAuthResult callback", e)
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
