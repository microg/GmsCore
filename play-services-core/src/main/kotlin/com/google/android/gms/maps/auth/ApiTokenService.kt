/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.maps.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import com.squareup.wire.GrpcClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import org.microg.gms.common.Constants
import org.microg.gms.common.PackageUtils

private const val TAG = "ApiTokenService"

private const val KEY_PACKAGE_NAME = "PACKAGE_NAME"
private const val KEY_API_KEY = "API_KEY"

private const val DEFAULT_TIME_OUT = 10 * 1000L
private const val DEFAULT_VALIDITY_DURATION = 5 * 24 * 60 * 60 * 1000L
private const val X_GMM_CLIENT_BIN = "kAIF8gICCgA"
private const val X_GOOGLE_API_KEY = "AIzaSyDgmW4ZMvNblSXqMOgsbY8uRrTnfR3E7pY"
private const val API_BASE_URL = "https://mapsmobilesdks-pa.googleapis.com/"

private const val HEADER_GMM_CLIENT_BIN = "X-Gmm-Client-bin"
private const val HEADER_GOOGLE_API_KEY = "X-Goog-Api-Key"

class ApiTokenService : LifecycleService() {

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Log.d(TAG, "onBind")
        return ApiTokenServiceImpl(this, lifecycle).asBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)
    }
}

class ApiTokenServiceImpl(private val context: Context, override val lifecycle: Lifecycle) : IApiTokenService.Stub(), LifecycleOwner {

    private fun errorBundle(code: Short) = bundleOf("ERROR_CODE" to code)
    private fun resultBundle(token: String?, expiryTime: Long?, validityDuration: Long?) = bundleOf(
        "EXPIRY_TIME" to (expiryTime ?: (System.currentTimeMillis() + DEFAULT_VALIDITY_DURATION)),
        "VALIDITY_DURATION" to (validityDuration ?: DEFAULT_VALIDITY_DURATION),
        "API_TOKEN" to token
    )

    override fun requestApiToken(params: Bundle?): Bundle {
        params?.keySet()
        Log.d(TAG, "Method requestApiToken is called. Thread:${Thread.currentThread().name} Params: $params")
        var callerPackageName = params?.getString(KEY_PACKAGE_NAME)
        val packagesForUid = context.applicationContext.packageManager.getPackagesForUid(getCallingUid())
        if (callerPackageName == null || packagesForUid.isNullOrEmpty() || (!packagesForUid.contains(callerPackageName) && !packagesForUid.contains(Constants.GMS_PACKAGE_NAME))) {
            Log.d(TAG, "error: ${String.format(ApiError.ERROR_INVALID_PACKAGE.value, callerPackageName)}")
            return errorBundle(ApiError.ERROR_INVALID_PACKAGE.code)
        }
        val signatureDigest = PackageUtils.firstSignatureDigest(context, callerPackageName)
        if (TextUtils.isEmpty(signatureDigest)) {
            Log.d(TAG, "error: ${String.format(ApiError.ERROR_CERT_NOT_FOUND.value, callerPackageName)}")
            return errorBundle(ApiError.ERROR_CERT_NOT_FOUND.code)
        }
        val requestApiKey = params?.getString(KEY_API_KEY)
        if (TextUtils.isEmpty(requestApiKey)) {
            Log.d(TAG, "error: ${ApiError.ERROR_API_KEY_NOT_FOUND.value}")
            return errorBundle(ApiError.ERROR_API_KEY_NOT_FOUND.code)
        }
        return runBlocking {
            try {
                val apiTokenRequest = ApiTokenRequest.build {
                    apiKey = requestApiKey
                    fingerprint = signatureDigest
                    packageName = callerPackageName
                    expiryTimeMillis = System.currentTimeMillis() + DEFAULT_VALIDITY_DURATION
                }
                val response = withTimeout(DEFAULT_TIME_OUT) {
                    withContext(Dispatchers.IO) {
                        grpcClient().CreateAndroidApiToken().executeBlocking(ApiTokenRequestWrapper.build { request = apiTokenRequest })
                    }
                }
                if (TextUtils.isEmpty(response.apiToken)) {
                    Log.d(TAG, "error: ${ApiError.ERROR_API_TOKEN_EMPTY.value}")
                    errorBundle(ApiError.ERROR_API_TOKEN_EMPTY.code)
                } else {
                    Log.d(TAG, "requestApiToken success. response: $response")
                    resultBundle(response.apiToken, response.expiryTime, response.durationTime)
                }
            } catch (e: Exception) {
                Log.d(TAG, "error: ${String.format(ApiError.ERROR_API_REQUEST.value, e.message)}")
                errorBundle(ApiError.ERROR_API_REQUEST.code)
            }
        }
    }

    private fun grpcClient(): MapsMobileSDKsServiceClient {
        val client = OkHttpClient().newBuilder().addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder().header(HEADER_GMM_CLIENT_BIN, X_GMM_CLIENT_BIN).header(HEADER_GOOGLE_API_KEY, X_GOOGLE_API_KEY)
            val request = requestBuilder.build()
            chain.proceed(request)
        }.build()
        val grpcClient = GrpcClient.Builder().client(client).baseUrl(API_BASE_URL).minMessageToCompress(Long.MAX_VALUE).build()
        return grpcClient.create(MapsMobileSDKsServiceClient::class)
    }

}

private enum class ApiError(val code: Short, val value: String) {
    ERROR_INVALID_PACKAGE(1, "Package name %s doesn't match any process executed with the caller's UID."),
    ERROR_CERT_NOT_FOUND(2, "Certificate footprint was not found for the package: %s."),
    ERROR_API_KEY_NOT_FOUND(3, "API key was not found in the request."),
    ERROR_API_TOKEN_EMPTY(4, "Received empty API token response."),
    ERROR_API_REQUEST(5, "Error requesting API token. message: %s.")
}