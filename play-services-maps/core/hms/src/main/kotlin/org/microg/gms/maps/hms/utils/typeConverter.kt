/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms.utils

import android.os.Bundle
import android.util.Log
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.internal.ICancelableCallback
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.HuaweiMapOptions
import com.huawei.hms.maps.model.*
import com.google.android.gms.maps.model.CameraPosition as GmsCameraPosition
import com.google.android.gms.maps.model.CircleOptions as GmsCircleOptions
import com.google.android.gms.maps.model.Dash as GmsDash
import com.google.android.gms.maps.model.Dot as GmsDot
import com.google.android.gms.maps.model.Gap as GmsGap
import com.google.android.gms.maps.model.GroundOverlayOptions as GmsGroundOverlayOptions
import com.google.android.gms.maps.model.LatLng as GmsLatLng
import com.google.android.gms.maps.model.LatLngBounds as GmsLatLngBounds
import com.google.android.gms.maps.model.MarkerOptions as GmsMarkerOptions
import com.google.android.gms.maps.model.PatternItem as GmsPatternItem
import com.google.android.gms.maps.model.PolygonOptions as GmsPolygonOptions
import com.google.android.gms.maps.model.PolylineOptions as GmsPolylineOptions
import com.google.android.gms.maps.model.Tile as GmsTile
import com.google.android.gms.maps.model.TileOverlayOptions as GmsTileOverlayOptions
import com.google.android.gms.maps.model.VisibleRegion as GmsVisibleRegion

fun GmsCameraPosition.toHms(): CameraPosition {
    return CameraPosition.Builder().target(target.toHms()).zoom(toHmsZoom(zoom)).tilt(tilt)
        .bearing(toHmsBearing(bearing)).build()
}

fun GmsCircleOptions.toHms(): CircleOptions =
    CircleOptions().center(center.toHms()).clickable(isClickable).fillColor(fillColor)
        .radius(radius).strokeColor(strokeColor).strokeWidth(strokeWidth).visible(isVisible)
        .zIndex(zIndex)

fun GmsPatternItem.toHms(): PatternItem {
    return when (this) {
        is GmsDash -> Dash(length)
        is GmsDot -> Dot()
        is GmsGap -> Gap(length)
        else -> PatternItem(0,0f)
    }
}

fun GoogleMapOptions.toHms(): HuaweiMapOptions {
    val huaweiMapOptions = HuaweiMapOptions()
    camera?.let { huaweiMapOptions.camera(camera?.toHms()) }
    if (maxZoomPreference != 0f) {
        huaweiMapOptions.maxZoomPreference(toHmsZoom(maxZoomPreference))
    }
    if (minZoomPreference != 0f) {
        huaweiMapOptions.minZoomPreference(toHmsZoom(minZoomPreference))
    }
    latLngBoundsForCameraTarget?.let {
        huaweiMapOptions.latLngBoundsForCameraTarget(
            latLngBoundsForCameraTarget?.toHms()
        )
    }

    return huaweiMapOptions
        .compassEnabled(isCompassEnabled)
        .liteMode(liteMode)
//        .mapType(mapType)
        .rotateGesturesEnabled(rotateGesturesEnabled == true)
        .scrollGesturesEnabled(scrollGesturesEnabled == true)
        .tiltGesturesEnabled(tiltGesturesEnabled == true)
        .useViewLifecycleInFragment(useViewLifecycleInFragment == true)
        .zOrderOnTop(zOrderOnTop == true)
        .zoomControlsEnabled(zoomControlsEnabled == true)
        .zoomGesturesEnabled(zoomGesturesEnabled == true)
}

fun GmsLatLng.toHms(): LatLng =
    LatLng(latitude, longitude)

fun GmsLatLngBounds.toHms(): LatLngBounds =
    LatLngBounds(
        LatLng(if(southwest.latitude.isNaN()) 0.0 else southwest.latitude, if(southwest.longitude.isNaN()) 0.0 else southwest.longitude),
        LatLng(if(northeast.latitude.isNaN()) 0.0 else northeast.latitude, if(northeast.longitude.isNaN()) 0.0 else northeast.longitude)
    )

fun ICancelableCallback.toHms(): HuaweiMap.CancelableCallback =
    object : HuaweiMap.CancelableCallback {
        override fun onFinish() = this@toHms.onFinish()
        override fun onCancel() = this@toHms.onCancel()
    }

fun GmsMarkerOptions.toHms(): MarkerOptions {
    val markerOptions = MarkerOptions()
    icon?.let { markerOptions.icon(it.remoteObject.unwrap()) }
    return markerOptions.alpha(alpha).anchorMarker(anchorU, anchorV).draggable(isDraggable)
        .flat(isFlat).infoWindowAnchor(infoWindowAnchorU, infoWindowAnchorV)
        .position(position.toHms()).rotation(rotation).snippet(snippet).title(title)
        .visible(isVisible).zIndex(zIndex)
}

