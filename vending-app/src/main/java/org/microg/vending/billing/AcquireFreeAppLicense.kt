package org.microg.vending.billing

import android.accounts.Account
import android.content.Context
import android.util.Log
import io.ktor.client.plugins.ClientRequestException
import io.ktor.utils.io.errors.IOException
import org.microg.gms.common.DeviceConfiguration
import org.microg.gms.common.asProto
import org.microg.vending.UploadDeviceConfigRequest
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_DETAILS
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_PURCHASE
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_UPLOAD_DEVICE_CONFIG
import org.microg.vending.billing.core.HeaderProvider
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.billing.proto.BuyResponse
import org.microg.vending.billing.proto.GoogleApiResponse

suspend fun HttpClient.acquireFreeAppLicense(context: Context, account: Account, packageName: String): Boolean {
    val authData = AuthManager.getAuthData(context, account)

    val deviceInfo = createDeviceEnvInfo(context)
    if (deviceInfo == null || authData == null) {
        Log.e(TAG, "Unable to auto-purchase $packageName when deviceInfo = $deviceInfo and authData = $authData")
        return false
    }

    var headers = HeaderProvider.getDefaultHeaders(authData, deviceInfo)

    // Check if app is free
    val detailsResult = try {
        get(
            url = URL_DETAILS,
            headers = headers,
            params = mapOf("doc" to packageName),
            adapter = GoogleApiResponse.ADAPTER
        ).payload?.detailsResponse
    } catch (e: IOException) {
        Log.e(TAG, "Unable to auto-purchase $packageName because of a network error or unexpected response when gathering app data", e)
        return false
    }

    val item = detailsResult?.item
    val appDetails = item?.details?.appDetails
    val versionCode = appDetails?.versionCode
    if (detailsResult == null || versionCode == null || appDetails.packageName != packageName) {
        Log.e(TAG, "Unable to auto-purchase $packageName because the server did not send sufficient or matching details")
        return false
    }

    val offer = item.offer
    if (offer == null) {
        Log.e(TAG, "Unable to auto-purchase $packageName because the app is not being offered at the store")
    }

    val freeApp = detailsResult.item.offer?.micros == 0L
    if (!freeApp) {
        Log.e(TAG, "Unable to auto-purchase $packageName because it is not a free app")
        return false
    }

    // Purchase app
    val parameters = mapOf(
        "ot" to (offer?.offerType ?: 1).toString(),
        "doc" to packageName,
        "vc" to versionCode.toString()
    )

    var buyResult : BuyResponse?
    try {
        buyResult = purchase(headers, parameters)
    } catch (e: Exception) {
        Log.w(TAG, "acquireFreeAppLicense: purchase failed!", e)
        if (e is ClientRequestException && e.response.status.value == 400) {
            val deviceConfigResultToken = runCatching { uploadDeviceConfig(context, headers) }.getOrNull()
            if (deviceConfigResultToken == null) {
                Log.e(TAG, "Unable to auto-purchase $packageName because of a network error or unexpected response during device config upload")
                return false
            }
            deviceConfigResultToken.let {
                authData.deviceConfigToken = it
                headers = HeaderProvider.getDefaultHeaders(authData, deviceInfo)
            }
            buyResult = try {
                purchase(headers, parameters)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to auto-purchase $packageName because of a network error or unexpected response during purchase", e)
                return false
            }
        } else {
            Log.e(TAG, "Unable to auto-purchase $packageName because of a network error or unexpected response during purchase", e)
            return false
        }
    }

    if (buyResult?.deliveryToken.isNullOrBlank()) {
        Log.e(TAG, "Auto-purchasing $packageName failed. Was the purchase rejected by the server?")
        return false
    } else {
        Log.i(TAG, "Auto-purchased $packageName.")
    }

    return true
}

private suspend fun HttpClient.purchase(headers: Map<String, String> = emptyMap(), parameters: Map<String, String> = emptyMap()) = post(
    url = URL_PURCHASE,
    headers = headers,
    params = parameters,
    adapter = GoogleApiResponse.ADAPTER
).payload?.buyResponse

private suspend fun HttpClient.uploadDeviceConfig(context: Context, headers: Map<String, String> = emptyMap()) = post(
    url = URL_UPLOAD_DEVICE_CONFIG,
    headers = headers,
    payload = UploadDeviceConfigRequest(DeviceConfiguration(context).asProto()),
    adapter = GoogleApiResponse.ADAPTER
).payload?.uploadDeviceConfigResponse?.deviceConfigToken