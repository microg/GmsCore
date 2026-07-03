/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import com.google.android.gms.constellation.*
import com.google.android.gms.constellation.internal.IConstellationApiService
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import com.google.android.gms.common.api.Status

private const val TAG = "GmsConstellation"

class ConstellationServiceImpl(private val context: Context) : IConstellationApiService.Stub() {

    override fun getIidToken(callbacks: IConstellationCallbacks?, request: GetIidTokenRequest?) {
        Log.d(TAG, "getIidToken($request)")
        val response = GetIidTokenResponse()
        response.iidToken = generateIidToken()
        callbacks?.onGetIidToken(Status.SUCCESS, response)
    }

    override fun verifyPhoneNumberV1(callbacks: IConstellationCallbacks?, request: VerifyPhoneNumberRequest?) {
        Log.d(TAG, "verifyPhoneNumberV1($request)")
        handleVerifyPhoneNumber(callbacks, request)
    }

    override fun verifyPhoneNumberSingleUse(callbacks: IConstellationCallbacks?, request: VerifyPhoneNumberRequest?) {
        Log.d(TAG, "verifyPhoneNumberSingleUse($request)")
        handleVerifyPhoneNumber(callbacks, request)
    }

    override fun verifyPhoneNumber(callbacks: IConstellationCallbacks?, request: VerifyPhoneNumberRequest?) {
        Log.d(TAG, "verifyPhoneNumber($request)")
        handleVerifyPhoneNumber(callbacks, request)
    }

    override fun getPnvCapabilities(callbacks: IConstellationCallbacks?, request: GetPnvCapabilitiesRequest?) {
        Log.d(TAG, "getPnvCapabilities($request)")
        val response = GetPnvCapabilitiesResponse()
        response.capable = isDeviceCapable()
        response.hasDroidGuard = true
        callbacks?.onGetPnvCapabilities(Status.SUCCESS, response)
    }

    private fun handleVerifyPhoneNumber(callbacks: IConstellationCallbacks?, request: VerifyPhoneNumberRequest?) {
        val response = VerifyPhoneNumberResponse()
        val phoneNumber = extractPhoneNumber(request)
        response.verifiedPhoneNumber = phoneNumber
        response.verificationStatus = 1
        val iidResponse = GetIidTokenResponse()
        iidResponse.iidToken = generateIidToken()
        response.iidToken = iidResponse.iidToken
        if (phoneNumber != null) {
            val info = PhoneNumberInfo()
            info.phoneNumber = phoneNumber
            info.transportType = 3
            response.phoneNumberInfos = arrayOf(info)
        }
        callbacks?.onVerifyPhoneNumber(Status.SUCCESS, response)
    }

    private fun extractPhoneNumber(request: VerifyPhoneNumberRequest?): String? {
        if (request == null) return null
        if (request.imsiRequests != null) {
            for (imsiRequest in request.imsiRequests) {
                if (imsiRequest != null && imsiRequest.msisdn != null
                    && imsiRequest.msisdn.startsWith("+")) {
                    return imsiRequest.msisdn
                }
            }
        }
        if (request.policyId != null && request.policyId.startsWith("+")) {
            return request.policyId
        }
        return getSimPhoneNumber()
    }

    private fun getSimPhoneNumber(): String? {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.line1Number?.takeIf { it.startsWith("+") }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get phone number", e)
            null
        }
    }

    private fun generateIidToken(): String {
        return "microg-constellation-iid-token"
    }

    private fun isDeviceCapable(): Boolean {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.simState == TelephonyManager.SIM_STATE_READY
        } catch (e: Exception) {
            false
        }
    }
}
