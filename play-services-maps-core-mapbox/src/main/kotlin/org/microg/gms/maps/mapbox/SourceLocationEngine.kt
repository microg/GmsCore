/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.mapbox

import android.app.PendingIntent
import android.location.Location
import android.os.Handler
import android.os.Looper
import com.google.android.gms.maps.internal.ILocationSourceDelegate
import com.google.android.gms.maps.internal.IOnLocationChangeListener
import com.mapbox.mapboxsdk.location.engine.LocationEngine
import com.mapbox.mapboxsdk.location.engine.LocationEngineCallback
import com.mapbox.mapboxsdk.location.engine.LocationEngineRequest
import com.mapbox.mapboxsdk.location.engine.LocationEngineResult

class SourceLocationEngine(private val locationSource: ILocationSourceDelegate) : LocationEngine, IOnLocationChangeListener.Stub() {
    val callbacks: MutableSet<Pair<LocationEngineCallback<LocationEngineResult>, Handler>> = hashSetOf()
    var lastLocation: Location? = null

    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) {
        callback.onSuccess(LocationEngineResult.create(lastLocation))
    }

    override fun requestLocationUpdates(request: LocationEngineRequest, callback: LocationEngineCallback<LocationEngineResult>, looper: Looper?) {
        callbacks.add(callback to Handler(looper ?: Looper.myLooper() ?: Looper.getMainLooper()))
        if (callbacks.size == 1) {
            locationSource.activate(this)
        }
    }

    override fun requestLocationUpdates(request: LocationEngineRequest, pendingIntent: PendingIntent?) {
        throw UnsupportedOperationException()
    }

    override fun removeLocationUpdates(callback: LocationEngineCallback<LocationEngineResult>) {
        callbacks.removeAll { it.first == callback }
        if (callbacks.isEmpty()) {
            locationSource.deactivate()
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