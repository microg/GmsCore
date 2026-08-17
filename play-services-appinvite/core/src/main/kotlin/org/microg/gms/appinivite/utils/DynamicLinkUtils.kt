/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.appinivite.utils

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.LocaleList
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Request.Method.POST
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonRequest
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import kotlinx.coroutines.CompletableDeferred
import okio.ByteString.Companion.decodeHex
import org.json.JSONException
import org.json.JSONObject
import org.microg.gms.appinvite.ClientIdInfo
import org.microg.gms.appinvite.ClientPlatform
import org.microg.gms.appinvite.LinkInfo
import org.microg.gms.appinvite.MutateAppInviteLinkRequest
import org.microg.gms.appinvite.MutateAppInviteLinkResponse
import org.microg.gms.appinvite.MutateDataRequest
import org.microg.gms.appinvite.MutateDataResponseWithError
import org.microg.gms.appinvite.MutateOperation
import org.microg.gms.appinvite.MutateOperationId
import org.microg.gms.appinvite.SystemInfo
import org.microg.gms.common.Constants
import org.microg.gms.utils.digest
import org.microg.gms.utils.getCertificates
import org.microg.gms.utils.toHexString
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.HashMap
import java.util.Locale
import kotlin.collections.firstOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object DynamicLinkUtils {

    suspend fun requestLinkResponse(linkUrl: String, queue: RequestQueue): MutateAppInviteLinkResponse? {
        val request = ProtobufPostRequest(
            "https://datamixer-pa.googleapis.com/v1/mutateonekey?alt=proto&key=AIzaSyAP-gfH3qvi6vgHZbSYwQ_XHqV_mXHhzIk", MutateOperation(
                id = MutateOperationId.AppInviteLink, mutateRequest = MutateDataRequest(
                    appInviteLink = MutateAppInviteLinkRequest(
                        client = ClientIdInfo(
                            platform = ClientPlatform.Android,
                            packageName = Constants.GMS_PACKAGE_NAME,
                            signature = Constants.GMS_PACKAGE_SIGNATURE_SHA1.decodeHex().base64(),
                            language = Locale.getDefault().language
                        ), link = LinkInfo(
                            invitationId = "", uri = linkUrl
                        ), system = SystemInfo(
                            gms = SystemInfo.GmsInfo(
                                versionCode = Constants.GMS_VERSION_CODE
                            )
                        )
                    )
                )
            ), MutateDataResponseWithError.ADAPTER
        )
        val response = try {
            request.sendAndAwait(queue)
        } catch (e: Exception) {
            return null
        }
        if (response.errorStatus != null || response.dataResponse?.appInviteLink == null) return null
        return response.dataResponse?.appInviteLink
    }

    suspend fun requestShortLinks(context: Context, packageName: String, apiKey: String, longDynamicLink: String, queue: RequestQueue) = suspendCoroutine<JSONObject> { con ->
        queue.add(object : JsonRequest<JSONObject>(POST, "https://firebasedynamiclinks.googleapis.com/v1/shortLinks?key=$apiKey", JSONObject().apply {
            put("longDynamicLink", longDynamicLink)
        }.toString(), {
            con.resume(it)
        }, {
            con.resumeWithException(RuntimeException(it))
        }) {
            override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject> {
                return try {
                    val jsonString = String(response.data, Charset.forName(HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET)))
                    Response.success(JSONObject(jsonString), null)
                } catch (e: UnsupportedEncodingException) {
                    Response.error(ParseError(e))
                } catch (je: JSONException) {
                    Response.error(ParseError(je))
                }
            }

            override fun getHeaders(): Map<String, String?> = mapOf(
                "X-Android-Package" to packageName,
                "X-Android-Cert" to context.packageManager.getCertificates(packageName).firstOrNull()?.digest("SHA1")?.toHexString()?.uppercase()
            )
        })
    }
}

internal class ProtobufPostRequest<I : Message<I, *>, O>(url: String, private val i: I, private val oAdapter: ProtoAdapter<O>) : Request<O>(Method.POST, url, null) {
    private val deferred = CompletableDeferred<O>()

    override fun getHeaders(): Map<String, String> {
        val headers = HashMap(super.getHeaders())
        headers["Accept-Language"] = if (SDK_INT >= 24) LocaleList.getDefault().toLanguageTags() else Locale.getDefault().language
        headers["X-Android-Package"] = Constants.GMS_PACKAGE_NAME
        headers["X-Android-Cert"] = Constants.GMS_PACKAGE_SIGNATURE_SHA1
        return headers
    }

    override fun getBody(): ByteArray = i.encode()

    override fun getBodyContentType(): String = "application/x-protobuf"

    override fun parseNetworkResponse(response: NetworkResponse): Response<O> {
        return try {
            Response.success(oAdapter.decode(response.data), null)
        } catch (e: VolleyError) {
            Response.error(e)
        } catch (e: Exception) {
            Response.error(VolleyError())
        }
    }

    override fun deliverResponse(response: O) {
        deferred.complete(response)
    }

    override fun deliverError(error: VolleyError) {
        deferred.completeExceptionally(error)
    }

    suspend fun await(): O = deferred.await()

    suspend fun sendAndAwait(queue: RequestQueue): O {
        queue.add(this)
        return await()
    }
}