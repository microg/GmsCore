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
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.firebase.dynamiclinks.internal.DynamicLinkData
import com.google.firebase.dynamiclinks.internal.IDynamicLinksCallbacks
import com.google.firebase.dynamiclinks.internal.IDynamicLinksService
import com.google.firebase.dynamiclinks.internal.ShortDynamicLinkImpl
import com.google.firebase.dynamiclinks.internal.WarningImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.fido.core.map
import org.microg.gms.appinivite.utils.DynamicLinkUtils
import org.microg.gms.utils.singleInstanceOf
import org.microg.gms.utils.warnOnTransactionIssues
import java.net.URLEncoder

private const val TAG = "DynamicLinksService"

class DynamicLinksService : BaseService(TAG, GmsService.DYNAMIC_LINKS) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val callingPackage = PackageUtils.getAndCheckCallingPackage(this, request.packageName) ?: throw IllegalArgumentException("Missing package name")
        callback.onPostInitComplete(0, DynamicLinksServiceImpl(this, callingPackage, lifecycle, request.extras), null)
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
                    val response = runCatching { withContext(Dispatchers.IO) { DynamicLinkUtils.requestLinkResponse(link, queue) } }.getOrNull()
                    val data = if (response == null) {
                        DynamicLinkData(null, link, 0, 0, null, null)
                    } else {
                        DynamicLinkData(
                            response.metadata?.info?.url, response.data_?.intentData, (response.data_?.app?.minAppVersion ?: 0).toInt(), System.currentTimeMillis(), null, null
                        )
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
        val domainUriPrefix = extras.getString("domainUriPrefix")
        val parameters = extras.getBundle("parameters")
        var longDynamicLink: String? = null
        if (!domainUriPrefix.isNullOrEmpty() && parameters != null) {
            val params = parameters.keySet().mapNotNull { key ->
                parameters[key]?.toString()?.let { value ->
                    val encodedValue = URLEncoder.encode(value, "UTF-8").replace("%21", "!").replace("+", "%20")
                    "$key=$encodedValue"
                }
            }.joinToString("&")
            longDynamicLink = "$domainUriPrefix?$params"
        }
        val apikey = extras.getString("apiKey")
        if (apikey != null && longDynamicLink != null) {
            lifecycleScope.launchWhenCreated {
                val jsonResult = withContext(Dispatchers.IO) {
                    runCatching {
                        DynamicLinkUtils.requestShortLinks(context, callingPackageName, apikey, longDynamicLink, queue)
                    }.onFailure {
                        Log.d(TAG, "createShortDynamicLink: ", it)
                    }.getOrNull()
                }
                if (jsonResult == null) {
                    callback.onStatusShortDynamicLink(Status.SUCCESS, ShortDynamicLinkImpl())
                    return@launchWhenCreated
                }
                val shortLink = jsonResult.optString("shortLink")
                val previewLink = jsonResult.optString("previewLink")
                val warningList = jsonResult.optJSONArray("warning")?.map {
                    val warningMessage = getJSONObject(it).optString("warningMessage")
                    WarningImpl(warningMessage)
                } ?: emptyList()
                callback.onStatusShortDynamicLink(Status.SUCCESS, ShortDynamicLinkImpl(shortLink.let { Uri.parse(it) }, previewLink.let { Uri.parse(it) }, warningList))
            }
            return
        }
        callback.onStatusShortDynamicLink(Status.SUCCESS, ShortDynamicLinkImpl())
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int) = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
