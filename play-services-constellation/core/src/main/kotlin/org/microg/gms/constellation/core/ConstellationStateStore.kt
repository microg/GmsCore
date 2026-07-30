/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import android.content.Context
import android.content.SharedPreferences
import android.telephony.TelephonyManager
import android.util.Log
import com.google.android.gms.constellation.PhoneNumberInfo
import com.google.android.gms.constellation.VerifyPhoneNumberResponse
import java.util.UUID

/**
 * Persistent state store for Constellation verification data.
 * Tracks verified phone numbers, RCS state, tokens, and IID data.
 */
class ConstellationStateStore(context: Context) {

    companion object {
        private const val TAG = "ConstellationStateStore"
        private const val PREFS_NAME = "constellation_state_store"
        private const val KEY_VERIFIED_NUMBERS = "verified_numbers"
        private const val KEY_LAST_VERIFIED_PHONE = "last_verified_phone"
        private const val KEY_LAST_VERIFIED_TIMESTAMP = "last_verified_timestamp"
        private const val KEY_RCS_ENABLED = "rcs_enabled"
        private const val KEY_VERIFICATION_TOKEN = "verification_token"
        private const val KEY_RCS_CONFIG_TOKEN = "rcs_config_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_IID_TOKEN = "iid_token"
        private const val KEY_IID_TOKEN_EXPIRY = "iid_token_expiry"
        private const val KEY_DEVICE_FINGERPRINT = "device_fingerprint"
        private const val KEY_PNV_CACHE = "pnv_cache"
        private const val DEFAULT_TOKEN_TTL = 7L * 24 * 60 * 60 * 1000
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val tm: TelephonyManager? = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    val deviceFingerprint: String by lazy {
        prefs.getString(KEY_DEVICE_FINGERPRINT, null) ?: run {
            val fp = generateFingerprint()
            prefs.edit().putString(KEY_DEVICE_FINGERPRINT, fp).apply()
            fp
        }
    }

    @Synchronized
    fun getVerifiedNumbers(): Set<String> {
        val stored = prefs.getStringSet(KEY_VERIFIED_NUMBERS, emptySet()) ?: emptySet()
        return stored.toSet()
    }

    @Synchronized
    fun addVerifiedNumber(phoneNumber: String, token: String?, rcsConfigToken: String?, ttlMillis: Long) {
        val current = getVerifiedNumbers().toMutableSet()
        current.add(phoneNumber)
        val editor = prefs.edit()
            .putStringSet(KEY_VERIFIED_NUMBERS, current)
            .putString(KEY_LAST_VERIFIED_PHONE, phoneNumber)
            .putLong(KEY_LAST_VERIFIED_TIMESTAMP, System.currentTimeMillis())

        if (token != null) {
            editor.putString(KEY_VERIFICATION_TOKEN, token)
        }
        if (rcsConfigToken != null) {
            editor.putString(KEY_RCS_CONFIG_TOKEN, rcsConfigToken)
        }

        val ttl = if (ttlMillis > 0) ttlMillis else DEFAULT_TOKEN_TTL
        editor.putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + ttl)
        editor.putBoolean(KEY_RCS_ENABLED, true)
        editor.apply()

        Log.d(TAG, "Verified number added: $phoneNumber")
    }

    @Synchronized
    fun removeVerifiedNumber(phoneNumber: String) {
        val current = getVerifiedNumbers().toMutableSet()
        current.remove(phoneNumber)
        prefs.edit()
            .putStringSet(KEY_VERIFIED_NUMBERS, current)
            .apply()
        Log.d(TAG, "Verified number removed: $phoneNumber")
    }

