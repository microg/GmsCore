/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.icu.util.TimeZone
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import com.android.billingclient.api.BillingClient.BillingResponseCode
import org.microg.gms.profile.Build
import org.microg.gms.utils.digest
import org.microg.gms.utils.getExtendedPackageInfo
import org.microg.gms.utils.toBase64
import org.microg.vending.billing.core.*
import java.util.*
import kotlin.collections.Collection
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Set
import kotlin.collections.any
import kotlin.collections.filter
import kotlin.collections.firstOrNull
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.toByteArray
import kotlin.collections.toList
import kotlin.collections.toSet
import kotlin.collections.toTypedArray

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

@SuppressLint("MissingPermission")
fun getDeviceIdentifier(context: Context): String {
    // TODO: Improve dummy data
    val deviceId = DeviceIdentifier.meid /*try {
        (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)?.let {
            it.subscriberId ?: it.deviceId
        }
    } catch (e: Exception) {
        null
    }*/
    if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "getDeviceIdentifier deviceId: $deviceId")
    return deviceId.toByteArray(Charsets.UTF_8).digest("SHA-1").toBase64(Base64.URL_SAFE + Base64.NO_WRAP + Base64.NO_PADDING)
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

fun getDisplayInfo(context: Context): DisplayMetrics? {
    return try {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (windowManager != null) {
            val displayMetrics = android.util.DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            return DisplayMetrics(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                displayMetrics.xdpi,
                displayMetrics.ydpi,
                displayMetrics.densityDpi
            )
        }
        return DisplayMetrics(
            context.resources.displayMetrics.widthPixels,
            context.resources.displayMetrics.heightPixels,
            context.resources.displayMetrics.xdpi,
            context.resources.displayMetrics.ydpi,
            context.resources.displayMetrics.densityDpi
        )
    } catch (e: Exception) {
        null
    }
}

// TODO: Improve privacy
fun getBatteryLevel(context: Context): Int {
    var batteryLevel = -1;
    val intentFilter = IntentFilter("android.intent.action.BATTERY_CHANGED")
    context.registerReceiver(null, intentFilter)?.let {
        val level = it.getIntExtra("level", -1)
        val scale = it.getIntExtra("scale", -1)
        if (scale > 0) {
            batteryLevel = level * 100 / scale
        }
    }
    if (batteryLevel == -1 && SDK_INT >= 33) {
        context.registerReceiver(null, intentFilter, Context.RECEIVER_EXPORTED)?.let {
            val level = it.getIntExtra("level", -1)
            val scale = it.getIntExtra("scale", -1)
            if (scale > 0) {
                batteryLevel = level * 100 / scale
            }
        }
    }
    return batteryLevel
}

fun getTelephonyData(context: Context): TelephonyData? {
    // TODO: Dummy data
    return null /*try {
        context.getSystemService(Context.TELEPHONY_SERVICE)?.let {
            val telephonyManager = it as TelephonyManager
            return TelephonyData(
                telephonyManager.simOperatorName!!,
                DeviceIdentifier.meid,
                telephonyManager.networkOperator!!,
                telephonyManager.simOperator!!,
                telephonyManager.phoneType
            )
        }
    } catch (e: Exception) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "getTelephonyData", e)
        null
    }*/
}

fun hasPermissions(context: Context, permissions: List<String>): Boolean {
    for (permission in permissions) {
        if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
            return false
    }
    return true
}

@SuppressLint("MissingPermission")
fun getLocationData(context: Context): LocationData? {
    // TODO: Dummy data
    return null /*try {
        (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?)?.let { locationManager ->
            if (hasPermissions(
                    context,
                    listOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            ) {
                locationManager.getLastKnownLocation("network")?.let { location ->
                    return LocationData(
                        location.altitude,
                        location.latitude,
                        location.longitude,
                        location.accuracy,
                        location.time.toDouble()
                    )
                }
            } else {
                null
            }
        }
    } catch (e: Exception) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "getLocationData", e)
        null
    }*/
}

