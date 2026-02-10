/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.content.Context
import android.util.Log
import com.squareup.wire.GrpcClient
import com.squareup.wire.Service
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.microg.gms.phonenumberverification.PhoneDeviceVerificationClient
import org.microg.gms.phonenumberverification.ProceedRequest
import org.microg.gms.phonenumberverification.ProceedResponse
import org.microg.gms.phonenumberverification.SyncRequest
import org.microg.gms.phonenumberverification.SyncResponse
import java.util.concurrent.TimeUnit

private const val TAG = "PhoneVerificationClient"
private const val BASE_URL = "https://phonedeviceverification-pa.googleapis.com"
private const val API_KEY = "AIzaSyAP-gfH3qvi6vgHZbSYwQ_XHqV_mXHhzIk"
private const val GMS_PACKAGE_NAME = "com.google.android.gms"
private const val GMS_PACKAGE_SIGNATURE_SHA1 = "38918a453d07199354f8b19af05ec6562ced5788"

class PhoneVerificationClient(
    private val context: Context,
    private val spatulaHeaderProvider: suspend () -> String
) {
    private fun buildOkHttpClient(spatulaHeader: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(ConstellationHeaderInterceptor(spatulaHeader))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun buildGrpcClient(spatulaHeader: String): PhoneDeviceVerificationClient {
        val okHttpClient = buildOkHttpClient(spatulaHeader)
        val grpcClient = GrpcClient.Builder()
            .client(okHttpClient)
            .baseUrl(BASE_URL)
            .minMessageToCompress(Long.MAX_VALUE)
            .build()
        return grpcClient.create(PhoneDeviceVerificationClient::class)
    }

    suspend fun sync(request: SyncRequest): SyncResponse {
        val spatula = spatulaHeaderProvider()
        val client = buildGrpcClient(spatula)
        Log.d(TAG, "Calling Sync with session=${request.header_?.session_id}")
        return client.Sync().execute(request)
    }

    suspend fun proceed(request: ProceedRequest): ProceedResponse {
        val spatula = spatulaHeaderProvider()
        val client = buildGrpcClient(spatula)
        Log.d(TAG, "Calling Proceed with session=${request.header_?.session_id}")
        return client.Proceed().execute(request)
    }
}

private class ConstellationHeaderInterceptor(
    private val spatulaHeader: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("x-goog-api-key", API_KEY)
            .header("x-android-package", GMS_PACKAGE_NAME)
            .header("x-android-cert", GMS_PACKAGE_SIGNATURE_SHA1)
            .header("x-goog-spatula", spatulaHeader)
            .header("te", "trailers")
            .header("user-agent", "grpc-java-okhttp/1.66.0-SNAPSHOT")
            .build()
        return chain.proceed(request)
    }
}
