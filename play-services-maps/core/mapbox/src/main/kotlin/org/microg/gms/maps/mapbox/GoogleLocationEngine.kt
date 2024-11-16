/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.mapbox

import android.app.PendingIntent
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mapbox.mapboxsdk.location.engine.LocationEngine
import com.mapbox.mapboxsdk.location.engine.LocationEngineCallback
import com.mapbox.mapboxsdk.location.engine.LocationEngineRequest
import com.mapbox.mapboxsdk.location.engine.LocationEngineResult

class GoogleLocationEngine(context: Context) : LocationEngine {
    private val listenerMap: MutableMap<LocationEngineCallback<LocationEngineResult>, LocationListener> = hashMapOf()
    private val client = LocationServices.getFusedLocationProviderClient(context)

    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) {
        client.lastLocation.addOnCompleteListener {
            if (it.isSuccessful) callback.onSuccess(LocationEngineResult.create(it.result))
            else callback.onFailure(it.exception)
        }
    }

    override fun requestLocationUpdates(request: LocationEngineRequest, callback: LocationEngineCallback<LocationEngineResult>, looper: Looper?) {
        listenerMap[callback] = listenerMap[callback] ?: LocationListener { callback.onSuccess(LocationEngineResult.create(it)) }
        client.requestLocationUpdates(
            LocationRequest.Builder(request.interval)
                .setPriority(
                    when (request.priority) {
                        LocationEngineRequest.PRIORITY_HIGH_ACCURACY -> Priority.PRIORITY_HIGH_ACCURACY
                        LocationEngineRequest.PRIORITY_BALANCED_POWER_ACCURACY -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
                        LocationEngineRequest.PRIORITY_LOW_POWER -> Priority.PRIORITY_LOW_POWER
                        LocationEngineRequest.PRIORITY_NO_POWER -> Priority.PRIORITY_PASSIVE
                        else -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
                    }
                )
                .setMinUpdateDistanceMeters(request.displacement)
                .setMinUpdateIntervalMillis(request.fastestInterval)
                .setMaxUpdateDelayMillis(request.maxWaitTime)
                .build(), listenerMap[callback]!!, looper
        )
    }

    override fun requestLocationUpdates(request: LocationEngineRequest, pendingIntent: PendingIntent?) {
        throw UnsupportedOperationException()
    }

    override fun removeLocationUpdates(callback: LocationEngineCallback<LocationEngineResult>) {
        listenerMap[callback]?.let { client.removeLocationUpdates(it) }
        listenerMap.remove(callback)
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent?) {
        throw UnsupportedOperationException()
    }
}