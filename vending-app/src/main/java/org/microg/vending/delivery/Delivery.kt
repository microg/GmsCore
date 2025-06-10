/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.delivery

import android.accounts.AccountManager
import android.content.Context
import android.util.Log
import com.android.vending.buildRequestHeaders
import com.google.android.finsky.BulkGrant
import com.google.android.finsky.BulkRequest
import com.google.android.finsky.BulkRequestWrapper
import com.google.android.finsky.BulkResponseWrapper
import com.google.android.finsky.DeviceSyncInfo
import com.google.android.finsky.SyncResponse
import com.google.android.finsky.splitinstallservice.PackageComponent
import com.google.android.finsky.syncDeviceInfo
import org.microg.vending.billing.core.AuthData
import org.microg.vending.billing.core.GooglePlayApi
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_DELIVERY
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.billing.proto.GoogleApiResponse
import org.microg.vending.splitinstall.SPLIT_LANGUAGE_TAG

private const val TAG = "GmsVendingDelivery"

/**
 * Call the FDFE delivery endpoint to retrieve download URLs for the
 * desired components. If specific split install packages are requested,
 * only those will be contained in the result.
 */
suspend fun HttpClient.requestDownloadUrls(
        context: Context,
        packageName: String,
        versionCode: Long,
        auth: AuthData,
        requestSplitPackages: List<String>? = null,
        deliveryToken: String? = null,
): List<PackageComponent> {

    val requestUrl = StringBuilder("$URL_DELIVERY?doc=$packageName&ot=1&vc=$versionCode")

    requestSplitPackages?.apply {
        requestUrl.append(
            "&bvc=$versionCode&pf=1&pf=2&pf=3&pf=4&pf=5&pf=7&pf=8&pf=9&pf=10&da=4&bda=4&bf=4&fdcf=1&fdcf=2&ch="
        )
        forEach { requestUrl.append("&mn=").append(it) }
    }

    deliveryToken?.let {
        requestUrl.append("&dtok=$it")
    }

    Log.v(TAG, "requestDownloadUrls start")
    val languages = requestSplitPackages?.filter { it.startsWith(SPLIT_LANGUAGE_TAG) }?.map {
        it.replace(SPLIT_LANGUAGE_TAG, "")
    }
    Log.d(TAG, "requestDownloadUrls languages: $languages")

    val androidId = auth.gsfId.toLong(16)
    val headers = buildRequestHeaders(
        auth = auth.authToken,
        androidId = androidId,
        languages
    ).minus(
        // TODO: understand behavior. According to tests, these headers break split install queries but may be needed for normal ones
        (if (requestSplitPackages != null) listOf("X-DFE-Encoded-Targets", "X-DFE-Phenotype", "X-DFE-Device-Id", "X-DFE-Client-Id") else emptyList()).toSet()
    )

    kotlin.runCatching {
        //Synchronize account device information to prevent failure to obtain sub-package download information
        syncDeviceInfo(context, AccountManager.get(context).accounts.firstOrNull { it.name == auth.email }!!, auth.authToken, androidId)
    }
    kotlin.runCatching {
        //Authorize the account to prevent the inability to obtain split information
        post(
                url = GooglePlayApi.URL_BULK,
                headers = headers,
                payload = BulkRequestWrapper.build {
                    request(BulkRequest.build {
                        packageName(packageName)
                        grant(BulkGrant.build { grantLevel = 1 })
                    })
                },
                adapter = BulkResponseWrapper.ADAPTER
        )
    }

    val response = get(
        url = requestUrl.toString(),
        headers = headers,
        adapter = GoogleApiResponse.ADAPTER
    )
    Log.d(TAG, "requestDownloadUrls end response -> $response")

    val basePackage = response.payload!!.deliveryResponse!!.deliveryData?.let {
        if (it.baseUrl != null && it.baseBytes != null) {
            PackageComponent(packageName, "base", it.baseUrl, it.baseBytes.toLong())
        } else null
    }
    val splitComponents = response.payload.deliveryResponse!!.deliveryData!!.splitPackages.filter {
        !it.splitPackageName.isNullOrEmpty() && !it.downloadUrl.isNullOrEmpty()
    }.map {
        if (requestSplitPackages != null) {
            // Only download requested, if specific components were requested
            requestSplitPackages.firstOrNull { requestComponent ->
                (it.splitPackageName?.contains(requestComponent) ?: false || requestComponent.contains(it.splitPackageName!!))
            }?.let { requestComponent ->
                PackageComponent(packageName, it.splitPackageName!!, it.downloadUrl!!, it.size!!.toLong())
            }
        } else {
            // Download all offered components (server chooses)
            PackageComponent(packageName, it.splitPackageName!!, it.downloadUrl!!, it.size!!.toLong())
        }
    }

    val components = if (requestSplitPackages != null) {
        splitComponents
    } else {
        listOf(basePackage) + splitComponents
    }.filterNotNull()

    Log.d(TAG, "requestDownloadUrls end -> $components")

    return components
}