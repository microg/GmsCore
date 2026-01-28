/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms

import android.graphics.Point
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.maps.internal.IProjectionDelegate
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.VisibleRegion
import com.huawei.hms.maps.Projection
import com.huawei.hms.maps.model.CameraPosition
import org.microg.gms.maps.hms.utils.toGms
import org.microg.gms.maps.hms.utils.toHms
import kotlin.math.roundToInt

private const val TAG = "GmsProjectionImpl"

class ProjectionImpl(private var projection: Projection, private var withoutTiltOrBearing: Boolean) : IProjectionDelegate.Stub() {
    private var lastVisibleRegion: VisibleRegion? = null
    private var visibleRegion = projection.visibleRegion

    private var farLeft: Point? = visibleRegion.farLeft?.let { projection.toScreenLocation(it) }
    private var farRight: Point? = visibleRegion.farRight?.let { projection.toScreenLocation(it) }
    private var nearLeft: Point? = visibleRegion.nearLeft?.let { projection.toScreenLocation(it) }

    private var farLeftLat = visibleRegion.farLeft?.latitude ?: 0.0
    private var nearLeftLat = visibleRegion.nearLeft?.latitude ?: 0.0
    private var farLeftLng = visibleRegion.farLeft?.longitude ?: 0.0
    private var farRightLng = visibleRegion.farRight?.longitude ?: 0.0
    private var farLeftX = farLeft?.x ?: 0
    private var farLeftY = farLeft?.y ?: 0
    private var farRightX = farRight?.x ?: (farLeftX + 1)
    private var nearLeftY = nearLeft?.y ?: (farLeftY + 1)

    fun updateProjectionState(cameraPosition: CameraPosition, projection: Projection) {
        val tilt = cameraPosition.tilt
        val bearing = cameraPosition.bearing
        val useFast = tilt < 1f && (bearing % 360f < 1f || bearing % 360f > 359f)
        Log.d(TAG, "updateProjectionState: useFastMode: $useFast")

        visibleRegion = projection.visibleRegion
        withoutTiltOrBearing = useFast

        farLeft = visibleRegion.farLeft?.let { projection.toScreenLocation(it) }
        farRight = visibleRegion.farRight?.let { projection.toScreenLocation(it) }
        nearLeft = visibleRegion.nearLeft?.let { projection.toScreenLocation(it) }

        farLeftLat = visibleRegion.farLeft?.latitude ?: 0.0
        nearLeftLat = visibleRegion.nearLeft?.latitude ?: 0.0
        farLeftLng = visibleRegion.farLeft?.longitude ?: 0.0
        farRightLng = visibleRegion.farRight?.longitude ?: 0.0
        farLeftX = farLeft?.x ?: 0
        farLeftY = farLeft?.y ?: 0
        farRightX = farRight?.x ?: (farLeftX + 1)
        nearLeftY = nearLeft?.y ?: (farLeftY + 1)
    }

    private fun isInvalid(): Boolean {
        return farLeftX == farRightX || farLeftY == nearLeftY || (farRightX == 1 && farLeftX == 0) || (nearLeftY == 1 && farLeftY == 0)
    }

    override fun fromScreenLocation(obj: IObjectWrapper?): LatLng? = try {
        obj.unwrap<Point>()?.let {
            if (withoutTiltOrBearing && farLeft != null && farRight != null && nearLeft != null) {
                if (isInvalid()) {
                    Log.w(TAG, "Invalid projection layout, fallback to SDK")
                    projection.fromScreenLocation(Point(it)).toGms()
                } else {
                    val xPercent = (it.x.toFloat() - farLeftX) / (farRightX - farLeftX)
                    val yPercent = (it.y.toFloat() - farLeftY) / (nearLeftY - farLeftY)

                    val lon = farLeftLng + xPercent * (farRightLng - farLeftLng)
                    val lat = farLeftLat + yPercent * (nearLeftLat - farLeftLat)

                    Log.d(TAG, "fromScreenLocation: $it -> lat: $lat lon: $lon")

                    LatLng(lat, lon)
                }
            } else {
                projection.fromScreenLocation(Point(it)).toGms()
            }
        }
    } catch (e: Exception) {
        Log.d(TAG, "fromScreenLocation() error", e)
        LatLng(0.0, 0.0)
    }

    override fun toScreenLocation(latLng: LatLng?): IObjectWrapper = try {
        ObjectWrapper.wrap(latLng?.toHms()?.let {
            if (withoutTiltOrBearing && farLeft != null && farRight != null && nearLeft != null) {
                if (isInvalid()) {
                    Log.w(TAG, "Invalid projection layout, fallback to SDK")
                    projection.toScreenLocation(it).let { p -> Point(p.x, p.y) }
                } else {
                    val xPercent = (it.longitude - farLeftLng) / (farRightLng - farLeftLng)
                    val yPercent = (it.latitude - farLeftLat) / (nearLeftLat - farLeftLat)

                    val x = farLeftX + xPercent * (farRightX - farLeftX)
                    val y = farLeftY + yPercent * (nearLeftY - farLeftY)

                    Log.d(TAG, "toScreenLocation: $latLng -> x: $x y: $y")

                    Point(x.roundToInt(), y.roundToInt())
                }
            } else {
                projection.toScreenLocation(it).let { p -> Point(p.x, p.y) }
            }
        })
    } catch (e: Exception) {
        Log.d(TAG, "toScreenLocation() error", e)
        ObjectWrapper.wrap(Point(0, 0))
    }

    override fun getVisibleRegion(): VisibleRegion? {
        if (visibleRegion.farLeft.latitude.isNaN() || visibleRegion.farLeft.longitude.isNaN()) {
            return lastVisibleRegion
        }
        lastVisibleRegion = visibleRegion.toGms()
        Log.d(TAG, "getVisibleRegion: $visibleRegion")
        return lastVisibleRegion
    }
}

class DummyProjection : IProjectionDelegate.Stub() {
    override fun fromScreenLocation(obj: IObjectWrapper?): LatLng {
        Log.d(TAG, "Map not initialized when calling getProjection(). Cannot calculate fromScreenLocation")
        return LatLng(0.0, 0.0)
    }

    override fun toScreenLocation(latLng: LatLng?): IObjectWrapper {
        Log.d(TAG, "Map not initialized when calling getProjection(). Cannot calculate toScreenLocation")
        return ObjectWrapper.wrap(Point(0, 0))
    }

    override fun getVisibleRegion(): VisibleRegion {
        Log.d(TAG, "Map not initialized when calling getProjection(). Cannot calculate getVisibleRegion")
        return VisibleRegion(LatLngBounds(LatLng(0.0, 0.0), LatLng(0.0, 0.0)))
    }
}