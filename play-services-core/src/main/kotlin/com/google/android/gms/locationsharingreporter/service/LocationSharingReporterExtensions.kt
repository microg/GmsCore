/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.locationsharingreporter.service

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.os.Build.VERSION.SDK_INT
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.locationsharingreporter.LocationShare
import com.google.android.gms.locationsharingreporter.PeriodicLocationReportingIssues
import com.google.android.gms.locationsharingreporter.service.LocationSharingUpdate.Companion.stopUpdateLocation
import com.google.android.gms.locationsharingreporter.service.ReportingRequestStoreFile.getReportingRequestStore
import com.google.android.gms.locationsharingreporter.service.ReportingRequestStoreFile.setLocationSharingEnabled
import com.squareup.wire.GrpcClient
import com.squareup.wire.Service
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.microg.gms.auth.AuthConstants
import social.userlocation.frontend.BatteryInfo
import social.userlocation.frontend.ClientEnvironment
import social.userlocation.frontend.DeviceLocationMessage
import social.userlocation.frontend.DeviceLocationRecord
import social.userlocation.frontend.GeoPoint
import social.userlocation.frontend.GeofenceReportingConfig
import social.userlocation.frontend.ReadSharesRequest
import social.userlocation.frontend.ReadSharesResponse
import social.userlocation.frontend.UploadConfig
import social.userlocation.frontend.UploadLocationRequest
import social.userlocation.frontend.UploadLocationResponse
import social.userlocation.frontend.UploadPolicy
import social.userlocation.frontend.UserLocationFrontendServiceClient
import social.userlocation.frontend.WearOsAccountAndLocationConfig
import social.userlocation.frontend.hflh
import java.util.Collections

private const val TAG = "LocationSharingReporter"
private const val AUTH_TOKEN_SCOPE: String = "oauth2:https://www.googleapis.com/auth/social.userlocation"
private const val STATE_ENABLED = 1
private const val STATE_DISABLED = 2


private class HeaderInterceptor(
        private val oauthToken: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request().newBuilder().header("authorization", "Bearer $oauthToken")
        return chain.proceed(original.build())
    }
}

private inline fun <reified S : Service> grpcClient(
        account: Account,
        accountManager: AccountManager
): S {
    val token = accountManager.blockingGetAuthToken(account, AUTH_TOKEN_SCOPE, true)
    val client = OkHttpClient().newBuilder()
            .addInterceptor(HeaderInterceptor(token))
            .build()
    val grpcClient = GrpcClient.Builder()
            .client(client)
            .baseUrl("https://socialuserlocation.googleapis.com")
            .minMessageToCompress(Long.MAX_VALUE)
            .build()
    return grpcClient.create(S::class)
}

fun sendLocationSharingEnable(isChecked: Boolean, account: Account, context: Context) {
    setLocationSharingEnabled(context, isChecked, account.name)
    val issues = if (!isChecked) {
        mutableSetOf(LocationShareIssue.SHARING_DISABLED.code)
    } else {
        emptySet()
    }
    val issuesByAccount = Bundle().apply {
        putIntArray(account.name, issues.toIntArray())
    }
    val periodicLocationReportingIssues = PeriodicLocationReportingIssues(intArrayOf(), issuesByAccount, true)
    Log.d(TAG, "sendLocationSharingEnable : $periodicLocationReportingIssues")
    val intent = Intent("com.google.android.gms.locationsharingreporter.PERIODIC_LOCATION_REPORTING_STATUS_DID_CHANGE")
    intent.putExtra("com.google.android.gms.locationsharingreporter.issues", SafeParcelableSerializer.serializeToBytes(periodicLocationReportingIssues))
    intent.`package` = "com.google.android.apps.maps"
    context.sendBroadcast(intent)
}

