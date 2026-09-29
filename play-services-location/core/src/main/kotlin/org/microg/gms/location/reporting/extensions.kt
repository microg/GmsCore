/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.reporting

import android.accounts.Account
import android.accounts.AccountManager
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.PowerManager
import android.os.UserManager
import android.provider.Settings
import androidx.core.location.LocationManagerCompat
import com.squareup.wire.GrpcClient
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.microg.gms.auth.AuthConstants
import org.microg.gms.common.Constants
import org.microg.gms.profile.Build
import org.microg.gms.profile.ProfileManager
import userlocation.BuildInfo
import userlocation.DeviceCapabilities
import userlocation.DeviceDescriptor
import userlocation.UserLocationReportingServiceClient
import java.util.concurrent.TimeUnit

const val TAG = "LocationReporting"

private const val SERVICE_HOST = "https://userlocation.googleapis.com"
private const val AUTH_TOKEN_SCOPE = "oauth2:https://www.googleapis.com/auth/userlocation.reporting"
private const val CALL_TIMEOUT_SECONDS = 10L
internal const val DEVICE_STATE_PREFERENCES = "location_reporting_api"
const val REPORTING_SETTINGS_CHANGED_ACTION = "com.google.android.gms.location.reporting.SETTINGS_CHANGED"

private const val MAPS_PACKAGE_NAME = "com.google.android.apps.maps"
private const val RESTRICTED_PROFILE_KEY = "restricted_profile"
private const val WATCH_FEATURE = "android.hardware.type.watch"
private const val AUTOMOTIVE_FEATURE = "android.hardware.type.automotive"
private const val TELEVISION_FEATURE = "android.hardware.type.television"
private const val LOCATION_MODE_KEY = "location_mode"
private const val WIFI_SCAN_ALWAYS_AVAILABLE_KEY = "wifi_scan_always_enabled"
private const val BATTERY_SAVER_TRIGGER_LEVEL_KEY = "low_power_trigger_level"

internal const val OPT_IN_RESULT_SUCCESS = 0
internal const val OPT_IN_RESULT_WRITE_FAILED = 1
internal const val OPT_IN_RESULT_MISSING_ACCOUNT = 2
internal const val OPT_IN_RESULT_INVALID_ACCOUNT = 3
internal const val OPT_IN_RESULT_CALLER_NOT_ALLOWED = 5
internal const val OPT_IN_RESULT_TAG_TOO_LONG = 11

internal data class ReportingApiSession(
    val client: UserLocationReportingServiceClient,
    val deviceTag: Int,
    val device: DeviceDescriptor
)

internal data class LocalReportingConditions(
    val countryAllowed: Boolean,
    val deviceCapable: Boolean,
    val restrictedProfile: Boolean,
    val deviceFormFactorSupported: Boolean,
    val locationEnabled: Boolean
) {
    val allowed: Boolean
        get() = countryAllowed && deviceCapable && !restrictedProfile && deviceFormFactorSupported
}

private val baseHttpClient = OkHttpClient.Builder()
    .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .build()

private class AuthorizationInterceptor(private val oauthToken: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("authorization", "Bearer $oauthToken")
            .build()
        return chain.proceed(request)
    }
}

private fun reportingServiceClient(oauthToken: String): UserLocationReportingServiceClient {
    val client = baseHttpClient.newBuilder()
        .addInterceptor(AuthorizationInterceptor(oauthToken))
        .build()
    val grpcClient = GrpcClient.Builder()
        .client(client)
        .baseUrl(SERVICE_HOST)
        .minMessageToCompress(Long.MAX_VALUE)
        .build()
    return grpcClient.create(UserLocationReportingServiceClient::class)
}

