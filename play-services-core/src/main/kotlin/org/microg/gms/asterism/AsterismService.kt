/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.asterism

import android.content.Context
import android.os.Parcel
import android.util.Log
import com.google.android.gms.asterism.AsterismConsent
import com.google.android.gms.asterism.GetAsterismConsentRequest
import com.google.android.gms.asterism.SetAsterismConsentRequest
import com.google.android.gms.asterism.internal.IAsterismApiService
import com.google.android.gms.asterism.internal.IAsterismCallbacks
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "AsterismService"
private const val PREFS_NAME = "asterism_consent"

class AsterismService : BaseService(TAG, GmsService.ASTERISM) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        Log.d(TAG, "handleServiceRequest from $packageName")
        callback.onPostInitComplete(ConnectionResult.SUCCESS, AsterismServiceImpl(this, packageName).asBinder(), null)
    }
}

class AsterismServiceImpl(private val context: Context, private val packageName: String?) : IAsterismApiService.Stub() {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getAsterismConsent(request: GetAsterismConsentRequest?, callbacks: IAsterismCallbacks) {
        Log.d(TAG, "getAsterismConsent: $request")
        val account = request?.accountName ?: "default"
        val status = prefs.getInt("consent_status_$account", 1) // default to GRANTED (1)
        val timestamp = prefs.getLong("consent_timestamp_$account", System.currentTimeMillis())
        val tosVersion = prefs.getInt("consent_tos_$account", 1)

        val consent = AsterismConsent(
            status,
            timestamp,
            tosVersion,
            request?.accountName,
            byteArrayOf(0x01)
        )
        try {
            callbacks.onConsent(Status.SUCCESS, consent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to deliver onConsent callback", e)
        }
    }

    override fun setAsterismConsent(request: SetAsterismConsentRequest?, callbacks: IAsterismCallbacks) {
        Log.d(TAG, "setAsterismConsent: $request")
        val account = request?.accountName ?: "default"
        val status = request?.consentStatus ?: request?.consent?.consentStatus ?: 1
        val timestamp = request?.consent?.timestamp ?: System.currentTimeMillis()
        val tosVersion = request?.consent?.tosVersion ?: 1

        prefs.edit()
            .putInt("consent_status_$account", status)
            .putLong("consent_timestamp_$account", timestamp)
            .putInt("consent_tos_$account", tosVersion)
            .apply()

        try {
            callbacks.onConsentSet(Status.SUCCESS)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to deliver onConsentSet callback", e)
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
