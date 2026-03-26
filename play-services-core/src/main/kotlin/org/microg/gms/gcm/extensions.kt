/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.gcm

import com.squareup.wire.GrpcClient
import com.squareup.wire.Service
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

const val ACTION_GCM_RECONNECT = "org.microg.gms.gcm.RECONNECT"
const val ACTION_GCM_CONNECTED = "org.microg.gms.gcm.CONNECTED"
const val ACTION_GCM_REGISTER_ACCOUNT = "org.microg.gms.gcm.REGISTER_ACCOUNT"
const val ACTION_GCM_REGISTER_ALL_ACCOUNTS = "org.microg.gms.gcm.REGISTER_ALL_ACCOUNTS"
const val ACTION_GCM_NOTIFY_COMPLETE = "org.microg.gms.gcm.NOTIFY_COMPLETE"
const val KEY_GCM_REGISTER_ACCOUNT_NAME = "register_account_name"
const val EXTRA_NOTIFICATION_ACCOUNT = "notification_account"

const val GMS_NOTS_OAUTH_SERVICE = "oauth2:https://www.googleapis.com/auth/notifications"
const val GMS_NOTS_BASE_URL = "https://notifications-pa.googleapis.com"

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
