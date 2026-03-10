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
import okio.ByteString
import org.microg.gms.profile.Build
import org.microg.vending.billing.getUserAgent
import org.microg.vending.proto.AndroidVersionMeta
import org.microg.vending.proto.DeviceMeta
import org.microg.vending.proto.EncodedTriple
import org.microg.vending.proto.EncodedTripleWrapper
import org.microg.vending.proto.IntWrapper
import org.microg.vending.proto.Locality
import org.microg.vending.proto.LocalityWrapper
import org.microg.vending.proto.RequestHeader
import org.microg.vending.proto.RequestLanguagePackage
import org.microg.vending.proto.StringWrapper
import org.microg.vending.proto.Timestamp
import org.microg.vending.proto.TimestampContainer
import org.microg.vending.proto.TimestampContainer1
import org.microg.vending.proto.TimestampContainer1Wrapper
import org.microg.vending.proto.TimestampContainer2
import org.microg.vending.proto.TimestampStringWrapper
import org.microg.vending.proto.TimestampWrapper
import org.microg.vending.proto.UnknownByte12
import org.microg.vending.proto.UserAgent
import org.microg.vending.proto.Uuid
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID
import java.util.zip.GZIPOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "VendingRequestHeaders"

const val AUTH_TOKEN_SCOPE: String = "oauth2:https://www.googleapis.com/auth/googleplay"

private const val BASE64_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
private const val FINSKY_VERSION = "Finsky/37.5.24-29%20%5B0%5D%20%5BPR%5D%20565477504"

fun buildRequestHeaders(auth: String, androidId: Long, language: List<String>? = null): Map<String, String> {
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
    val userAgent = getUserAgent()

    return mapOf(
        "X-PS-RH" to xPsRh,
        "User-Agent" to userAgent,
        "Accept-Language" to "en-US",
        "Connection" to "Keep-Alive",
        "X-DFE-Device-Id" to androidId.toBigInteger().toString(16),
        "X-DFE-Client-Id" to "am-google",
        "X-DFE-Encoded-Targets" to "CAESN/qigQYC2AMBFfUbyA7SM5Ij/CvfBoIDgxHqGP8R3xzIBvoQtBKFDZ4HAY4FrwSVMasHBO0O2Q8akgYRAQECAQO7AQEpKZ0CnwECAwRrAQYBr9PPAoK7sQMBAQMCBAkIDAgBAwEDBAICBAUZEgMEBAMLAQEBBQEBAcYBARYED+cBfS8CHQEKkAEMMxcBIQoUDwYHIjd3DQ4MFk0JWGYZEREYAQOLAYEBFDMIEYMBAgICAgICOxkCD18LGQKEAcgDBIQBAgGLARkYCy8oBTJlBCUocxQn0QUBDkkGxgNZQq0BZSbeAmIDgAEBOgGtAaMCDAOQAZ4BBIEBKUtQUYYBQscDDxPSARA1oAEHAWmnAsMB2wFyywGLAxol+wImlwOOA80CtwN26A0WjwJVbQEJPAH+BRDeAfkHK/ABASEBCSAaHQemAzkaRiu2Ad8BdXeiAwEBGBUBBN4LEIABK4gB2AFLfwECAdoENq0CkQGMBsIBiQEtiwGgA1zyAUQ4uwS8AwhsvgPyAcEDF27vApsBHaICGhl3GSKxAR8MC6cBAgItmQYG9QIeywLvAeYBDArLAh8HASI4ELICDVmVBgsY/gHWARtcAsMBpALiAdsBA7QBpAJmIArpByn0AyAKBwHTARIHAX8D+AMBcRIBBbEDmwUBMacCHAciNp0BAQF0OgQLJDuSAh54kwFSP0eeAQQ4M5EBQgMEmwFXywFo0gFyWwMcapQBBugBPUW2AVgBKmy3AR6PAbMBGQxrUJECvQR+8gFoWDsYgQNwRSczBRXQAgtRswEW0ALMAREYAUEBIG6yATYCRE8OxgER8gMBvQEDRkwLc8MBTwHZAUOnAXiiBakDIbYBNNcCIUmuArIBSakBrgFHKs0EgwV/G3AD0wE6LgECtQJ4xQFwFbUCjQPkBS6vAQqEAUZF3QIM9wEhCoYCQhXsBCyZArQDugIziALWAdIBlQHwBdUErQE6qQaSA4EEIvYBHir9AQVLmgMCApsCKAwHuwgrENsBAjNYswEVmgIt7QJnN4wDEnta+wGfAcUBxgEtEFXQAQWdAUAeBcwBAQM7rAEJATJ0LENrdh73A6UBhAE+qwEeASxLZUMhDREuH0CGARbd7K0GlQo",
        "X-DFE-Phenotype" to "H4sIAAAAAAAAAB3OO3KjMAAA0KRNuWXukBkBQkAJ2MhgAZb5u2GCwQZbCH_EJ77QHmgvtDtbv-Z9_H63zXXU0NVPB1odlyGy7751Q3CitlPDvFd8lxhz3tpNmz7P92CFw73zdHU2Ie0Ad2kmR8lxhiErTFLt3RPGfJQHSDy7Clw10bg8kqf2owLokN4SecJTLoSwBnzQSd652_MOf2d1vKBNVedzg4ciPoLz2mQ8efGAgYeLou-l-PXn_7Sna1MfhHuySxt-4esulEDp8Sbq54CPPKjpANW-lkU2IZ0F92LBI-ukCKSptqeq1eXU96LD9nZfhKHdtjSWwJqUm_2r6pMHOxk01saVanmNopjX3YxQafC4iC6T55aRbC8nTI98AF_kItIQAJb5EQxnKTO7TZDWnr01HVPxelb9A2OWX6poidMWl16K54kcu_jhXw-JSBQkVcD_fPsLSZu6joIBAAA"
    ) + if (auth.isNotEmpty()) mapOf("Authorization" to "Bearer $auth") else emptyMap()
}

fun makeTimestamp(millis: Long): Timestamp {
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
