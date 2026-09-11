/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.deviceinfo

import android.Manifest
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.icu.util.TimeZone
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.Build.VERSION.SDK_INT
import android.os.SystemClock
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.WindowManager
import androidx.core.content.ContextCompat
import org.microg.gms.auth.AuthConstants
import org.microg.gms.common.Constants
import org.microg.gms.common.DeviceIdentifier
import org.microg.gms.profile.Build
import org.microg.gms.utils.digest
import org.microg.gms.utils.toBase64
import java.util.Locale

private const val TAG = "DeviceInfoCollector"

@SuppressLint("MissingPermission")
fun getDeviceIdentifier(context: Context): String {
    // TODO: Improve dummy data
    val deviceId = DeviceIdentifier().meid /*try {
        (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)?.let {
            it.subscriberId ?: it.deviceId
        }
    } catch (e: Exception) {
        null
    }*/
    return deviceId.toByteArray(Charsets.UTF_8).digest("SHA-1")
        .toBase64(Base64.URL_SAFE + Base64.NO_WRAP + Base64.NO_PADDING)
}

fun getDisplayInfo(context: Context): DisplayMetrics? {
    return try {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
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
    var batteryLevel = -1
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

@SuppressLint("MissingPermission")
fun getLocationData(context: Context): LocationData? {
    // TODO: Dummy data
    return null /*try {
        (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?)?.let { locationManager ->
            locationManager.getLastKnownLocation("network")?.let { location ->
                return LocationData(
                    location.altitude,
                    location.latitude,
                    location.longitude,
                    location.accuracy,
                    location.time.toDouble()
                )
            }
        }
    } catch (e: Exception) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "getLocationData", e)
        null
    }*/
}

@SuppressLint("MissingPermission")
fun getNetworkData(context: Context): NetworkData {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    val linkDownstreamBandwidth: Long = 0
    val linkUpstreamBandwidth: Long = 0
    // TODO: Dummy data — populate bandwidth via NetworkCapabilities when permission available
    val isActiveNetworkMetered = connectivityManager?.isActiveNetworkMetered ?: false
    val netAddressList = mutableListOf<String>()
    // TODO: Dummy data — enumerate NetworkInterface inet addresses
    return NetworkData(
        linkDownstreamBandwidth,
        linkUpstreamBandwidth,
        isActiveNetworkMetered,
        netAddressList
    )
}

@SuppressLint("HardwareIds")
fun getAndroidId(context: Context): String =
    Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""

fun isCharging(context: Context): Boolean {
    val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val intent = if (Build.VERSION.SDK_INT < 33) {
        context.registerReceiver(null, intentFilter)
    } else {
        context.registerReceiver(null, intentFilter, null, null)
    }
    return intent?.let {
        val status = it.getIntExtra("status", -1)
        status == 2 || status == 5
    } ?: false
}

fun isInCallOrRingMode(context: Context): Boolean {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    return audioManager?.let {
        when (it.mode) {
            AudioManager.MODE_IN_CALL, AudioManager.MODE_RINGTONE -> true
            else -> false
        }
    } ?: false
}

fun isUsbConnected(context: Context): Boolean {
    val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
    val packageManager = context.packageManager
    return if (usbManager != null &&
        (packageManager.hasSystemFeature("android.hardware.usb.host") ||
                packageManager.hasSystemFeature("android.hardware.usb.accessory"))
    ) {
        try {
            val accessoryList = usbManager.accessoryList
            val deviceList = usbManager.deviceList
            !(accessoryList == null && deviceList.isEmpty())
        } catch (e: NullPointerException) {
            false
        }
    } else {
        false
    }
}

fun getScreenBrightness(context: Context): Int {
    return try {
        Settings.System.getInt(context.contentResolver, "screen_brightness")
    } catch (e: Settings.SettingNotFoundException) {
        -1
    }
}

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/**
 * Build a [DeviceEnvInfo] for a payments / vending session.
 *
 * Module-specific parameters (gpVersionCode / gpVersionName / gpPkgName / userAgent)
 * must be supplied because each module identifies itself with its own version string.
 *
 * All payments-protocol extension fields (sdkVersion / gmsPackageName / camera permission /
 * battery / USB / call mode / screen brightness) are always collected and populated —
 * vending downstream ignores fields it doesn't send.
 *
 * Static device info (DEVICE / PRODUCT / SERIAL / …) is read through the profile-aware
 * [Build] wrapper, so test profiles override what the system would expose.
 *
 * @return null if the package info lookup fails or any inner collector throws
 */
@SuppressLint("MissingPermission")
fun createDeviceEnvInfo(
    context: Context,
    gpVersionCode: Long,
    gpVersionName: String,
    gpPkgName: String,
    userAgent: String = "",
): DeviceEnvInfo? {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(Constants.VENDING_PACKAGE_NAME, 0)
        Log.d(TAG, "createDeviceEnvInfo: pkg=${packageInfo.packageName} ver=${packageInfo.versionName}/${packageInfo.versionCode}")
        DeviceEnvInfo(
            gpVersionCode = gpVersionCode,
            gpVersionName = gpVersionName,
            gpPkgName = gpPkgName,
            gpLastUpdateTime = packageInfo.lastUpdateTime,
            gpFirstInstallTime = packageInfo.firstInstallTime,
            gpSourceDir = packageInfo.applicationInfo!!.sourceDir!!,
            androidId = getAndroidId(context),
            biometricSupport = true,
            biometricSupportCDD = true,
            deviceId = getDeviceIdentifier(context),
            serialNo = Build.SERIAL ?: "",
            locale = Locale.getDefault(),
            userAgent = userAgent,
            device = Build.DEVICE ?: "",
            displayMetrics = getDisplayInfo(context),
            telephonyData = getTelephonyData(context),
            locationData = getLocationData(context),
            networkData = getNetworkData(context),
            product = Build.PRODUCT ?: "",
            model = Build.MODEL ?: "",
            manufacturer = Build.MANUFACTURER ?: "",
            fingerprint = Build.FINGERPRINT ?: "",
            release = Build.VERSION.RELEASE ?: "",
            brand = Build.BRAND ?: "",
            batteryLevel = getBatteryLevel(context),
            timeZoneOffset = if (SDK_INT >= 24) TimeZone.getDefault().rawOffset.toLong() else 0,
            isAdbEnabled = false,
            installNonMarketApps = true,
            uptimeMillis = SystemClock.uptimeMillis(),
            timeZoneDisplayName = if (SDK_INT >= 24) TimeZone.getDefault().displayName!! else "",
            googleAccounts = AccountManager.get(context)
                .getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE).map { it.name },
            sdkVersion = SDK_INT.toString(),
            gmsPackageName = Constants.GMS_PACKAGE_NAME,
            cameraPermissionState = if (context.hasPermission(Manifest.permission.CAMERA)) 1 else 2,
            isInCallOrRingMode = isInCallOrRingMode(context),
            isUsbConnected = isUsbConnected(context),
            isCharging = isCharging(context),
            screenBrightness = getScreenBrightness(context),
        )
    } catch (e: Exception) {
        Log.w(TAG, "createDeviceEnvInfo", e)
        null
    }
}
