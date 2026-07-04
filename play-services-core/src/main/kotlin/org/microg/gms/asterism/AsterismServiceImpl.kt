/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.asterism

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.asterism.*
import com.google.android.gms.asterism.internal.IAsterismApiService
import com.google.android.gms.asterism.internal.IAsterismCallbacks
import com.google.android.gms.common.api.Status
import java.security.MessageDigest
import java.util.UUID

private const val TAG = "GmsAsterism"
private const val PREFS_NAME = "asterism_consent"
private const val KEY_CONSENTED = "consented"
private const val KEY_CONSENT_TOKEN = "consent_token"

class AsterismServiceImpl(private val context: Context) : IAsterismApiService.Stub() {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun getAsterismConsent(callbacks: IAsterismCallbacks?, request: GetAsterismConsentRequest?) {
        Log.d(TAG, "getAsterismConsent($request)")
        val response = GetAsterismConsentResponse()
        response.consented = prefs.getBoolean(KEY_CONSENTED, true)
        response.consentToken = getOrCreateConsentToken()
        callbacks?.onGetAsterismConsent(Status.SUCCESS, response)
    }

    override fun setAsterismConsent(callbacks: IAsterismCallbacks?, request: SetAsterismConsentRequest?) {
        Log.d(TAG, "setAsterismConsent(consented=${request?.consented})")
        val response = SetAsterismConsentResponse()
        if (request != null) {
            prefs.edit()
                .putBoolean(KEY_CONSENTED, request.consented)
                .putString(KEY_CONSENT_TOKEN, generateConsentToken())
                .apply()
            response.success = true
        } else {
            response.success = false
        }
        callbacks?.onSetAsterismConsent(if (response.success) Status.SUCCESS else Status.ERROR, response)
    }

    private fun getOrCreateConsentToken(): String {
        val existing = prefs.getString(KEY_CONSENT_TOKEN, null)
        if (existing != null) return existing
        val token = generateConsentToken()
        prefs.edit().putString(KEY_CONSENT_TOKEN, token).apply()
        return token
    }

    private fun generateConsentToken(): String {
        return try {
            val source = arrayOf(
                android.os.Build.FINGERPRINT,
                android.os.Build.SERIAL,
                context.packageName
            ).joinToString("|")

            val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray())
            digest.joinToString("") { "%02x".format(it) }.take(32)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate stable consent token, using UUID", e)
            UUID.randomUUID().toString().replace("-", "").take(32)
        }
    }
}
