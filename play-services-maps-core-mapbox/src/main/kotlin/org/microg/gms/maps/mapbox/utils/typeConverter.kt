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
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.geometry.VisibleRegion
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_TOP_LEFT
import com.mapbox.mapboxsdk.utils.ColorUtils
import org.microg.gms.kotlin.unwrap
import org.microg.gms.maps.mapbox.model.BitmapDescriptorImpl

fun com.google.android.gms.maps.model.LatLng.toMapbox(): LatLng =
        LatLng(latitude, longitude)

fun com.google.android.gms.maps.model.LatLngBounds.toMapbox(): LatLngBounds =
        LatLngBounds.from(this.northeast.latitude, this.northeast.longitude, this.southwest.latitude, this.southwest.longitude)

fun com.google.android.gms.maps.model.CameraPosition.toMapbox(): CameraPosition =
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
    for (key in newBundle.keySet()) {
        val value = newBundle.get(key)
        when (value) {
            is com.google.android.gms.maps.model.CameraPosition -> newBundle.putParcelable(key, value.toMapbox())
            is com.google.android.gms.maps.model.LatLng -> newBundle.putParcelable(key, value.toMapbox())
            is com.google.android.gms.maps.model.LatLngBounds -> newBundle.putParcelable(key, value.toMapbox())
            is Bundle -> newBundle.putBundle(key, value.toMapbox())
        }
    }
    return newBundle
}

fun LatLng.toGms(): com.google.android.gms.maps.model.LatLng =
        com.google.android.gms.maps.model.LatLng(latitude, longitude)

fun LatLngBounds.toGms(): com.google.android.gms.maps.model.LatLngBounds =
        com.google.android.gms.maps.model.LatLngBounds(southWest.toGms(), northEast.toGms())

fun CameraPosition.toGms(): com.google.android.gms.maps.model.CameraPosition =
        com.google.android.gms.maps.model.CameraPosition(target.toGms(), zoom.toFloat() + 1.0f, tilt.toFloat(), bearing.toFloat())

fun Bundle.toGms(): Bundle {
    val newBundle = Bundle(this)
    for (key in newBundle.keySet()) {
        val value = newBundle.get(key)
        when (value) {
            is CameraPosition -> newBundle.putParcelable(key, value.toGms())
            is LatLng -> newBundle.putParcelable(key, value.toGms())
            is LatLngBounds -> newBundle.putParcelable(key, value.toGms())
            is Bundle -> newBundle.putBundle(key, value.toGms())
        }
    }
    return newBundle
}

fun VisibleRegion.toGms(): com.google.android.gms.maps.model.VisibleRegion =
        com.google.android.gms.maps.model.VisibleRegion(nearLeft.toGms(), nearRight.toGms(), farLeft.toGms(), farRight.toGms(), latLngBounds.toGms())