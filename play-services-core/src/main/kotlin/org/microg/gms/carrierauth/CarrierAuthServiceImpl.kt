/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.carrierauth

import android.content.Context
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log

private const val TAG = "GmsCarrierAuth"

class CarrierAuthServiceImpl(private val context: Context) {

    fun asBinder(): Bundle {
        val bundle = Bundle()
        bundle.putString("carrier_auth_version", "1")
        bundle.putBoolean("carrier_auth_available", isSimReady())

        // Report EAP-AKA support capabilities
        bundle.putBoolean("eap_aka_supported", supportsEapAka())
        bundle.putString("carrier_name", getCarrierName())

        Log.d(TAG, "Providing carrier auth capabilities")
        return bundle
    }

    private fun isSimReady(): Boolean {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.simState == TelephonyManager.SIM_STATE_READY
        } catch (e: Exception) {
            false
        }
    }

    private fun supportsEapAka(): Boolean {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (tm.simState != TelephonyManager.SIM_STATE_READY) return false
            // Try a minimal EAP-AKA auth to check if SIM supports it
            val testResponse = tm.getIccAuthentication(
                TelephonyManager.APPTYPE_USIM,
                TelephonyManager.AUTHTYPE_EAP_AKA,
                "00"
            )
            testResponse != null
        } catch (e: Exception) {
            false
        }
    }

    private fun getCarrierName(): String? {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.simOperatorName
        } catch (e: Exception) {
            null
        }
    }
}
