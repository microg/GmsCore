/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.mapbox

import android.app.PendingIntent
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.maps.internal.ILocationSourceDelegate
import com.google.android.gms.maps.internal.IOnLocationChangeListener
import com.mapbox.mapboxsdk.location.engine.LocationEngine
import com.mapbox.mapboxsdk.location.engine.LocationEngineCallback
import com.mapbox.mapboxsdk.location.engine.LocationEngineRequest
import com.mapbox.mapboxsdk.location.engine.LocationEngineResult

class SourceLocationEngine(private val locationSource: ILocationSourceDelegate) : LocationEngine, IOnLocationChangeListener.Stub() {
    val callbacks: MutableSet<Pair<LocationEngineCallback<LocationEngineResult>, Handler>> = hashSetOf()
    var lastLocation: Location? = null
    var active: Boolean = false

    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) {
        callback.onSuccess(LocationEngineResult.create(lastLocation))
    }

    override fun requestLocationUpdates(request: LocationEngineRequest, callback: LocationEngineCallback<LocationEngineResult>, looper: Looper?) {
        callbacks.add(callback to Handler(looper ?: Looper.myLooper() ?: Looper.getMainLooper()))
        if (!active) {
            active = true
            try {
                locationSource.activate(this)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun requestLocationUpdates(request: LocationEngineRequest, pendingIntent: PendingIntent?) {
        throw UnsupportedOperationException()
    }

    override fun removeLocationUpdates(callback: LocationEngineCallback<LocationEngineResult>) {
        callbacks.removeAll { it.first == callback }
        if (callbacks.isEmpty() && active) {
            active = false
            try {
                locationSource.deactivate()
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent?) {
        throw UnsupportedOperationException()
    }

    override fun onLocationChanged(location: Location?) {
        lastLocation = location
        for ((callback, handler) in callbacks) {
            handler.post {
                callback.onSuccess(LocationEngineResult.create(location))
            }
        }
    }
}