fun requestReadShares(
        context: Context,
        account: Account
): ReadSharesResponse? {
    try {
        val accountManager = AccountManager.get(context)
        val geofenceReportingConfig = GeofenceReportingConfig.Builder()
                .protocol(GeofenceReportingConfig.ReportingProtocol.REPORTING_PROTOCOL_MULTI_DEVICE_WITH_PRIMARY_DETECTION)
                .build()

        val config = WearOsAccountAndLocationConfig.Builder()
                .isLocationReportingEnabled(true)
                .build()

        val readSharesRequest = ReadSharesRequest.Builder()
                .config(config)
                .geofenceReportingConfig(geofenceReportingConfig)
                .build()

        val grpcClient = grpcClient<UserLocationFrontendServiceClient>(account, accountManager)
        return grpcClient.ReadShares().executeBlocking(readSharesRequest)
    } catch (e: Exception) {
        Log.w(TAG, "Error reading shares from server", e)
        return null
    }
}

fun readSharesResponseDetail(readSharesResponse: ReadSharesResponse?, context: Context, account: Account): ReportingRequestStore {
    return ReportingRequestStoreFile.loadReportingRequestStore(context) { store ->
        if (readSharesResponse != null) {
            val updatedMap = if (readSharesResponse.pinpointLocationSharesList.isEmpty()) {
                store.accountLocationSharingMap - account.name
            } else {
                val existingLocationSharingInfo = store.accountLocationSharingMap[account.name]
                if (existingLocationSharingInfo == null) {
                    val newSharingInfo = LocationSharingInfo.Builder().apply {
                        createdTimestamp = readSharesResponse.locationReportingParameters?.serverTimestamp
                                ?: System.currentTimeMillis()
                    }.build()
                    store.accountLocationSharingMap + (account.name to newSharingInfo)
                } else {
                    val updatedSharingInfo = existingLocationSharingInfo.newBuilder().apply {
                        createdTimestamp = readSharesResponse.locationReportingParameters?.serverTimestamp
                                ?: System.currentTimeMillis()
                    }.build()
                    store.accountLocationSharingMap + (account.name to updatedSharingInfo)
                }
            }

            store.newBuilder()
                    .startReportingTimestamp(System.currentTimeMillis())
                    .accountLocationSharingMap(updatedMap)
                    .build()
        } else {
            store
        }
    }
}

fun requestUploadLocation(
        context: Context,
        account: Account,
        request: UploadLocationRequest
): UploadLocationResponse? {
    return runCatching {
        val grpcClient: UserLocationFrontendServiceClient = grpcClient(account, AccountManager.get(context))
        grpcClient.UploadLocation().executeBlocking(request)
    }.onFailure { exception ->
        Log.e(TAG, "Upload location failed", exception)
    }.getOrNull()
}

fun validateGoogleAccount(account: Account) {
    require(account.type == AuthConstants.DEFAULT_ACCOUNT_TYPE) {
        "Invalid account type, not a Google account"
    }
}

fun validateMakePrimaryOption(option: Int) {
    val validOptions = listOf(
            ReportingType.ONGOING_REPORTING_ENABLED.value,
            ReportingType.SINGLE_SHARE_REPORTING_ENABLED.value,
    )
    require(option in validOptions) { "Invalid MakePrimaryOption" }
}

fun validateReportingType(type: Int) {
    val validTypes = listOf(
            ReportingType.SINGLE_SHARE_REPORTING_ENABLED.value,
            ReportingType.ONGOING_REPORTING_ENABLED.value,
    )
    require(type in validTypes) {
        "Invalid ReportingType, valid reporting types are: SINGLE_SHARE_REPORTING_ENABLED, ONGOING_REPORTING_ENABLED."
    }
}

fun validateLocationShare(locationShare: LocationShare?) {
    require(locationShare?.obfuscatedGaiaId != null || locationShare?.tokenId != null) {
        "Invalid location share, neither obfuscated gaia ID nor token ID are specified in it."
    }
}

fun updateDeviceBatterySaverState(context: Context) {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isPowerSaveMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        powerManager.isPowerSaveMode
    } else {
        false
    }
    Log.i(TAG, "Updating device battery saver state, isEnabled: $isPowerSaveMode")

    ReportingRequestStoreFile.updateReportingRequestStore(context) { requestStore ->
        val state = if (isPowerSaveMode) STATE_ENABLED else STATE_DISABLED
        return@updateReportingRequestStore requestStore.newBuilder().batterySaverState(state).build()
    }
}

