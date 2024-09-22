package org.microg.vending.delivery

import android.util.Log
import com.android.vending.buildRequestHeaders
import com.google.android.finsky.GoogleApiResponse
import com.google.android.finsky.splitinstallservice.PackageComponent
import org.microg.vending.billing.core.AuthData
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_DELIVERY
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.splitinstall.SPLIT_LANGUAGE_TAG

private const val TAG = "GmsVendingDelivery"

/**
 * Call the FDFE delivery endpoint to retrieve download URLs for the
 * desired components. If specific split install packages are requested,
 * only those will be contained in the result.
 */
suspend fun HttpClient.requestDownloadUrls(
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

    val response = get(
        url = requestUrl.toString(),
        headers = buildRequestHeaders(auth.authToken, auth.gsfId.toLong(16), languages),
        adapter = GoogleApiResponse.ADAPTER
    )
    Log.d(TAG, "requestDownloadUrls end response -> $response")

    val basePackage = response.response!!.splitReqResult!!.pkgList?.let {
        if (it.baseUrl != null && it.baseBytes != null) {
            PackageComponent(packageName, "base", it.baseUrl, it.baseBytes)
        } else null
    }
    val splitComponents = response.response.splitReqResult!!.pkgList!!.pkgDownLoadInfo.filter {
        !it.splitPkgName.isNullOrEmpty() && !it.downloadUrl.isNullOrEmpty()
    }.map {
        if (requestSplitPackages != null) {
            // Only download requested, if specific components were requested
            requestSplitPackages.firstOrNull { requestComponent ->
                requestComponent.contains(it.splitPkgName!!)
            }?.let { requestComponent ->
                PackageComponent(packageName, requestComponent, it.downloadUrl!!, it.size!!)
            }
        } else {
            // Download all offered components (server chooses)
            PackageComponent(packageName, it.splitPkgName!!, it.downloadUrl!!, it.size!!)
        }
    }

    val components = (listOf(basePackage) + splitComponents).filterNotNull()

    Log.d(TAG, "requestDownloadUrls end -> $components")

    return components
}