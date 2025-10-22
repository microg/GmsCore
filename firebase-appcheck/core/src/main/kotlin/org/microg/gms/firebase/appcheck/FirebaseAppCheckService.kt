/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.firebase.appcheck

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.firebase.appcheck.AppCheckToken
import com.google.firebase.appcheck.interop.IAppCheckInteropService
import com.google.firebase.appcheck.interop.IAppCheckTokenCallback
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "AppCheckService"
private const val TOKEN_EXPIRY_BUFFER_MS = 5 * 60 * 1000L // 5 minutes

class FirebaseAppCheckService(
    private val context: Context,
    override val lifecycle: Lifecycle,
    private val projectId: String,
    private val appId: String,
    private val apiKey: String
) : IAppCheckInteropService.Stub(), LifecycleOwner {

    private val tokenProvider = AppCheckTokenProvider(context)
    private var cachedToken: AppCheckToken? = null

    override fun getToken(forceRefresh: Boolean, callback: IAppCheckTokenCallback) {
        Log.d(TAG, "getToken called, forceRefresh: $forceRefresh")
        
        lifecycleScope.launch {
            try {
                val token = if (forceRefresh || shouldRefreshToken()) {
                    refreshToken()
                } else {
                    cachedToken ?: refreshToken()
                }
                
                callback.onSuccess(token)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get App Check token", e)
                callback.onFailure(e.message ?: "Failed to get App Check token")
            }
        }
    }

    private fun shouldRefreshToken(): Boolean {
        val token = cachedToken ?: return true
        val currentTime = System.currentTimeMillis()
        return token.expireTimeMillis - currentTime < TOKEN_EXPIRY_BUFFER_MS
    }

    private suspend fun refreshToken(): AppCheckToken {
        Log.d(TAG, "Refreshing App Check token")
        
        // Generate a nonce for the token request
        val nonce = generateNonce()
        
        // Get Play Integrity token
        val playIntegrityToken = tokenProvider.getPlayIntegrityToken(context.packageName, nonce)
            ?: throw RuntimeException("Failed to get Play Integrity token")
        
        // Exchange for App Check token
        val appCheckTokenString = tokenProvider.exchangePlayIntegrityToken(
            projectId, appId, playIntegrityToken, apiKey
        )
        
        // Parse expiry time (tokens typically last 1 hour)
        val expiryTime = System.currentTimeMillis() + (60 * 60 * 1000L) // 1 hour
        
        val token = AppCheckToken(appCheckTokenString, expiryTime)
        cachedToken = token
        
        Log.d(TAG, "Successfully refreshed App Check token")
        return token
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        return android.util.Base64.encodeToString(
            bytes,
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING
        )
    }
}