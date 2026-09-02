/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.core.os.bundleOf
import com.android.billingclient.api.BillingClient.BillingResponseCode
import org.microg.gms.deviceinfo.DeviceEnvInfo
import org.microg.gms.profile.Build
import org.microg.gms.utils.digest
import org.microg.gms.utils.getExtendedPackageInfo
import org.microg.gms.utils.toBase64
import org.microg.vending.billing.core.ClientInfo

fun Map<String, Any?>.toBundle(): Bundle = bundleOf(*this.toList().toTypedArray())

/**
 * Returns true if the receiving collection contains any of the specified elements.
 *
 * @param elements the elements to look for in the receiving collection.
 * @return true if any element in [elements] is found in the receiving collection.
 */
fun <T> Collection<T>.containsAny(vararg elements: T): Boolean {
    return containsAny(elements.toSet())
}

/**
 * Returns true if the receiving collection contains any of the elements in the specified collection.
 *
 * @param elements the elements to look for in the receiving collection.
 * @return true if any element in [elements] is found in the receiving collection.
 */
fun <T> Collection<T>.containsAny(elements: Collection<T>): Boolean {
    val set = if (elements is Set) elements else elements.toSet()
    return any(set::contains)
}

fun String.decodeHex(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }
    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

fun resultBundle(@BillingResponseCode code: Int, msg: String?, data: Bundle = Bundle.EMPTY): Bundle {
    val res = bundleOf(
        "RESPONSE_CODE" to code,
        "DEBUG_MESSAGE" to msg
    )
    res.putAll(data)
    Log.d(TAG, "Result: $res")
    return res
}

fun getGoogleAccount(context: Context, name: String? = null): Account? {
    var accounts =
        AccountManager.get(context).getAccountsByType(DEFAULT_ACCOUNT_TYPE).toList()
    name?.let { accounts = accounts.filter { it.name == name } }
    if (accounts.isEmpty())
        return null
    return accounts[0]
}

fun createClient(context: Context, pkgName: String): ClientInfo? {
    return try {
        val packageInfo = context.packageManager.getExtendedPackageInfo(pkgName)
        ClientInfo(
            pkgName,
            packageInfo.certificates.firstOrNull()?.digest("MD5")?.toBase64(Base64.URL_SAFE + Base64.NO_WRAP + Base64.NO_PADDING) ?: "",
            packageInfo.shortVersionCode
        )
    } catch (e: Exception) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "createClient", e)
        null
    }
}

fun bundleToMap(bundle: Bundle?): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    if (bundle == null)
        return result
    for (key in bundle.keySet()) {
        bundle.get(key)?.let {
            result[key] = it
        }
    }
    return result
}

fun getUserAgent(): String {
    return "Android-Finsky/${Uri.encode(VENDING_VERSION_NAME)} (api=3,versionCode=$VENDING_VERSION_CODE,sdk=${Build.VERSION.SDK_INT},device=${Build.DEVICE},hardware=${Build.HARDWARE},product=${Build.PRODUCT},platformVersionRelease=${Build.VERSION.RELEASE},model=${Uri.encode(Build.MODEL)},buildId=${Build.ID},isWideScreen=0,supportedAbis=${Build.SUPPORTED_ABIS.joinToString(";")})"
}

fun createDeviceEnvInfo(context: Context): DeviceEnvInfo? =
    org.microg.gms.deviceinfo.createDeviceEnvInfo(
        context,
        gpVersionCode = VENDING_VERSION_CODE,
        gpVersionName = VENDING_VERSION_NAME,
        gpPkgName = VENDING_PACKAGE_NAME,
        userAgent = getUserAgent(),
    )