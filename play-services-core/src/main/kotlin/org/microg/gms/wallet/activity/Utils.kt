/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wallet.activity

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.text.TextUtils
import android.util.Log
import org.microg.gms.common.Constants
import org.microg.gms.deviceinfo.DeviceEnvInfo
import org.microg.gms.deviceinfo.createDeviceEnvInfo
import org.microg.vending.billing.proto.BasicDeviceFeature
import org.microg.vending.billing.proto.ClientToken
import org.microg.vending.billing.proto.CompressionType
import org.microg.vending.billing.proto.DeviceBasedInputType
import org.microg.vending.billing.proto.FidoDeviceFeature
import org.microg.vending.billing.proto.SecureElementState
import org.microg.vending.billing.proto.UserVerifying
import java.lang.Long
import java.util.Locale
import java.util.UUID

private const val TAG = "WalletUtils"

fun createDeviceEnvInfo(context: Context): DeviceEnvInfo? {
    val packageInfo = tryGetPackageInfo(context, Constants.VENDING_PACKAGE_NAME)
    Log.d(TAG, "createDeviceEnvInfo: pkg=${packageInfo?.packageName} ver=${packageInfo?.versionName}/${packageInfo?.versionCode}")
    return createDeviceEnvInfo(
        context,
        gpVersionCode = packageInfo?.versionCode?.toLong() ?: 0L,
        gpVersionName = packageInfo?.versionName ?: "",
        gpPkgName = Constants.VENDING_PACKAGE_NAME,
    )
}

fun localeToString(locale: Locale): String {
    val result = StringBuilder()
    result.append(locale.language)
    locale.country.let {
        if (it.isNotEmpty())
            result.append("-$it")
    }
    locale.variant.let {
        if (it.isNotEmpty())
            result.append("-$it")
    }
    return result.toString()
}