    @Synchronized
    fun isVerified(phoneNumber: String): Boolean {
        if (!getVerifiedNumbers().contains(phoneNumber)) return false
        val expiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0L)
        return expiry == 0L || System.currentTimeMillis() < expiry
    }

    @Synchronized
    fun getLastVerifiedPhoneNumber(): String? = prefs.getString(KEY_LAST_VERIFIED_PHONE, null)

    @Synchronized
    fun getVerificationToken(): String? = prefs.getString(KEY_VERIFICATION_TOKEN, null)

    @Synchronized
    fun getRcsConfigToken(): String? = prefs.getString(KEY_RCS_CONFIG_TOKEN, null)

    @Synchronized
    fun isRcsEnabled(): Boolean = prefs.getBoolean(KEY_RCS_ENABLED, false)

    @Synchronized
    fun getTokenExpiryTimestamp(): Long = prefs.getLong(KEY_TOKEN_EXPIRY, 0L)

    @Synchronized
    fun getPhoneNumberInfo(phoneNumber: String): PhoneNumberInfo {
        val verified = isVerified(phoneNumber)
        val rcsEnabled = isRcsEnabled()
        return PhoneNumberInfo(
            phoneNumber, verified, rcsEnabled,
            if (verified) VerifyPhoneNumberResponse.STATUS_VERIFIED else VerifyPhoneNumberResponse.STATUS_UNKNOWN,
            prefs.getLong(KEY_LAST_VERIFIED_TIMESTAMP, 0L),
            getTokenExpiryTimestamp(),
            getSimOperatorName(), formatPhoneNumber(phoneNumber),
            getSimCountryCode(), isRoaming(), 0, null
        )
    }

    @Synchronized
    fun storeIidToken(token: String, expiryTimestamp: Long) {
        prefs.edit()
            .putString(KEY_IID_TOKEN, token)
            .putLong(KEY_IID_TOKEN_EXPIRY, expiryTimestamp)
            .apply()
        Log.d(TAG, "IID token stored, expiry=$expiryTimestamp")
    }

    @Synchronized
    fun getIidToken(): String? {
        val token = prefs.getString(KEY_IID_TOKEN, null)
        val expiry = prefs.getLong(KEY_IID_TOKEN_EXPIRY, 0L)
        return if (token != null && (expiry == 0L || System.currentTimeMillis() < expiry)) {
            token
        } else {
            null
        }
    }

    @Synchronized
    fun isIidTokenExpired(): Boolean {
        val expiry = prefs.getLong(KEY_IID_TOKEN_EXPIRY, 0L)
        return expiry > 0 && System.currentTimeMillis() >= expiry
    }

    @Synchronized
    fun storePnvCache(phoneNumber: String, capabilitiesJson: String, ttlSeconds: Long) {
        prefs.edit()
            .putString("pnv_cache_$phoneNumber", capabilitiesJson)
            .putLong("pnv_cache_${phoneNumber}_expiry", System.currentTimeMillis() + ttlSeconds * 1000)
            .apply()
    }

    @Synchronized
    fun getPnvCache(phoneNumber: String): String? {
        val expiry = prefs.getLong("pnv_cache_${phoneNumber}_expiry", 0L)
        if (expiry > 0 && System.currentTimeMillis() < expiry) {
            return prefs.getString("pnv_cache_$phoneNumber", null)
        }
        return null
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        Log.d(TAG, "State store cleared")
    }

    private fun generateFingerprint(): String {
        return "constellation_${UUID.randomUUID().toString().take(12)}"
    }

    private fun getSimOperatorName(): String? {
        return try { tm?.simOperatorName } catch (e: SecurityException) { null }
    }

    private fun getSimCountryCode(): Int {
        return try {
            tm?.simCountryIso?.let { iso ->
                // Simple country code mapping
                when (iso.uppercase()) {
                    "US" -> 1; "CA" -> 1; "FR" -> 33; "DE" -> 49; "GB" -> 44
                    "IN" -> 91; "CN" -> 86; "JP" -> 81; "BR" -> 55; "RU" -> 7
                    else -> 0
                }
            } ?: 0
        } catch (e: SecurityException) { 0 }
    }

    private fun isRoaming(): Boolean {
        return try { tm?.isNetworkRoaming ?: false } catch (e: SecurityException) { false }
    }

    private fun formatPhoneNumber(number: String): String? {
        return if (number.length >= 10) {
            "${number.substring(0, 3)}-${number.substring(3, 6)}-${number.substring(6)}"
        } else number
    }
}
