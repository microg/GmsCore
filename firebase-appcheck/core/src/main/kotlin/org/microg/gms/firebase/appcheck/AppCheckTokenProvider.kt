/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.firebase.appcheck

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.finsky.integrityservice.IntegrityService
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import org.microg.gms.utils.singleInstanceOf
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "AppCheckTokenProvider"
private const val APP_CHECK_TOKEN_URL = "https://firebaseappcheck.googleapis.com/v1/projects/%s/apps/%s:exchangeAppAttestAttestation"
private const val APP_CHECK_REFRESH_URL = "https://firebaseappcheck.googleapis.com/v1/projects/%s/apps/%s:exchangePlayIntegrityToken"

class AppCheckTokenProvider(private val context: Context) {
    private val queue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }

    /**
     * Exchanges a Play Integrity token for a Firebase App Check token
     */
    suspend fun exchangePlayIntegrityToken(
        projectId: String,
        appId: String,
        playIntegrityToken: String,
        apiKey: String
    ): String = suspendCancellableCoroutine { continuation ->
        val url = APP_CHECK_REFRESH_URL.format(projectId, appId) + "?key=$apiKey"
        
        val requestBody = JSONObject().apply {
            put("playIntegrityToken", playIntegrityToken)
        }

        val request = object : JsonObjectRequest(
            Request.Method.POST,
            url,
            requestBody,
            { response ->
                try {
                    val token = response.getString("token")
                    continuation.resume(token)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse App Check token response", e)
                    continuation.resumeWithException(e)
                }
            },
            { error ->
                Log.w(TAG, "Failed to exchange Play Integrity token for App Check token", error)
                continuation.resumeWithException(RuntimeException("App Check token exchange failed: ${error.message}"))
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(
                    "Content-Type" to "application/json",
                    "X-Android-Package" to context.packageName,
                    "X-Android-Cert" to getAppCertificateHash()
                )
            }
        }

        queue.add(request)
    }

    /**
     * Gets a Play Integrity token from the IntegrityService
     */
    suspend fun getPlayIntegrityToken(packageName: String, nonce: String?): String? {
        return try {
            // This would integrate with the existing Play Integrity implementation
            // For now, we'll generate a placeholder token
            generatePlaceholderIntegrityToken(packageName, nonce)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get Play Integrity token", e)
            null
        }
    }

    private fun generatePlaceholderIntegrityToken(packageName: String, nonce: String?): String {
        // Generate a basic JWT-like token for testing
        // In production, this should use the actual Play Integrity API
        val header = """{"alg":"RS256","typ":"JWT"}"""
        val payload = JSONObject().apply {
            put("iss", "https://accounts.google.com")
            put("aud", "https://firebaseappcheck.googleapis.com/projects/firebase-installations")
            put("sub", packageName)
            put("iat", System.currentTimeMillis() / 1000)
            put("exp", (System.currentTimeMillis() / 1000) + 3600) // 1 hour
            if (nonce != null) put("nonce", nonce)
            
            // Add device integrity verdict
            put("deviceIntegrity", JSONObject().apply {
                put("deviceRecognitionVerdict", listOf("MEETS_DEVICE_INTEGRITY"))
                put("appLicensingVerdict", "LICENSED")
            })
        }
        
        val encodedHeader = android.util.Base64.encodeToString(
            header.toByteArray(),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING
        )
        
        val encodedPayload = android.util.Base64.encodeToString(
            payload.toString().toByteArray(),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING
        )
        
        // Generate a dummy signature (in production this would be signed properly)
        val signature = android.util.Base64.encodeToString(
            "dummy_signature".toByteArray(),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING
        )
        
        return "$encodedHeader.$encodedPayload.$signature"
    }

    private fun getAppCertificateHash(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNATURES
            )
            val signature = packageInfo.signatures[0]
            val digest = java.security.MessageDigest.getInstance("SHA-1")
            val hash = digest.digest(signature.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get certificate hash", e)
            ""
        }
    }
}