fun getNetworkData(context: Context): NetworkData {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    var linkDownstreamBandwidth: Long = 0
    var linkUpstreamBandwidth: Long = 0
    // TODO: Dummy data
    /*
    if (hasPermissions(context, listOf(Manifest.permission.ACCESS_NETWORK_STATE)) && SDK_INT >= 23) {
        connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)?.let {
            linkDownstreamBandwidth = (it.linkDownstreamBandwidthKbps * 1000 / 8).toLong()
            linkUpstreamBandwidth = (it.linkUpstreamBandwidthKbps * 1000 / 8).toLong()
        }
    }
     */
    val isActiveNetworkMetered = connectivityManager?.isActiveNetworkMetered ?: false
    val netAddressList = mutableListOf<String>()
    // TODO: Dummy data
    /*try {
        NetworkInterface.getNetworkInterfaces()?.let { enumeration ->
            while (true) {
                if (!enumeration.hasMoreElements()) {
                    break
                }
                val enumeration1 = enumeration.nextElement().inetAddresses
                while (enumeration1.hasMoreElements()) {
                    val inetAddress = enumeration1.nextElement() as InetAddress
                    if (inetAddress.isLoopbackAddress) {
                        continue
                    }
                    netAddressList.add(inetAddress.hostAddress)
                }
            }
        }
    } catch (socketException: NullPointerException) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "getNetworkData:${socketException.message}")
    }*/
    return NetworkData(
        linkDownstreamBandwidth,
        linkUpstreamBandwidth,
        isActiveNetworkMetered,
        netAddressList
    )
}

@SuppressLint("HardwareIds")
fun getAndroidId(context: Context): String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
}

fun getUserAgent(): String {
    return "Android-Finsky/${Uri.encode(VENDING_VERSION_NAME)} (api=3,versionCode=$VENDING_VERSION_CODE,sdk=${Build.VERSION.SDK_INT},device=${Build.DEVICE},hardware=${Build.HARDWARE},product=${Build.PRODUCT},platformVersionRelease=${Build.VERSION.RELEASE},model=${Uri.encode(Build.MODEL)},buildId=${Build.ID},isWideScreen=0,supportedAbis=${Build.SUPPORTED_ABIS.joinToString(";")})"
}

fun createDeviceEnvInfo(context: Context): DeviceEnvInfo? {
    try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return DeviceEnvInfo(
            gpVersionCode = VENDING_VERSION_CODE,
            gpVersionName = VENDING_VERSION_NAME,
            gpPkgName = VENDING_PACKAGE_NAME,
            androidId = getAndroidId(context),
            biometricSupport = true,
            biometricSupportCDD = true,
            deviceId = getDeviceIdentifier(context),
            serialNo = Build.SERIAL ?: "",
            locale = Locale.getDefault(),
            userAgent = getUserAgent(),
            gpLastUpdateTime = packageInfo.lastUpdateTime,
            gpFirstInstallTime = packageInfo.firstInstallTime,
            gpSourceDir = packageInfo.applicationInfo.sourceDir!!,
            device = Build.DEVICE!!,
            displayMetrics = getDisplayInfo(context),
            telephonyData = getTelephonyData(context),
            product = Build.PRODUCT!!,
            model = Build.MODEL!!,
            manufacturer = Build.MANUFACTURER!!,
            fingerprint = Build.FINGERPRINT!!,
            release = Build.VERSION.RELEASE!!,
            brand = Build.BRAND!!,
            batteryLevel = getBatteryLevel(context),
            timeZoneOffset = if (SDK_INT >= 24) TimeZone.getDefault().rawOffset.toLong() else 0,
            locationData = getLocationData(context),
            isAdbEnabled = false, //Settings.Global.getInt(context.contentResolver, "adb_enabled", 0) == 1,
            installNonMarketApps = true, //Settings.Secure.getInt(context.contentResolver, "install_non_market_apps", 0) == 1,
            networkData = getNetworkData(context),
            uptimeMillis = SystemClock.uptimeMillis(),
            timeZoneDisplayName = if (SDK_INT >= 24) TimeZone.getDefault().displayName!! else "",
            googleAccounts = AccountManager.get(context).getAccountsByType(DEFAULT_ACCOUNT_TYPE).map { it.name }
        )
    } catch (e: Exception) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "createDeviceInfo", e)
        return null
    }
}