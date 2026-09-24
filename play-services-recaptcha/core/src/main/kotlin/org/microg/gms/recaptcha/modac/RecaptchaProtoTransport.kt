/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha.modac

import android.content.Context
import android.os.LocaleList
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.Volley
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.microg.gms.profile.Build
import org.microg.gms.utils.singleInstanceOf

private const val PROTO_CONTENT_TYPE = "application/x-protobuffer"
private const val MAX_PROTO_RESPONSE_BYTES = 1024 * 1024

internal data class RecaptchaProtoResponse(val statusCode: Int, val body: ByteArray)

internal class RecaptchaTransportException(
    val statusCode: Int?,
    cause: Throwable,
) : Exception("reCAPTCHA transport failed${statusCode?.let { " with HTTP $it" }.orEmpty()}", cause)

internal interface RecaptchaProtoTransport {
    suspend fun post(endpoint: String, body: ByteArray, timeoutMs: Long): RecaptchaProtoResponse
}

internal class VolleyRecaptchaProtoTransport(context: Context) : RecaptchaProtoTransport {
    private val queue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }

    override suspend fun post(
        endpoint: String,
        body: ByteArray,
        timeoutMs: Long,
    ): RecaptchaProtoResponse = suspendCancellableCoroutine { continuation ->
        val request = RawProtoRequest(
            endpoint = endpoint,
            requestBody = body,
            onSuccess = { response ->
                if (continuation.isActive) continuation.resume(response)
            },
            onError = { error ->
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        RecaptchaTransportException(error.networkResponse?.statusCode, error),
                    )
                }
            },
        ).apply {
            retryPolicy = DefaultRetryPolicy(
                timeoutMs.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt(),
                0,
                1.0f,
            )
            setShouldCache(false)
        }
        continuation.invokeOnCancellation { request.cancel() }
        try {
            queue.add(request)
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }
}

private class RawProtoRequest(
    endpoint: String,
    private val requestBody: ByteArray,
    private val onSuccess: (RecaptchaProtoResponse) -> Unit,
    onError: (VolleyError) -> Unit,
) : Request<RecaptchaProtoResponse>(Method.POST, endpoint, onError) {

    override fun getHeaders(): Map<String, String> = mapOf(
        "Accept" to PROTO_CONTENT_TYPE,
        "Accept-Language" to if (Build.VERSION.SDK_INT >= 24) {
            LocaleList.getDefault().toLanguageTags()
        } else {
            Locale.getDefault().language
        },
    )

    override fun getBody(): ByteArray = requestBody

    override fun getBodyContentType(): String = PROTO_CONTENT_TYPE

    override fun parseNetworkResponse(response: NetworkResponse): Response<RecaptchaProtoResponse> {
        if (response.data.size > MAX_PROTO_RESPONSE_BYTES) {
            return Response.error(ParseError(IllegalStateException("reCAPTCHA response exceeds size limit")))
        }
        return Response.success(
            RecaptchaProtoResponse(response.statusCode, response.data),
            HttpHeaderParser.parseCacheHeaders(response),
        )
    }

    override fun deliverResponse(response: RecaptchaProtoResponse) {
        onSuccess(response)
    }
}
