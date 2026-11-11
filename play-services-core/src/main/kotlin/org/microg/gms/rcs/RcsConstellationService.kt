/*
 * SPDX-FileCopyrightText: 2024, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.util.Log

/**
 * Bound GMS service handling `com.google.android.gms.constellation.service.START` and
 * `com.google.android.gms.rcs.PROVISIONING` requests from Google Messages (`com.google.android.apps.messaging`)
 * so RCS number verification and ACS provisioning succeed without root or Strong Integrity (#2994).
 */
class RcsConstellationService : Service() {
    companion object {
        private const val TAG = "GmsRcsConstellation"
        const val ACTION_CONSTELLATION_START = "com.google.android.gms.constellation.service.START"
        const val ACTION_RCS_PROVISIONING = "com.google.android.gms.rcs.PROVISIONING"
        const val TRANSACTION_VERIFY_PHONE_NUMBER = IBinder.FIRST_CALL_TRANSACTION + 1
        const val TRANSACTION_GET_RCS_CONFIG = IBinder.FIRST_CALL_TRANSACTION + 2
    }

    private val binder = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            return when (code) {
                TRANSACTION_VERIFY_PHONE_NUMBER -> {
                    data.enforceInterface("org.microg.gms.rcs.IRcsConstellationService")
                    val rawPhone = data.readString() ?: ""
                    val simId = data.readString() ?: "microg-sim"
                    val mcc = data.readInt().let { if (it <= 0) 310 else it }
                    val mnc = data.readInt().let { if (it < 0) 260 else it }
                    val res = RcsProvisioningManager.verifyPhoneNumberForRcs(rawPhone, simId, mcc, mnc)
                    reply?.writeNoException()
                    if (res != null) {
                        reply?.writeInt(1)
                        val bundle = Bundle().apply {
                            putString("e164_number", res.e164Number)
                            putString("verification_token", res.verificationToken)
                            putInt("provisioning_state", res.provisioningState)
                            putString("acs_url", res.acsUrl)
                        }
                        bundle.writeToParcel(reply!!, 0)
                    } else {
                        reply?.writeInt(0)
                    }
                    true
                }
                TRANSACTION_GET_RCS_CONFIG -> {
                    data.enforceInterface("org.microg.gms.rcs.IRcsConstellationService")
                    val rawPhone = data.readString() ?: "+15555550100"
                    val mcc = data.readInt().let { if (it <= 0) 310 else it }
                    val mnc = data.readInt().let { if (it < 0) 260 else it }
                    val carrier = RcsProvisioningManager.buildAcsConfigUrl(mcc, mnc)
                    val ver = RcsProvisioningManager.verifyPhoneNumberForRcs(rawPhone, "microg", mcc, mnc)
                    reply?.writeNoException()
                    reply?.writeString(if (ver != null) RcsProvisioningManager.buildWapProvisioningXml(ver, carrier) else "")
                    true
                }
                else -> super.onTransact(code, data, reply, flags)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind action=${intent?.action}")
        return binder
    }
}