fun createClientTokenInfo1(
    context: Context,
    deviceInfo: DeviceEnvInfo,
    gsfId: String?,
    shouldIncludeExtraInfo: Boolean = false,
): ClientToken.Info1 {
    return ClientToken.Info1.Builder().apply {
        this.locale = localeToString(deviceInfo.locale)
        this.unknown8 = 2
        this.gpVersionCode = deviceInfo.gpVersionCode
        this.deviceInfo = ClientToken.DeviceInfo.Builder().apply {
            this.sdkVersion = deviceInfo.sdkVersion
            this.device = deviceInfo.device
            deviceInfo.displayMetrics?.let {
                this.widthPixels = it.widthPixels
                this.heightPixels = it.heightPixels
                this.xdpi = it.xdpi
                this.ydpi = it.ydpi
                this.densityDpi = it.densityDpi
            }
            this.gpPackage = deviceInfo.gmsPackageName
            this.gpVersionCode = deviceInfo.gpVersionCode.toString()
            this.gpVersionName = deviceInfo.gpVersionName
            if (shouldIncludeExtraInfo) {
                this.envInfo = ClientToken.EnvInfo.Builder().apply {
                    this.deviceData = ClientToken.DeviceData.Builder().apply {
                        this.unknown1 = 0
                        deviceInfo.telephonyData?.let {
                            this.simOperatorName = it.simOperatorName
                            this.phoneDeviceId = it.phoneDeviceId
                            this.phoneDeviceId1 = it.phoneDeviceId
                        }
                        this.gsfId = Long.parseLong(gsfId ?: "1", 16)
                        this.device = deviceInfo.device
                        this.product = deviceInfo.product
                        this.model = deviceInfo.model
                        this.manufacturer = deviceInfo.manufacturer
                        this.fingerprint = deviceInfo.fingerprint
                        this.release = deviceInfo.release
                        this.brand = deviceInfo.brand
                        this.serial = deviceInfo.serialNo
                        this.isEmulator = false
                    }.build()
                    this.otherInfo = ClientToken.OtherInfo.Builder().apply {
                        this.packageInfoList = listOfNotNull(
                            tryGetPackageInfo(context, Constants.GMS_PACKAGE_NAME),
                            tryGetPackageInfo(context, Constants.VENDING_PACKAGE_NAME)
                        ).map { buildPackageInfo(it) }
                        this.batteryLevel = deviceInfo.batteryLevel
                        this.timeZoneOffset = deviceInfo.timeZoneOffset
                        this.isAdbEnabled = deviceInfo.isAdbEnabled
                        this.installNonMarketApps = deviceInfo.installNonMarketApps
                        this.iso3Language = deviceInfo.locale.isO3Language
                        this.netAddress = deviceInfo.networkData?.netAddressList ?: emptyList()
                        this.locale = deviceInfo.locale.toString()
                        this.language = deviceInfo.locale.language
                        this.country = deviceInfo.locale.country
                        this.uptimeMillis = deviceInfo.uptimeMillis
                        this.timeZoneDisplayName = deviceInfo.timeZoneDisplayName
                        this.googleAccountCount = deviceInfo.googleAccounts.size
                        this.isUserAMonkey = ActivityManager.isUserAMonkey()
                        this.isInCallOrRingMode = deviceInfo.isInCallOrRingMode
                        this.isUsbConnected = deviceInfo.isUsbConnected
                        this.isCharging = deviceInfo.isCharging
                        this.screenBrightness = deviceInfo.screenBrightness
                        this.displayMetrics = deviceInfo.displayMetrics?.let {
                            ClientToken.DisplayMetrics.Builder().apply {
                                this.widthPixels = it.widthPixels
                                this.heightPixels = it.heightPixels
                            }.build()
                        }
                    }.build()
                }.build()
            }
            this.callingPackage = deviceInfo.gpPkgName
            this.marketClientId = "am-android-att-us"
            this.unknown15 = 2
            this.moduleVersion = 253534000
            this.curAuthContext = getBasicSupportedFeatures(context)
            this.cameraPermissionState = deviceInfo.cameraPermissionState
            deviceInfo.networkData?.let {
                this.linkDownstreamBandwidth = it.linkDownstreamBandwidth
                this.linkUpstreamBandwidth = it.linkUpstreamBandwidth
                this.isActiveNetworkMetered = it.isActiveNetworkMetered
            }
            this.supportedAuthTypes = listOf(
                FidoDeviceFeature.FINGERPRINT,
                FidoDeviceFeature.BIOMETRIC,
                FidoDeviceFeature.PIN_PASSWORD_OR_PATTERN,
            )
            deviceInfo.telephonyData?.let {
                this.isSmsCapable = it.isSmsCapable
                this.grantedPhonePermissionState = it.grantedPhonePermissionState
                this.activeSubscriptionInfoCount = it.activeSubscriptionInfoCount
            }
            this.phenotypeServerToken = emptyList()
            this.unknown34 = 0
            this.uptimeMillis = deviceInfo.uptimeMillis
            this.timeZoneDisplayName = deviceInfo.timeZoneDisplayName
            this.androidId = Long.parseLong(gsfId ?: "1", 16)
            this.secureElementState = SecureElementState.SECURE_ELEMENT_STATE_UNKNOWN
            this.inputTypeList = listOf(
                DeviceBasedInputType.DEVICE_BASED_INPUT_TYPE_CARD_OCR,
                DeviceBasedInputType.DEVICE_BASED_INPUT_TYPE_NFC,
            )
            this.ocrServiceAvailability = true
            this.gpLongVersionCode = deviceInfo.gpVersionCode.toString()
            this.longVersionCode = Constants.GMS_VERSION_CODE.toString()
            if (!TextUtils.isEmpty(deviceInfo.model)) {
                this.modelName = deviceInfo.product
            }
        }.build()
        this.leastSignificantBits = UUID.randomUUID().leastSignificantBits
        this.googleAccounts = deviceInfo.googleAccounts
        this.unknown_bool_1 = false
        this.unknown_int_1 = 0
        this.userVerifying = UserVerifying.Builder().unknown2(true).build()
        this.sessionId = UUID.randomUUID().leastSignificantBits
        this.google_account_count = deviceInfo.googleAccounts.size
        this.current_account_index = 1
        this.compressed_types = listOf(
            CompressionType.COMPRESSION_TYPE_IDENTITY,
            CompressionType.COMPRESSION_TYPE_BROTLI
        )
    }.build()
}

fun tryGetPackageInfo(context: Context, packageName: String): PackageInfo? {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        Log.w(TAG, "tryGetPackageInfo($packageName) not installed", e)
        null
    }
}

fun buildPackageInfo(packageInfo: PackageInfo): ClientToken.GPInfo {
    return ClientToken.GPInfo.Builder().apply {
        if (packageInfo.packageName.isNotEmpty()) {
            this.package_ = packageInfo.packageName
        }
        this.versionCode = packageInfo.versionCode.toString()
        this.lastUpdateTime = packageInfo.lastUpdateTime
        this.firstInstallTime = packageInfo.firstInstallTime
        packageInfo.applicationInfo?.sourceDir?.let { this.sourceDir = it }
    }.build()
}

fun Context.isPackageInstalled(packageName: String, matchAnySignatures: Boolean = false): Boolean {
    return try {
        packageManager.getPackageInfo(
            packageName,
            if (matchAnySignatures) PackageManager.GET_SIGNING_CERTIFICATES else 0
        )
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

fun getBasicSupportedFeatures(context: Context): List<BasicDeviceFeature> {
    val packageManager = context.packageManager
    val features = mutableListOf<BasicDeviceFeature>()

    val intent = Intent("com.google.android.gms.ocr.ACTION_CARD_CAPTURE").apply {
        setPackage("com.google.android.gms")
    }
    if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
        features.add(BasicDeviceFeature.CAMERA_DOCUMENT_CAPTURE)
    }

    if (NfcAdapter.getDefaultAdapter(context) != null) {
        features.add(BasicDeviceFeature.NFC_DEVICE_SUPPORT)
    }

    if (context.isPackageInstalled("com.felicanetworks.mfc")) {
        features.add(BasicDeviceFeature.FELICA_SUPPORT)
    }

    return features
}

