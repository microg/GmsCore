/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.firebase.dynamiclinks

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.firebase.dynamiclinks.internal.DynamicLinkData
import com.google.firebase.dynamiclinks.internal.IDynamicLinksCallbacks
import com.google.firebase.dynamiclinks.internal.IDynamicLinksService
import com.google.firebase.dynamiclinks.internal.ShortDynamicLinkImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.BaseService
import org.microg.gms.common.Constants
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.profile.Build
import org.microg.gms.utils.singleInstanceOf
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "DynamicLinksService"

private const val DYNAMIC_LINK_URL = "https://datamixer-pa.googleapis.com/v1/mutateonekey?alt=proto&key=AIzaSyAP-gfH3qvi6vgHZbSYwQ_XHqV_mXHhzIk"

class DynamicLinksService : BaseService(TAG, GmsService.DYNAMIC_LINKS) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        callback.onPostInitComplete(0, DynamicLinksServiceImpl(this, request.packageName, lifecycle, request.extras), null)
    }

}

class DynamicLinksServiceImpl(private val context: Context, private val callingPackageName: String, override val lifecycle: Lifecycle, extras: Bundle?) : IDynamicLinksService.Stub(), LifecycleOwner {

    private val queue = singleInstanceOf { Volley.newRequestQueue(context) }

    override fun getDynamicLink(callback: IDynamicLinksCallbacks, link: String?) {
        Log.d(TAG, "getDynamicLink: callingPackageName: $callingPackageName link: $link")
        if (link != null) {
            val linkUri = Uri.parse(link)
            if ("http" == linkUri.scheme || "https" == linkUri.scheme) {
                lifecycleScope.launchWhenCreated {
                    val response = runCatching { withContext(Dispatchers.IO) { requestDynamicLink(link) } }.getOrNull()
                    val shareLink = response?.response?.body?.shareLinkInfo?.shareLink
                    val data = if (shareLink == null) {
                        Log.d(TAG, "getShareLink is null")
                        DynamicLinkData(null, link, 0, 0, null, null)
                    } else {
                        DynamicLinkData(null, shareLink, 0, 0, null, null)
                    }
                    Log.d(TAG, "getDynamicLink: $link -> $data")
                    callback.onStatusDynamicLinkData(Status.SUCCESS, data)
                }
                return
            }
            val packageName = linkUri.getQueryParameter("apn")
            val amvParameter = linkUri.getQueryParameter("amv")
            if (packageName == null) {
                throw RuntimeException("Missing package name")
            } else if (callingPackageName != packageName) {
                throw RuntimeException("Registered package name:$callingPackageName does not match link package name: $packageName")
            }
            var amv = 0
            if (amvParameter != null && amvParameter !== "") {
                amv = amvParameter.toInt()
            }
            val data = DynamicLinkData(
                null, linkUri.getQueryParameter("link"), amv, 0, null, null
            )
            Log.d(TAG, "getDynamicLink: $link -> $data")
            callback.onStatusDynamicLinkData(Status.SUCCESS, data)
        } else {
            Log.d(TAG, "getDynamicLink: " + null + " -> " + null)
            callback.onStatusDynamicLinkData(Status.SUCCESS, null)
        }
    }

    override fun createShortDynamicLink(callback: IDynamicLinksCallbacks, extras: Bundle) {
        extras.keySet() // Unparcel
        Log.d(TAG, "createShortDynamicLink: $extras")
        callback.onStatusShortDynamicLink(Status.SUCCESS, ShortDynamicLinkImpl())
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (super.onTransact(code, data, reply, flags)) {
            return true
        }
        Log.d(TAG, "onTransact [unknown]: $code, $data, $flags")
        return false
    }

    private suspend fun requestDynamicLink(linkUrl: String) = suspendCoroutine { con ->
        queue.add(object : Request<DynamicLickResponse?>(Method.POST, DYNAMIC_LINK_URL, { con.resumeWithException(it) }) {
            override fun deliverResponse(response: DynamicLickResponse?) {
                Log.d(TAG, "requestDynamicLink response: $response")
                con.resume(response)
            }

            override fun getHeaders(): Map<String, String> {
                return hashMapOf<String, String>().apply {
                    put("X-Android-Package", Constants.GMS_PACKAGE_NAME)
                    put("X-Android-Cert", Constants.GMS_PACKAGE_SIGNATURE_SHA1)
                    put("User-Agent", "GmsCore/${Constants.GMS_VERSION_CODE} (${Build.DEVICE} ${Build.ID}); gzip")
                }
            }

            override fun getBody(): ByteArray {
                val requestBuilder = DynamicLinkRequest.Builder()
                requestBuilder.messageId(84453462)
                requestBuilder.wrapper(
                    LinkRequestWrapper.Builder().apply {
                        body(
                            LinkRequestBody.Builder().apply {
                                linkInfo(LinkInfo.Builder().apply {
                                    linkUrl(linkUrl)
                                }.build())
                            }.build()
                        )
                    }.build()
                )
                return requestBuilder.build().encode()
            }

            override fun getBodyContentType(): String = "application/x-protobuf"

            override fun parseNetworkResponse(response: NetworkResponse): Response<DynamicLickResponse?> {
                return try {
                    Response.success(DynamicLickResponse.ADAPTER.decode(response.data), null)
                } catch (e: Exception) {
                    Response.error(VolleyError(e))
                }
            }
        })
    }

}
