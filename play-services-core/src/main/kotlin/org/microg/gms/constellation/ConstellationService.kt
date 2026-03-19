/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.content.Context
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import com.google.android.gms.constellation.internal.IConstellationService
import kotlinx.coroutines.launch
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils

private const val TAG = "ConstellationService"

class ConstellationService : BaseService(TAG, GmsService.CONSTELLATION) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        Log.d(TAG, "handleServiceRequest for package: $packageName")
        
        callback.onPostInitComplete(
            CommonStatusCodes.SUCCESS,
            ConstellationServiceImpl(this, packageName ?: "unknown", lifecycle),
            null
        )
    }
}

class ConstellationServiceImpl(
    private val context: Context,
    private val packageName: String,
    override val lifecycle: Lifecycle
) : IConstellationService.Stub(), LifecycleOwner {

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    override fun getPhoneNumber(callbacks: IConstellationCallbacks?) {
        Log.d(TAG, "getPhoneNumber() called by $packageName")
        lifecycleScope.launch {
            try {
                val phoneNumber = telephonyManager?.line1Number ?: ""
                callbacks?.onPhoneNumber(Status.SUCCESS, phoneNumber)
            } catch (e: Exception) {
                Log.w(TAG, "Error in getPhoneNumber", e)
                callbacks?.onPhoneNumber(Status(CommonStatusCodes.INTERNAL_ERROR), "")
            }
        }
    }

    override fun verifyPhoneNumber(callbacks: IConstellationCallbacks?, phoneNumber: String?) {
        Log.d(TAG, "verifyPhoneNumber($phoneNumber) called by $packageName")
        lifecycleScope.launch {
            try {
                // Just mock verification success for now
                callbacks?.onVerificationResult(Status.SUCCESS, true)
            } catch (e: Exception) {
                Log.w(TAG, "Error in verifyPhoneNumber", e)
                callbacks?.onVerificationResult(Status(CommonStatusCodes.INTERNAL_ERROR), false)
            }
        }
    }
}
