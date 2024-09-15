/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.vending

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.os.Bundle
import android.util.Base64
import android.util.Log
import com.google.android.gms.common.BuildConfig
import okio.ByteString
import org.microg.gms.profile.Build
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID
import java.util.zip.GZIPOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "FakeLicenseRequest"

const val AUTH_TOKEN_SCOPE: String = "oauth2:https://www.googleapis.com/auth/googleplay"

private const val BASE64_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
private const val FINSKY_VERSION = "Finsky/37.5.24-29%20%5B0%5D%20%5BPR%5D%20565477504"

fun buildRequestHeaders(auth: String, androidId: Long, language: List<String> ?= null): Map<String, String> {
    var millis = System.currentTimeMillis()
    val timestamp = TimestampContainer.Builder().container2(
        TimestampContainer2.Builder().wrapper(TimestampWrapper.Builder().timestamp(makeTimestamp(millis)).build()).timestamp(makeTimestamp(millis)).build()
    )
    millis = System.currentTimeMillis()
    timestamp.container1Wrapper(
        TimestampContainer1Wrapper.Builder().androidId(androidId.toString()).container(
            TimestampContainer1.Builder().timestamp(millis.toString() + "000").wrapper(makeTimestamp(millis)).build()
        ).build()
    )

    val encodedTimestamps = String(Base64.encode(timestamp.build().encode().encodeGzip(), BASE64_FLAGS))
    val locality = Locality.Builder().unknown1(1).unknown2(2).countryCode("").region(
        TimestampStringWrapper.Builder().string("").timestamp(makeTimestamp(System.currentTimeMillis())).build()
    ).country(
        TimestampStringWrapper.Builder().string("").timestamp(makeTimestamp(System.currentTimeMillis())).build()
    ).unknown3(0).build()
    val encodedLocality = String(
        Base64.encode(locality.encode(), BASE64_FLAGS)
    )

    val header = RequestHeader.Builder().encodedTimestamps(StringWrapper.Builder().string(encodedTimestamps).build()).triple(
        EncodedTripleWrapper.Builder().triple(
            EncodedTriple.Builder().encoded1("").encoded2("").empty("").build()
        ).build()
    ).locality(LocalityWrapper.Builder().encodedLocalityProto(encodedLocality).build()).unknown(IntWrapper.Builder().integer(5).build()).empty("").deviceMeta(
        DeviceMeta.Builder().android(
            AndroidVersionMeta.Builder().androidSdk(Build.VERSION.SDK_INT).buildNumber(Build.ID).androidVersion(Build.VERSION.RELEASE).unknown(0).build()
        ).unknown1(
            UnknownByte12.Builder().bytes(ByteString.EMPTY).build().toString()
        ).unknown2(1).build()
    ).userAgent(
        UserAgent.Builder().deviceName(Build.DEVICE).deviceHardware(Build.HARDWARE).deviceModelName(Build.MODEL).finskyVersion(FINSKY_VERSION)
            .deviceProductName(Build.MODEL).androidId(androidId) // must not be 0
            .buildFingerprint(Build.FINGERPRINT).build()
    ).uuid(
        Uuid.Builder().uuid(UUID.randomUUID().toString()).unknown(2).build()
    ).apply {
        if (language != null) {
            languages(
                RequestLanguagePackage.Builder().language(language).build()
            )
        }
    }.build().encode()

    val xPsRh = String(Base64.encode(header.encodeGzip(), BASE64_FLAGS))
    Log.v(TAG, "X-PS-RH: $xPsRh")
    val userAgent =
        "$FINSKY_VERSION (api=3,versionCode=${BuildConfig.VERSION_CODE},sdk=${Build.VERSION.SDK}," + "device=${encodeString(Build.DEVICE)},hardware=${
            encodeString(Build.HARDWARE)
        }," + "product=${encodeString(Build.PRODUCT)},platformVersionRelease=${encodeString(Build.VERSION.RELEASE)}," + "model=${encodeString(Build.MODEL)},buildId=${
            encodeString(
                Build.ID
            )
        },isWideScreen=${0}," + "supportedAbis=${Build.SUPPORTED_ABIS.joinToString(";")})"
    Log.v(TAG, "User-Agent: $userAgent")

    return mapOf(
        "X-PS-RH" to xPsRh, "User-Agent" to userAgent, "Authorization" to "Bearer $auth", "Accept-Language" to "en-US", "Connection" to "Keep-Alive"
    )
}

private fun makeTimestamp(millis: Long): Timestamp {
    return Timestamp.Builder().seconds((millis / 1000)).nanos(((millis % 1000) * 1000000).toInt()).build()
}

private fun encodeString(s: String?): String {
    return URLEncoder.encode(s).replace("+", "%20")
}

/**
 * From [StackOverflow](https://stackoverflow.com/a/46688434/), CC BY-SA 4.0 by Sergey Frolov, adapted.
 */
fun ByteArray.encodeGzip(): ByteArray {
    try {
        ByteArrayOutputStream().use { byteOutput ->
            GZIPOutputStream(byteOutput).use { gzipOutput ->
                gzipOutput.write(this)
                gzipOutput.finish()
                return byteOutput.toByteArray()
            }
        }
    } catch (e: IOException) {
        Log.e(TAG, "Failed to encode bytes as GZIP")
        return ByteArray(0)
    }
}
suspend fun getAuthToken(accountManager: AccountManager, account: Account, authTokenType: String) =
    suspendCoroutine<Bundle> { continuation ->
        accountManager.getAuthToken(account, authTokenType, false, { future: AccountManagerFuture<Bundle> ->
            try {
                val result = future.result
                continuation.resume(result)
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }, null)
    }
