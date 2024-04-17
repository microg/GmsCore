/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.settings

import android.Manifest.permission.*
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager.*
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.location.LocationManager.NETWORK_PROVIDER
import android.os.Build.VERSION.SDK_INT
import android.provider.Settings
import androidx.core.content.getSystemService
import com.google.android.gms.location.LocationSettingsStates
import org.microg.gms.location.hasNetworkLocationServiceBuiltIn

data class DetailedLocationSettingsStates(
    val gpsSystemFeature: Boolean,
    val networkLocationSystemFeature: Boolean,
    val bluetoothLeSystemFeature: Boolean,
    val gpsProviderEnabled: Boolean,
    val networkLocationProviderEnabled: Boolean,
    val networkLocationProviderBuiltIn: Boolean,
    val fineLocationPermission: Boolean,
    val coarseLocationPermission: Boolean,
    val backgroundLocationPermission: Boolean,
    val blePresent: Boolean,
    val bleEnabled: Boolean,
    val bleScanAlways: Boolean,
    val airplaneMode: Boolean,
) {
    val gpsPresent: Boolean
        get() = gpsSystemFeature
    val networkLocationPresent: Boolean
        get() = networkLocationSystemFeature || networkLocationProviderBuiltIn
    val gpsUsable: Boolean
        get() = gpsProviderEnabled && fineLocationPermission && backgroundLocationPermission
    val networkLocationUsable: Boolean
        get() = (networkLocationProviderEnabled || networkLocationProviderBuiltIn) && coarseLocationPermission && backgroundLocationPermission
    val bleUsable: Boolean
        get() = blePresent && (bleEnabled || (bleScanAlways && !airplaneMode))

    fun toApi() = LocationSettingsStates(gpsUsable, networkLocationUsable, bleUsable, gpsPresent, networkLocationPresent, blePresent)
}

fun Context.getDetailedLocationSettingsStates(): DetailedLocationSettingsStates {
    val bluetoothLeSystemFeature = packageManager.hasSystemFeature(FEATURE_BLUETOOTH_LE)
    val locationManager = getSystemService<LocationManager>()
    val bluetoothManager = if (bluetoothLeSystemFeature) getSystemService<BluetoothManager>() else null
    val bleAdapter = bluetoothManager?.adapter

    return DetailedLocationSettingsStates(
        gpsSystemFeature = packageManager.hasSystemFeature(FEATURE_LOCATION_GPS),
        networkLocationSystemFeature = packageManager.hasSystemFeature(FEATURE_LOCATION_NETWORK),
        bluetoothLeSystemFeature = bluetoothLeSystemFeature,
        gpsProviderEnabled = locationManager?.isProviderEnabled(GPS_PROVIDER) == true,
        networkLocationProviderEnabled = locationManager?.isProviderEnabled(NETWORK_PROVIDER) == true,
        networkLocationProviderBuiltIn = hasNetworkLocationServiceBuiltIn(),
        fineLocationPermission = packageManager.checkPermission(ACCESS_FINE_LOCATION, packageName) == PERMISSION_GRANTED,
        coarseLocationPermission = packageManager.checkPermission(ACCESS_COARSE_LOCATION, packageName) == PERMISSION_GRANTED,
        backgroundLocationPermission = if (SDK_INT < 29) true else
            packageManager.checkPermission(ACCESS_BACKGROUND_LOCATION, packageName) == PERMISSION_GRANTED,
        blePresent = bleAdapter != null,
        bleEnabled = bleAdapter?.isEnabled == true,
        bleScanAlways = Settings.Global.getInt(contentResolver, "ble_scan_always_enabled", 0) == 1,
        airplaneMode = Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
    )
}