/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.firebase.appcheck

import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.os.bundleOf
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.play.core.integrity.protocol.IIntegrityService
import com.google.android.play.core.integrity.protocol.IIntegrityServiceCallback
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
    suspend fun getPlayIntegrityToken(packageName: String, nonce: String?): String? = suspendCancellableCoroutine { continuation ->
        var integrityService: IIntegrityService? = null
        
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                integrityService = IIntegrityService.Stub.asInterface(service)
                
                val request = Bundle().apply {
                    putString("package.name", packageName)
                    putLong("cloud.prj", 0L) // Default cloud project number
                    putByteArray("nonce", nonce?.toByteArray() ?: ByteArray(0))
                }
                
                val callback = object : IIntegrityServiceCallback.Stub() {
                    override fun onRequestIntegrityToken(response: Bundle) {
                        try {
                            val token = response.getString("token")
                            val error = response.getInt("error", 0)
                            
                            if (token != null && error == 0) {
                                Log.d(TAG, "Successfully obtained Play Integrity token")
                                continuation.resume(token)
                            } else {
                                Log.w(TAG, "Failed to get Play Integrity token, error: $error")
                                continuation.resumeWithException(RuntimeException("Play Integrity error: $error"))
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error processing Play Integrity response", e)
                            continuation.resumeWithException(e)
                        } finally {
                            context.unbindService(this@connection)
                        }
                    }
                }
                
                try {
                    integrityService?.requestIntegrityToken(request, callback)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to request integrity token", e)
                    continuation.resumeWithException(e)
                    context.unbindService(this)
                }
            }
            
            override fun onServiceDisconnected(name: ComponentName?) {
                integrityService = null
                if (continuation.isActive) {
                    continuation.resumeWithException(RuntimeException("Integrity service disconnected"))
                }
            }
        }
        
        val intent = Intent().apply {
            component = ComponentName("com.google.android.gms", "com.google.android.finsky.integrityservice.IntegrityService")
        }
        
        if (!context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
            Log.w(TAG, "Failed to bind to Integrity service")
            continuation.resumeWithException(RuntimeException("Failed to bind to Integrity service"))
        }
        
        continuation.invokeOnCancellation {
            try {
                context.unbindService(connection)
            } catch (e: Exception) {
                Log.w(TAG, "Error unbinding service", e)
            }
        }
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