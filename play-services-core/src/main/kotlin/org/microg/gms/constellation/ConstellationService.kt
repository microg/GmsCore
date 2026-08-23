/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.content.Context
import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.constellation.PhoneNumberVerificationRequest
import com.google.android.gms.constellation.VerificationResult
import com.google.android.gms.constellation.internal.IConstellationApiService
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "ConstellationService"

class ConstellationService : BaseService(TAG, GmsService.CONSTELLATION) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        Log.d(TAG, "handleServiceRequest from $packageName")
        callback.onPostInitComplete(ConnectionResult.SUCCESS, ConstellationServiceImpl(this, packageName).asBinder(), null)
    }
}

class ConstellationServiceImpl(private val context: Context, private val packageName: String?) : IConstellationApiService.Stub() {

    override fun verifyPhoneNumber(request: PhoneNumberVerificationRequest?, callbacks: IConstellationCallbacks) {
        Log.d(TAG, "verifyPhoneNumber: $request")
        val number = request?.phoneNumber ?: ""
        val result = VerificationResult(
            1, // SUCCESS / VERIFIED
            number,
            byteArrayOf(0x01, 0x02, 0x03),
            System.currentTimeMillis() + 86400000L * 30 // 30 days valid
        )
        try {
            callbacks.onVerificationResult(Status.SUCCESS, result)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to deliver onVerificationResult callback", e)
        }
    }

    override fun getVerificationStatus(phoneNumber: String?, callbacks: IConstellationCallbacks) {
        Log.d(TAG, "getVerificationStatus: $phoneNumber")
        val result = VerificationResult(
            1,
            phoneNumber ?: "",
            byteArrayOf(0x01, 0x02, 0x03),
            System.currentTimeMillis() + 86400000L * 30
        )
        try {
            callbacks.onVerificationResult(Status.SUCCESS, result)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to deliver onVerificationResult callback", e)
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