fun GmsGroundOverlayOptions.toHms(): GroundOverlayOptions {
    val groundOverlayOptions = GroundOverlayOptions()
    groundOverlayOptions.anchor(anchorU, anchorV).bearing(bearing)
        .clickable(isClickable)
        .image(image.remoteObject.unwrap())
        .visible(isVisible)
        .zIndex(zIndex)
    if (height > 0) {
        groundOverlayOptions.position(location.toHms(), width, height)
    } else {
        groundOverlayOptions.position(location.toHms(), width)
    }
    bounds?.let { groundOverlayOptions.positionFromBounds(it.toHms()) }
    return groundOverlayOptions
}

fun GmsTileOverlayOptions.toHms(): TileOverlayOptions {
    return TileOverlayOptions().tileProvider(tileProvider?.let { TileProvider { x, y, zoom -> it.getTile(x, y, zoom)?.toHms() } })
        .fadeIn(fadeIn)
        .visible(isVisible)
        .transparency(transparency)
        .zIndex(zIndex)
}

fun GmsTile.toHms(): Tile = Tile(width, height, data)

fun GmsPolygonOptions.toHms(): PolygonOptions {
    val polygonOptions = PolygonOptions()
    holes?.map {
        val hole = it?.map { it?.toHms() }
        polygonOptions.addHole(hole)
    }
    return polygonOptions.addAll(points.map { it.toHms() })
        .clickable(isClickable)
        .fillColor(fillColor)
        .geodesic(isGeodesic)
        .strokeColor(strokeColor).strokeJointType(strokeJointType).strokeWidth(strokeWidth)
        .visible(isVisible)
        .zIndex(zIndex)
}

fun GmsPolylineOptions.toHms(): PolylineOptions {
    val polylineOptions = PolylineOptions()
    polylineOptions.addAll(points.map { it.toHms() })
    return polylineOptions.clickable(isClickable).color(color).geodesic(isGeodesic)
        .jointType(jointType).visible(isVisible).width(toHmsPolylineWidth(width)).zIndex(zIndex)
}

fun toHmsPolylineWidth(gmsWidth: Float): Float = gmsWidth / 3

fun toHmsZoom(gmsZoom: Float?): Float {
    if (gmsZoom == null) {
        return 3f
    }
    if (gmsZoom < 3) {
        return 3f
    } else if (gmsZoom > 18) {
        return 18f
    }
    return gmsZoom
}

fun toHmsBearing(gmsBearing: Float): Float {
    return 360 - gmsBearing
}

fun Bundle.toHms(): Bundle {
    val newBundle = Bundle(this)
    val oldLoader = newBundle.classLoader
    newBundle.classLoader = GmsLatLng::class.java.classLoader
    for (key in newBundle.keySet()) {
        when (val value = newBundle.get(key)) {
            is GmsCameraPosition -> newBundle.putParcelable(key, value.toHms())
            is GmsLatLng -> newBundle.putParcelable(key, value.toHms())
            is GmsLatLngBounds -> newBundle.putParcelable(key, value.toHms())
            is Bundle -> newBundle.putBundle(key, value.toHms())
        }
    }
    newBundle.classLoader = oldLoader
    return newBundle
}

fun CameraPosition.toGms(): GmsCameraPosition =
    GmsCameraPosition(target.toGms(), zoom, tilt, bearing)

fun PatternItem.toGms(): GmsPatternItem = when (this) {
    is Dot -> GmsDot()
    is Dash -> GmsDash(length)
    is Gap  -> GmsGap(length)
    else -> GmsGap(0f)
}

fun LatLng.toGms(): GmsLatLng = GmsLatLng(latitude, longitude)

fun LatLngBounds.toGms(): GmsLatLngBounds = GmsLatLngBounds(
    GmsLatLng(southwest.latitude, southwest.longitude),
    GmsLatLng(northeast.latitude, northeast.longitude)
)

fun VisibleRegion.toGms(): GmsVisibleRegion =
    GmsVisibleRegion(
        nearLeft.toGms(),
        nearRight.toGms(),
        farLeft.toGms(),
        farRight.toGms(),
        latLngBounds.toGms()
    )

fun toGmsPolylineWidth(hmsWidth: Float): Float = hmsWidth * 3

fun Bundle.toGms(): Bundle {
    val newBundle = Bundle(this)
    val oldLoader = newBundle.classLoader
    newBundle.classLoader = LatLng::class.java.classLoader
    for (key in newBundle.keySet()) {
        when (val value = newBundle.get(key)) {
            is CameraPosition -> newBundle.putParcelable(key, value.toGms())
            is LatLng -> newBundle.putParcelable(key, value.toGms())
            is LatLngBounds -> newBundle.putParcelable(key, value.toGms())
            is Bundle -> newBundle.putBundle(key, value.toGms())
        }
    }
    newBundle.classLoader = oldLoader
    return newBundle
}