fun updateDeviceLocationSettingState(context: Context) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val isEnabled = LocationManagerCompat.isLocationEnabled(locationManager)
    Log.i(TAG, "Updating device location setting state, isEnabled: $isEnabled")

    ReportingRequestStoreFile.updateReportingRequestStore(context) { requestStore ->
        val state = if (isEnabled) STATE_ENABLED else STATE_DISABLED
        return@updateReportingRequestStore requestStore.newBuilder().locationSettingState(state).build()
    }
}

object ReportingObject {
    val generalIssues = HashSet<Int>()
    val issuesByAccount = HashMap<String, HashSet<Int>>()
}

fun getLocationReportingStatus(context: Context) {
    val reportingRequestStore = getReportingRequestStore(context)
    if (reportingRequestStore.batterySaverState == STATE_ENABLED) {
        ReportingObject.generalIssues.add(LocationShareIssue.BATTERY_SAVER_ENABLED.code)
    }
    if (reportingRequestStore.locationSettingState == STATE_ENABLED) {
        ReportingObject.generalIssues.add(LocationShareIssue.LOCATION_DISABLED_IN_SETTINGS.code)
    }

    val account = AccountManager.get(context).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)[0]
    if (account != null) {
        if (reportingRequestStore.accountLocationSharingMap.get(account.name) == null) {
            ReportingObject.issuesByAccount.put(account.name, HashSet(13))
        }

    }
}

fun refreshAndUploadLocation(context: Context, account: Account, location: Location) {
    Log.d(TAG, "Refreshing periodic location reporting state")
    val gmscoreVersion = try {
        context.packageManager.getPackageInfo("com.google.android.gms", 0).versionName
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }

    val uploadLocationRequestBuilder = UploadLocationRequest().newBuilder()
    uploadLocationRequestBuilder.requestCreationTime = System.currentTimeMillis()
    uploadLocationRequestBuilder.isUserReportingDisabled = true// todo
    uploadLocationRequestBuilder.geofenceReportingConfig(GeofenceReportingConfig().newBuilder()
             .protocol(GeofenceReportingConfig.ReportingProtocol.REPORTING_PROTOCOL_MULTI_DEVICE_WITH_PRIMARY_DETECTION)
             .build())
    uploadLocationRequestBuilder.shouldEnableGeofencingOptimization(false)
    uploadLocationRequestBuilder.uploadConfig(UploadConfig().newBuilder()
            .unknowInt1(5).uploadPolicy(UploadPolicy().newBuilder()
                    .allowNonOvenfreshUploads(true).build()).build())
    uploadLocationRequestBuilder.clientEnvironment = ClientEnvironment().newBuilder()
            .androidVersion("Android: $SDK_INT")
            .gmscoreVersion("Gmscore: $gmscoreVersion")
            .build()

    val deviceLocationMessage =DeviceLocationMessage().newBuilder()
        .deviceLocationRecord(
            DeviceLocationRecord().newBuilder()
                .eventTimestampMillis(System.currentTimeMillis())
                .geoPoint(GeoPoint().newBuilder().altitude(location.altitude).longitude(location.longitude).latitude(location.latitude).build())
                .accuracy(location.accuracy.toDouble())
                .batteryInfo(getBatterInfo(context))
                .unKnownMessage5(Collections.singletonList(hflh().newBuilder().unknowInt1(30).build()))
                .build()).build()
    uploadLocationRequestBuilder.deviceLocationMessageList(Collections.singletonList(deviceLocationMessage))


    val uploadLocationResponse = requestUploadLocation(context, account, uploadLocationRequestBuilder.build())
    Log.d(TAG, "UploadLocationResponse: $uploadLocationResponse")

    if (uploadLocationResponse?.locationReportingParameters?.isReportingEnabled == false) {
        stopUpdateLocation()
    }
}

private fun getBatterInfo(context: Context) : BatteryInfo {
    val intent = ContextWrapper(context).registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    if (intent != null) {
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale > 0) {
            Log.d(TAG, "getBatterInfo isCharging: $isCharging battery level:${level * 100 / scale}")
            return BatteryInfo().newBuilder().isCharging(isCharging).batteryLevelPercent(level * 100 / scale).build()
        }
    }
    return BatteryInfo()
}
