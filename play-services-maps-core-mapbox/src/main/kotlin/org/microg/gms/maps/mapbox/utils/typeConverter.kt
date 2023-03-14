/*
 * Copyright (C) 2019 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.maps.mapbox.utils

import android.os.Bundle
import com.google.android.gms.maps.internal.ICancelableCallback
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.geometry.VisibleRegion
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.google.android.gms.maps.model.CameraPosition as GmsCameraPosition
import com.google.android.gms.maps.model.LatLng as GmsLatLng
import com.google.android.gms.maps.model.LatLngBounds as GmsLatLngBounds
import com.google.android.gms.maps.model.VisibleRegion as GmsVisibleRegion

fun GmsLatLng.toMapbox(): LatLng =
        LatLng(latitude, longitude)

fun GmsLatLng.toPoint() = Point.fromLngLat(latitude, longitude)

fun GmsLatLngBounds.toMapbox(): LatLngBounds =
        LatLngBounds.from(this.northeast.latitude, this.northeast.longitude, this.southwest.latitude, this.southwest.longitude)

fun GmsCameraPosition.toMapbox(): CameraPosition =
        CameraPosition.Builder()
                .target(target.toMapbox())
                .zoom(zoom.toDouble() - 1.0)
                .tilt(tilt.toDouble())
                .bearing(bearing.toDouble())
                .build()

fun ICancelableCallback.toMapbox(): MapboxMap.CancelableCallback =
        object : MapboxMap.CancelableCallback {
            override fun onFinish() = this@toMapbox.onFinish()
            override fun onCancel() = this@toMapbox.onCancel()
        }


fun Bundle.toMapbox(): Bundle {
    val newBundle = Bundle(this)
    val oldLoader = newBundle.classLoader
    newBundle.classLoader = GmsLatLng::class.java.classLoader
    for (key in newBundle.keySet()) {
        val value = newBundle.get(key)
        when (value) {
            is GmsCameraPosition -> newBundle.putParcelable(key, value.toMapbox())
            is GmsLatLng -> newBundle.putParcelable(key, value.toMapbox())
            is GmsLatLngBounds -> newBundle.putParcelable(key, value.toMapbox())
            is Bundle -> newBundle.putBundle(key, value.toMapbox())
        }
    }
    newBundle.classLoader = oldLoader
    return newBundle
}

fun LatLng.toGms(): GmsLatLng = GmsLatLng(latitude, longitude)

fun LatLng.toPoint(): Point = Point.fromLngLat(latitude, longitude)

fun LatLngBounds.toGms(): GmsLatLngBounds = GmsLatLngBounds(southWest.toGms(), northEast.toGms())

fun CameraPosition.toGms(): GmsCameraPosition =
        GmsCameraPosition(target?.toGms(), zoom.toFloat() + 1.0f, tilt.toFloat(), bearing.toFloat())

fun Bundle.toGms(): Bundle {
    val newBundle = Bundle(this)
    val oldLoader = newBundle.classLoader
    newBundle.classLoader = LatLng::class.java.classLoader
    for (key in newBundle.keySet()) {
        val value = newBundle.get(key)
        when (value) {
            is CameraPosition -> newBundle.putParcelable(key, value.toGms())
            is LatLng -> newBundle.putParcelable(key, value.toGms())
            is LatLngBounds -> newBundle.putParcelable(key, value.toGms())
            is Bundle -> newBundle.putBundle(key, value.toGms())
        }
    }
    newBundle.classLoader = oldLoader
    return newBundle
}

fun VisibleRegion.toGms(): GmsVisibleRegion =
        GmsVisibleRegion(nearLeft?.toGms(), nearRight?.toGms(), farLeft?.toGms(), farRight?.toGms(), latLngBounds.toGms())
