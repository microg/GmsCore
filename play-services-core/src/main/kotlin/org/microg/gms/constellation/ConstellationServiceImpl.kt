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
import com.google.android.gms.iid.InstanceID

private const val TAG = "GmsConstellation"

class ConstellationServiceImpl(private val context: Context) : IConstellationApiService.Stub() {

    override fun getIidToken(callbacks: IConstellationCallbacks?, request: GetIidTokenRequest?) {
        Log.d(TAG, "getIidToken($request)")
        val response = GetIidTokenResponse()
        response.iidToken = generateIidToken(request)
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
        response.hasDroidGuard = isDroidGuardAvailable()
        callbacks?.onGetPnvCapabilities(Status.SUCCESS, response)
    }

    private fun handleVerifyPhoneNumber(callbacks: IConstellationCallbacks?, request: VerifyPhoneNumberRequest?) {
        val response = VerifyPhoneNumberResponse()
        val phoneNumber = extractPhoneNumber(request)

        performEapAkaAuth()

        response.verifiedPhoneNumber = phoneNumber ?: getSimPhoneNumber()
        response.verificationStatus = if (phoneNumber != null) 1 else 0
        response.iidToken = generateIidToken(null)

        if (phoneNumber != null) {
            val info = PhoneNumberInfo()
            info.phoneNumber = phoneNumber
            info.carrierId = getCarrierId()
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

    private fun generateIidToken(request: GetIidTokenRequest?): String {
        return try {
            InstanceID.getInstance(context).getToken(DEFAULT_PROJECT_NUMBER, "GCM")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get InstanceID token", e)
            ""
        }
    }

    companion object {
        private const val DEFAULT_PROJECT_NUMBER = "496232013492"
    }

    private fun performEapAkaAuth(): ByteArray? {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (tm.simState != TelephonyManager.SIM_STATE_READY) return null
            val challenge = buildEapAkaIdentityRequest()
            val response = tm.getIccAuthentication(
                TelephonyManager.APPTYPE_USIM,
                TelephonyManager.AUTHTYPE_EAP_AKA,
                challenge
            )
            response?.toByteArray(Charsets.ISO_8859_1)
        } catch (e: SecurityException) {
            Log.w(TAG, "EAP-AKA auth denied: missing permission", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "EAP-AKA auth failed", e)
            null
        }
    }

    private fun buildEapAkaIdentityRequest(): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val imsi = tm.subscriberId
            if (imsi != null) {
                val data = byteArrayOf(0) + imsi.toByteArray()
                data.joinToString("") { "%02X".format(it) }
            } else {
                "00"
            }
        } catch (e: Exception) {
            "00"
        }
    }

    private fun getCarrierId(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                tm.simCarrierId
            } else {
                -1
            }
        } catch (e: Exception) {
            -1
        }
    }

    private fun isDeviceCapable(): Boolean {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.simState == TelephonyManager.SIM_STATE_READY
        } catch (e: Exception) {
            false
        }
    }

    private fun isDroidGuardAvailable(): Boolean {
        return try {
            Class.forName("org.microg.gms.droidguard.HandleProxy")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
