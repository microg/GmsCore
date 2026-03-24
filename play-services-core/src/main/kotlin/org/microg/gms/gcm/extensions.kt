/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.gcm

import android.util.Base64
import com.squareup.wire.GrpcClient
import com.squareup.wire.Service
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.ByteString
import org.microg.gms.auth.AuthResponse
import org.microg.gms.auth.ItAuthData
import org.microg.gms.auth.ItMetadataData
import org.microg.gms.auth.OAuthAuthorization
import org.microg.gms.auth.OAuthTokenData
import org.microg.gms.auth.TokenField
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min

const val ACTION_GCM_MESSAGE_RECEIVE = "org.microg.gms.gcm.MESSAGE_RECEIVE"

const val ACTION_GCM_RECONNECT = "org.microg.gms.gcm.RECONNECT"
const val ACTION_GCM_CONNECTED = "org.microg.gms.gcm.CONNECTED"
const val ACTION_GCM_REGISTER_ACCOUNT = "org.microg.gms.gcm.REGISTER_ACCOUNT"
const val ACTION_GCM_REGISTER_ALL_ACCOUNTS = "org.microg.gms.gcm.REGISTER_ALL_ACCOUNTS"
const val KEY_GCM_REGISTER_ACCOUNT_NAME = "register_account_name"
const val EXTRA_NOTIFICATION_ACCOUNT = "notification_account"

const val DEFAULT_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
const val GMS_NOTS_OAUTH_SERVICE = "oauth2:https://www.googleapis.com/auth/notifications"
const val GMS_NOTS_BASE_URL = "https://notifications-pa.googleapis.com"

const val KEY_GCM_ANDROID_ID = "androidId"
const val KEY_GCM_REG_ID = "regId"

private const val AUTHS_TOKEN_PREFIX = "ya29.m."

fun AuthResponse.parseAuthsToken(): String? {
    if (auths.isNullOrEmpty() || itMetadata.isNullOrEmpty()) return null
    if (!auths.startsWith(AUTHS_TOKEN_PREFIX)) return null
    try {
        val tokenBase64 = auths.substring(AUTHS_TOKEN_PREFIX.length)
        val authData = ItAuthData.ADAPTER.decode(Base64.decode(tokenBase64, DEFAULT_FLAGS))
        val metadata = ItMetadataData.ADAPTER.decode(Base64.decode(itMetadata, DEFAULT_FLAGS))
        val authorization = OAuthAuthorization.Builder().apply {
            effectiveDurationSeconds(min(metadata.liveTime ?: Int.MAX_VALUE, expiresInDurationSec))
            if (metadata.field_?.types?.contains(TokenField.FieldType.SCOPE) == true) {
                val scopeIds = metadata.entries.flatMap { entry ->
                    entry.name.map { scope -> entry to scope }
                }.filter { (_, scope) ->
                    scope in grantedScopes
                }.mapNotNull { (entry, _) ->
                    entry.id
                }.toSet()
                scopeIds(scopeIds.toList())
            }
        }.build()
        val oAuthTokenData = OAuthTokenData.Builder().apply {
            fieldType(TokenField.FieldType.SCOPE.value)
            authorization(authorization.encodeByteString())
            durationMillis(0)
        }.build()
        val tokenDataBytes = oAuthTokenData.encode()
        val secretKey: ByteArray? = authData.signature?.toByteArray()
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(secretKey, "HmacSHA256")) }
        val bytes: ByteArray = mac.doFinal(tokenDataBytes)
        val newAuthData = authData.newBuilder().apply {
            tokens(arrayListOf(oAuthTokenData.encodeByteString()))
            signature(ByteString.of(*bytes))
        }.build()
        return AUTHS_TOKEN_PREFIX + Base64.encodeToString(newAuthData.encode(), DEFAULT_FLAGS)
    } catch (e: Exception) {
        return null;
    }
}

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
    interceptor: Interceptor,
    minMessageToCompress: Long = Long.MAX_VALUE,
): S {
    val client = OkHttpClient.Builder().apply {
        addInterceptor(interceptor)
    }.build()
    val grpcClient = GrpcClient.Builder()
        .client(client)
        .baseUrl(baseUrl)
        .minMessageToCompress(minMessageToCompress)
        .build()
    return grpcClient.create(S::class)
}
