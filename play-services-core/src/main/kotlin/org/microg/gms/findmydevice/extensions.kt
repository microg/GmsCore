/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.findmydevice

import androidx.annotation.RequiresApi
import com.google.android.gms.common.Feature
import okhttp3.Interceptor
import okhttp3.Response
import org.microg.gms.common.Constants
import org.microg.gms.profile.Build
import java.util.Locale
import kotlin.text.ifEmpty

const val TAG = "GmsFindDevice"

const val FIND_DEVICE_REMOTE_POLICY = "REMOTE_POLICY"

const val GMS_FMD_OAUTH_SERVICE = "oauth2:https://www.googleapis.com/auth/android_device_manager"
const val FMD_BASE_URL = "https://findmydevice-pa.googleapis.com"
const val FMD_API_KEY = "AIzaSyAP-gfH3qvi6vgHZbSYwQ_XHqV_mXHhzIk"

val FEATURES = arrayOf(
    Feature("SPOT_MANAGEMENT", 10L),
    Feature("SPOT_MANAGEMENT_CACHED_DEVICES", 1L),
    Feature("SPOT_FAST_PAIR", 5L),
    Feature("SPOT_FAST_PAIR_HISTORICAL_ACCOUNT_KEYS", 1L),
    Feature("SPOT_LOCATION_REPORT", 10L),
    Feature("SPOT_LOCATION_REPORT_DISABLE", 1L)
)

class RemotePayloadInterceptor() : Interceptor {
    @RequiresApi(21)
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
            .header("user-agent", buildUserAgent())
            .header("x-goog-api-key", FMD_API_KEY)
            .header("x-android-package", Constants.GMS_PACKAGE_NAME)
            .header("x-android-cert", Constants.GMS_PACKAGE_SIGNATURE_SHA1)
        return chain.proceed(requestBuilder.build())
    }
}

class ProcessSitrepInterceptor(
    private val authToken: String,
    private val authTime: Long = System.currentTimeMillis()
) : Interceptor {
    @RequiresApi(21)
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request().newBuilder()
            .header("user-agent", buildUserAgent())
            .header("authorization", "Bearer $authToken")
            .header("x-auth-time", authTime.toString())
        return chain.proceed(original.build())
    }
}

@RequiresApi(21)
private fun buildUserAgent(): String {
    val locale = Locale.getDefault()
    val localeStr = "${locale.language}_${locale.country}_#${locale.script.ifEmpty { "Hans" }}"
    return "${Constants.GMS_PACKAGE_NAME}/${Constants.GMS_VERSION_CODE} " +
            "(Linux; U; Android ${Build.VERSION.RELEASE}; $localeStr; ${Build.MODEL}; " +
            "Build/${Build.ID}; Cronet/140.0.7289.0) grpc-java-cronet/1.76.0-SNAPSHOT"
}