internal fun openReportingApiSession(
    context: Context,
    account: Account,
    deviceTag: Int
): ReportingApiSession? {
    if (account.type != AuthConstants.DEFAULT_ACCOUNT_TYPE) return null
    val oauthToken = runCatching {
        AccountManager.get(context).blockingGetAuthToken(account, AUTH_TOKEN_SCOPE, false)
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
    return ReportingApiSession(
        client = reportingServiceClient(oauthToken),
        deviceTag = deviceTag,
        device = buildDeviceDescriptor(context)
    )
}

private fun buildDeviceDescriptor(context: Context): DeviceDescriptor {
    ProfileManager.ensureInitialized(context)
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    return DeviceDescriptor.Builder()
        .buildFingerprint(Build.FINGERPRINT)
        .sdkInt(Build.VERSION.SDK_INT)
        .deviceModel(Build.MODEL)
        .enum12(0)
        .gmsVersionCode(Constants.GMS_VERSION_CODE)
        .gmsApkVersionCode(Constants.GMS_VERSION_CODE)
        .buildInfo(
            BuildInfo.Builder()
                .manufacturer(Build.MANUFACTURER)
                .brand(Build.BRAND)
                .product(Build.PRODUCT)
                .device(Build.DEVICE)
                .model(Build.MODEL)
                .buildFlag(activityManager?.isLowRamDevice ?: false)
                .build()
        )
        .moduleVersion(Constants.GMS_VERSION_CODE)
        .osType(1)
        .build()
}

internal fun buildDeviceCapabilities(context: Context, fullyEnabled: Boolean): DeviceCapabilities {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    val locationEnabled = locationManager?.let { LocationManagerCompat.isLocationEnabled(it) } ?: false
    val builder = DeviceCapabilities.Builder().locationEnabled(locationEnabled)
    if (!fullyEnabled) return builder.build()

    val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
    val restrictedProfile = userManager
        ?.getApplicationRestrictions(Constants.GMS_PACKAGE_NAME)
        ?.getString(RESTRICTED_PROFILE_KEY) == "true"
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val batterySaverOn = if (android.os.Build.VERSION.SDK_INT >= 21) {
        powerManager?.isPowerSaveMode ?: false
    } else {
        false
    }
    val packageManager = context.packageManager
    val deviceFormFactorSupported = !packageManager.hasSystemFeature(WATCH_FEATURE) &&
        !packageManager.hasSystemFeature(AUTOMOTIVE_FEATURE) &&
        !packageManager.hasSystemFeature(TELEVISION_FEATURE)
    val locationMode = Settings.Secure.getInt(
        context.contentResolver,
        LOCATION_MODE_KEY,
        Settings.Secure.LOCATION_MODE_OFF
    )

    builder.countryAllowed(true)
        .deviceCapable(true)
        .restrictedProfile(restrictedProfile)
        .deviceFormFactorSupported(deviceFormFactorSupported)
        .locationMode(locationMode)
        .batterySaverOn(batterySaverOn)
        .batterySaverTriggerLevel(
            Settings.Global.getInt(
                context.contentResolver,
                BATTERY_SAVER_TRIGGER_LEVEL_KEY,
                0
            )
        )

    if (packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
        builder.wifiScanningEnabled(
            Settings.Global.getInt(
                context.contentResolver,
                WIFI_SCAN_ALWAYS_AVAILABLE_KEY,
                0
            ) == 1
        )
    }
    return builder.build()
}

internal fun getLocalReportingConditions(context: Context): LocalReportingConditions {
    val capabilities = buildDeviceCapabilities(context, fullyEnabled = true)
    return LocalReportingConditions(
        countryAllowed = capabilities.countryAllowed != false,
        deviceCapable = capabilities.deviceCapable != false,
        restrictedProfile = capabilities.restrictedProfile == true,
        deviceFormFactorSupported = capabilities.deviceFormFactorSupported != false,
        locationEnabled = capabilities.locationEnabled == true
    )
}

internal fun isGoogleAccountOnDevice(context: Context, account: Account?): Boolean {
    if (account?.type != AuthConstants.DEFAULT_ACCOUNT_TYPE) return false
    return runCatching { context.googleAccounts.contains(account) }.getOrDefault(false)
}

internal val Context.googleAccounts: Array<Account>
    get() = AccountManager.get(this).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)

internal val Context.reportingPreferences: SharedPreferences
    get() = getSharedPreferences(DEVICE_STATE_PREFERENCES, Context.MODE_PRIVATE)

internal val Account.reportingPreferenceSuffix: String
    get() = "$type:$name"

internal fun notifyReportingSettingsChanged(context: Context, targetPackage: String? = null) {
    sequenceOf(MAPS_PACKAGE_NAME, Constants.GMS_PACKAGE_NAME, targetPackage)
        .filterNotNull()
        .distinct()
        .forEach { packageName ->
            context.sendBroadcast(Intent(REPORTING_SETTINGS_CHANGED_ACTION).setPackage(packageName))
        }
}

internal fun Boolean?.toReportingStatus(): Int = when (this) {
    true -> 1
    false -> -1
    null -> 0
}
