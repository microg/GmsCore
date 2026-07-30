/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.asterism.core

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.WorkerThread
import com.google.android.gms.asterism.GetAsterismConsentResponse
import java.util.UUID

/**
 * Persistent store for Asterism consent state.
 * Manages RCS consent tokens, expiry, and state transitions
 * using Android SharedPreferences for persistence across reboots.
 */
class AsterismConsentStore(context: Context) {

    companion object {
        private const val TAG = "AsterismConsentStore"
        private const val PREFS_NAME = "asterism_consent_store"
        private const val KEY_CONSENT_STATE = "consent_state"
        private const val KEY_CONSENT_TIMESTAMP = "consent_timestamp"
        private const val KEY_EXPIRY_TIMESTAMP = "expiry_timestamp"
        private const val KEY_CONSENT_TOKEN = "consent_token"
        private const val KEY_LAST_REFRESH_TIMESTAMP = "last_refresh_timestamp"
        private const val KEY_REFRESH_COUNT = "refresh_count"
        private const val KEY_DEVICE_ID = "device_id"
        private const val DEFAULT_TTL_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val deviceId: String by lazy {
        prefs.getString(KEY_DEVICE_ID, null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
            newId
        }
    }

    @Synchronized
    fun getCurrentState(): GetAsterismConsentResponse {
        val state = prefs.getInt(KEY_CONSENT_STATE, GetAsterismConsentResponse.CONSENT_UNKNOWN)
        val consentTimestamp = prefs.getLong(KEY_CONSENT_TIMESTAMP, 0L)
        val expiryTimestamp = prefs.getLong(KEY_EXPIRY_TIMESTAMP, 0L)
        val token = prefs.getString(KEY_CONSENT_TOKEN, null)
        val isExpired = state == GetAsterismConsentResponse.CONSENT_GRANTED
                && expiryTimestamp > 0
                && System.currentTimeMillis() > expiryTimestamp

        return if (isExpired) {
            Log.d(TAG, "Consent has expired, returning EXPIRED state")
            updateState(GetAsterismConsentResponse.CONSENT_EXPIRED, consentTimestamp, expiryTimestamp, token)
        } else {
            GetAsterismConsentResponse(
                state, consentTimestamp, expiryTimestamp, token,
                0, null
            )
        }
    }

    @Synchronized
    fun grantConsent(token: String?, ttlMillis: Long): GetAsterismConsentResponse {
        val now = System.currentTimeMillis()
        val ttl = if (ttlMillis > 0) ttlMillis else DEFAULT_TTL_MS
        val expiryTimestamp = now + ttl
        val consentToken = token ?: UUID.randomUUID().toString()
        return updateState(
            GetAsterismConsentResponse.CONSENT_GRANTED,
            now, expiryTimestamp, consentToken
        )
    }

    @Synchronized
    fun revokeConsent(): GetAsterismConsentResponse {
        return updateState(
            GetAsterismConsentResponse.CONSENT_DENIED,
            System.currentTimeMillis(), 0L, null
        )
    }

    @Synchronized
    fun refreshConsent(newTtlMillis: Long): GetAsterismConsentResponse {
        val current = getCurrentState()
        if (current.consentState != GetAsterismConsentResponse.CONSENT_GRANTED) {
            Log.w(TAG, "Cannot refresh consent: current state is ${current.consentState}")
            return current
        }

        val now = System.currentTimeMillis()
        val ttl = if (newTtlMillis > 0) newTtlMillis else DEFAULT_TTL_MS
        val newExpiry = now + ttl
        val refreshCount = prefs.getInt(KEY_REFRESH_COUNT, 0) + 1

        prefs.edit()
            .putLong(KEY_EXPIRY_TIMESTAMP, newExpiry)
            .putLong(KEY_LAST_REFRESH_TIMESTAMP, now)
            .putInt(KEY_REFRESH_COUNT, refreshCount)
            .apply()

        Log.d(TAG, "Consent refreshed. New expiry: $newExpiry, refresh count: $refreshCount")

        return GetAsterismConsentResponse(
            GetAsterismConsentResponse.CONSENT_GRANTED,
            current.consentTimestamp, newExpiry, current.consentToken,
            0, null
        )
    }

    @Synchronized
    fun setPending(): GetAsterismConsentResponse {
        val now = System.currentTimeMillis()
        return updateState(GetAsterismConsentResponse.CONSENT_PENDING, now, now + DEFAULT_TTL_MS, null)
    }

    fun getDeviceId(): String = deviceId

    fun getRefreshCount(): Int = prefs.getInt(KEY_REFRESH_COUNT, 0)

    fun getLastRefreshTimestamp(): Long = prefs.getLong(KEY_LAST_REFRESH_TIMESTAMP, 0L)

    fun clearAll() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Consent store cleared")
    }

    @WorkerThread
    private fun updateState(
        state: Int,
        consentTimestamp: Long,
        expiryTimestamp: Long,
        token: String?
    ): GetAsterismConsentResponse {
        val editor = prefs.edit()
            .putInt(KEY_CONSENT_STATE, state)
            .putLong(KEY_CONSENT_TIMESTAMP, consentTimestamp)
            .putLong(KEY_EXPIRY_TIMESTAMP, expiryTimestamp)

        if (token != null) {
            editor.putString(KEY_CONSENT_TOKEN, token)
        } else if (state != GetAsterismConsentResponse.CONSENT_GRANTED) {
            editor.remove(KEY_CONSENT_TOKEN)
        }

        editor.apply()

        Log.d(TAG, "Consent state updated to $state, token=${if (token != null) "present" else "null"}")

        return GetAsterismConsentResponse(
            state, consentTimestamp, expiryTimestamp, token,
            0, null
        )
    }
}
