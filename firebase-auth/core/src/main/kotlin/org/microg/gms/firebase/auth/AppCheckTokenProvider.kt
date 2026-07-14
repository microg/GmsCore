/*
 * SPDX-FileCopyrightText: 2024, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.firebase.auth

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.droidguard.DroidGuardClient
import com.google.android.gms.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.microg.gms.safetynet.Attestation
import org.microg.gms.safetynet.SafetyNetPreferences
import org.microg.gms.utils.singleInstanceOf
import org.microg.gms.droidguard.core.DroidGuardPreferences
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "GmsFirebaseAppCheck"
private const val SAFETYNET_API_KEY = "AIzaSyDqVnJBjE5ymo--oBJt3On7HQx9xNm1RHA"

/**
 * Manages Firebase App Check token acquisition and caching for the
 * IdentityToolkitClient.
 *
 * Flow:
 * 1. Build a SafetyNet attestation payload for the calling app
 * 2. Obtain a DroidGuard result (hardware-backed attestation)
 * 3. Send the attestation request to Google's androidcheck API
 * 4. Exchange the resulting JWS for a Firebase App Check token at
 *    firebaseappcheck.googleapis.com
 * 5. Cache the token until it expires
 * 6. The cached token is used as the X-Firebase-AppCheck header value
 */
class AppCheckTokenProvider(
    private val context: Context,
    private val apiKey: String,
    /** Numeric Google Cloud project number (e.g. "123456789012") */
    private val projectNumber: String?,
    /** Firebase App ID (e.g. "1:123456789012:android:abcdef1234567890") */
    private val firebaseAppId: String?,
    private val packageName: String,
) {
    private val queue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }
    private val random = SecureRandom()

    private var cachedToken: String? = null
    private var tokenExpiryMs: Long = 0L

    /**
     * Obtain a cached or fresh Firebase App Check token.
     *
     * Performs the full SafetyNet attestation + App Check exchange flow
     * when no cached token is available or the cached token has expired.
     * Returns null when App Check is unavailable or any step fails.
     */
    suspend fun getToken(): String? {
        if (cachedToken != null && SystemClock.elapsedRealtime() < tokenExpiryMs - 300_000L) {
            return cachedToken
        }
        if (projectNumber == null || firebaseAppId == null) return null
        if (!SafetyNetPreferences.isEnabled(context)) return null
        if (!DroidGuardPreferences.isAvailable(context)) return null

        return try {
            val jws = obtainSafetyNetAttestation() ?: return null
            val token = exchangeForAppCheckToken(jws) ?: return null
            cachedToken = token
            token
        } catch (e: Exception) {
            Log.w(TAG, "Failed to obtain App Check token", e)
            null
        }
    }

    /**
     * Performs a SafetyNet attestation and returns the JWS result.
     *
     * Steps:
     * 1. Build a SafetyNetData protobuf payload with app identity and nonce
     * 2. DroidGuard-process the payload hash for hardware-backed proof
     * 3. POST the combined data to Google's androidcheck API
     */
    private suspend fun obtainSafetyNetAttestation(): String? {
        return try {
            val nonce = ByteArray(32).also { random.nextBytes(it) }
            val attestation = Attestation(context, packageName)
            attestation.buildPayload(nonce)

            val data = mapOf("contentBinding" to attestation.payloadHashBase64)
            val dg = withContext(Dispatchers.IO) {
                DroidGuardClient.getResults(context, "attest", data).await()
            }
            attestation.setDroidGuardResult(dg)

            withContext(Dispatchers.IO) { attestation.attest(SAFETYNET_API_KEY) }
        } catch (e: Exception) {
            Log.w(TAG, "SafetyNet attestation failed", e)
            null
        }
    }

    /**
     * Exchange a SafetyNet attestation JWS for a Firebase App Check token.
     *
     * POST to firebaseappcheck.googleapis.com with the SafetyNet JWS.
     * The response contains an attestation_token (JWT) and a TTL.
     */
    private suspend fun exchangeForAppCheckToken(safetyNetJws: String): String? {
        val appId = firebaseAppId ?: return null
        val projectNum = projectNumber ?: return null

        val url = (
            "https://firebaseappcheck.googleapis.com/v1/" +
            "projects/$projectNum/apps/$appId:exchangeSafetyNetToken" +
            "?key=$apiKey"
        )

        return suspendCoroutine { cont ->
            queue.add(JsonObjectRequest(
                Request.Method.POST, url,
                JSONObject().apply {
                    put("safety_net_token", safetyNetJws)
                    put("version", 2)
                },
                { response ->
                    val token = response.optString("attestationToken", null)
                    if (token != null && token.isNotEmpty()) {
                        val ttl = response.optString("ttl", "3600s")
                        tokenExpiryMs = SystemClock.elapsedRealtime() + parseTtlSeconds(ttl) * 1000L
                        Log.d(TAG, "App Check token acquired, TTL: $ttl")
                    }
                    cont.resume(token)
                },
                { error ->
                    Log.w(TAG, "App Check exchange failed: ${error.networkResponse?.statusCode} " +
                        "${error.networkResponse?.data?.decodeToString() ?: error.message}")
                    cont.resume(null)
                }
            ))
        }
    }

    fun invalidate() {
        cachedToken = null
        tokenExpiryMs = 0L
    }

    companion object {
        private fun parseTtlSeconds(ttl: String): Long {
            return try {
                when {
                    ttl.endsWith("s") -> ttl.dropLast(1).toLong()
                    ttl.endsWith("m") -> ttl.dropLast(1).toLong() * 60
                    ttl.endsWith("h") -> ttl.dropLast(1).toLong() * 3600
                    else -> 3600L
                }
            } catch (e: NumberFormatException) {
                3600L
            }
        }
    }
}
