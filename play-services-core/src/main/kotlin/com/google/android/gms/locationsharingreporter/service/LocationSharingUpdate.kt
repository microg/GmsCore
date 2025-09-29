/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.locationsharingreporter.service

import android.accounts.Account
import android.content.Context
import android.os.Binder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.locationsharingreporter.service.ReportingRequestStoreFile.isLocationSharingEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationSharingUpdate {
    companion object {
        private var locationCallback: LocationCallback? = null
        private var accountInfo: Account? = null
        private const val TAG = "LocationSharingUpdate"
        @Volatile
        private var appContext: Context? = null

        fun startUpdateLocation(account: Account, context: Context) {
            Log.d(TAG, "startUpdateLocation: ")
            accountInfo = account
            if (locationCallback == null) {
                locationCallback = createLocationUpdatesCallback(context)
                locationCallback?.let {
                    val locationRequest = LocationRequest.Builder(5_000)
                            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                            .setMinUpdateIntervalMillis(5_000)
                            .setMaxUpdateDelayMillis(5_000)
                            .setMaxUpdates(5_000)
                            .setMaxUpdateAgeMillis(5_000)
                            .build()
                    val identity = Binder.clearCallingIdentity()
                    LocationServices.getFusedLocationProviderClient(context.applicationContext).requestLocationUpdates(locationRequest, it, Looper.getMainLooper())
                    Binder.restoreCallingIdentity(identity)
                }
            } else {
                Log.d(TAG, "startUpdateLocation locationCallback is not null")
            }
        }

        fun stopUpdateLocation() {
            Log.d(TAG, "stopUpdateLocation callback: $locationCallback")
            if (locationCallback != null) {
                Log.w(TAG, "stopUpdateLocation remove location callback")
                LocationServices.getFusedLocationProviderClient(appContext).removeLocationUpdates(locationCallback!!)
                locationCallback = null
            }
        }

        private fun createLocationUpdatesCallback(context: Context) : LocationCallback {
            Log.d(TAG, "createLocationUpdatesCallback: ")
            appContext = context.applicationContext
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult ?: return
                    val location = locationResult.lastLocation
                    Log.d(TAG, "Location update: ${location.latitude}, ${location.longitude}}")
                    if (accountInfo != null && location != null && appContext != null) {
                        val locationSharingEnabled = isLocationSharingEnabled(appContext!!, accountInfo!!.name)
                        if (locationSharingEnabled) {
                            CoroutineScope(Dispatchers.IO).launch {
                                refreshAndUploadLocation(appContext!!, accountInfo!!, location)
                            }
                        } else {
                            Log.w(TAG, "onLocationResult location sharing turn off")
                            LocationServices.getFusedLocationProviderClient(appContext).removeLocationUpdates(locationCallback!!)
                            locationCallback = null
                        }
                    }
                }

                override fun onLocationAvailability(availability: LocationAvailability?) {
                    super.onLocationAvailability(availability)
                }
            }
            return locationCallback
        }
    }
}