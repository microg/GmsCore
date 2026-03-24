/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.utils

import com.squareup.wire.GrpcClient
import com.squareup.wire.Service
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

class AuthHeaderInterceptor(
    private val oauthToken: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request().newBuilder().header("Authorization", "Bearer $oauthToken")
        return chain.proceed(original.build())
    }
}

inline fun <reified S : Service> createGrpcClient(
    baseUrl: String,
    oauthToken: String,
    minMessageToCompress: Long = Long.MAX_VALUE
): S {
    val client = OkHttpClient.Builder().apply {
        addInterceptor(AuthHeaderInterceptor(oauthToken))
    }.build()
    val grpcClient = GrpcClient.Builder()
        .client(client)
        .baseUrl(baseUrl)
        .minMessageToCompress(minMessageToCompress)
        .build()
    return grpcClient.create(S::class)
}