/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.deviceinfo

import java.util.Locale

data class DeviceEnvInfo(
    val gpVersionCode: Long,
    val gpVersionName: String,
    val gpPkgName: String,
    val gpLastUpdateTime: Long,
    val gpFirstInstallTime: Long,
    val gpSourceDir: String,
    val androidId: String,
    val biometricSupport: Boolean,
    val biometricSupportCDD: Boolean,
    val deviceId: String,
    val serialNo: String,
    val locale: Locale,
    val userAgent: String,
    val device: String,
    val displayMetrics: DisplayMetrics?,
    val telephonyData: TelephonyData?,
    val locationData: LocationData?,
    val networkData: NetworkData?,
    val product: String,
    val model: String,
    val manufacturer: String,
    val fingerprint: String,
    val release: String,
    val brand: String,
    val batteryLevel: Int,
    val timeZoneOffset: Long,
    val isAdbEnabled: Boolean,
    val installNonMarketApps: Boolean,
    val uptimeMillis: Long,
    val timeZoneDisplayName: String,
    val googleAccounts: List<String>,

    val sdkVersion: String? = null,
    val gmsPackageName: String? = null,
    val cameraPermissionState: Int = -1,
    val isInCallOrRingMode: Boolean = false,
    val isUsbConnected: Boolean = false,
    val isCharging: Boolean = false,
    val screenBrightness: Int = -1
)

data class DisplayMetrics(
    val widthPixels: Int,
    val heightPixels: Int,
    val xdpi: Float,
    val ydpi: Float,
    val densityDpi: Int
)

data class TelephonyData(
    val simOperatorName: String,
    val phoneDeviceId: String,
    val networkOperator: String,
    val simOperator: String,
    val phoneType: Int = -1,
    val grantedPhonePermissionState: Int = -1,
    val isSmsCapable: Boolean = false,
    val activeSubscriptionInfoCount: Int = 0
)

data class LocationData(
    val altitude: Double,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val time: Double
)

data class NetworkData(
    val linkDownstreamBandwidth: Long,
    val linkUpstreamBandwidth: Long,
    val isActiveNetworkMetered: Boolean,
    val netAddressList: List<String>,
)