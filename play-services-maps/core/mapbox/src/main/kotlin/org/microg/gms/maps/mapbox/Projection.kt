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

package org.microg.gms.maps.mapbox

import android.graphics.Point
import android.graphics.PointF
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.maps.internal.IProjectionDelegate
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import com.mapbox.mapboxsdk.maps.Projection
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.maps.model.LatLngBounds
import org.microg.gms.maps.mapbox.utils.toGms
import org.microg.gms.maps.mapbox.utils.toMapbox
import kotlin.math.roundToInt

val ZERO_LAT_LNG = com.mapbox.mapboxsdk.geometry.LatLng(0.0, 0.0)

// TODO: Do calculations using backed up locations instead of live (which requires UI thread)
class ProjectionImpl(private val projection: Projection, private val withoutTiltOrBearing: Boolean) : IProjectionDelegate.Stub() {
    private val visibleRegion = projection.getVisibleRegion(false)
    private val farLeft = visibleRegion.farLeft?.let { projection.toScreenLocation(it) }
    private val farRight = visibleRegion.farRight?.let { projection.toScreenLocation(it) }
    private val nearLeft = visibleRegion.nearLeft?.let { projection.toScreenLocation(it) }
    private val nearRight = visibleRegion.nearRight?.let { projection.toScreenLocation(it) }

    override fun fromScreenLocation(obj: IObjectWrapper?): LatLng? = try {
        obj.unwrap<Point>()?.let {
            if (withoutTiltOrBearing && farLeft != null && farRight != null && nearLeft != null) {
                val xPercent = (it.x.toFloat() - farLeft.x) / (farRight.x - farLeft.x)
                val yPercent = (it.y.toFloat() - farLeft.y) / (nearLeft.y - farLeft.y)
                val lon = (visibleRegion.farLeft?.longitude ?: 0.0) + xPercent *
                        ((visibleRegion.farRight?.longitude ?: 0.0) - (visibleRegion.farLeft?.longitude ?: 0.0))
                val lat = (visibleRegion.farLeft?.latitude?: 0.0) + yPercent *
                        ((visibleRegion.nearLeft?.latitude?: 0.0) - (visibleRegion.farLeft?.latitude?: 0.0))
                LatLng(lat, lon)
            } else {
                projection.fromScreenLocation(PointF(it)).toGms()
            }
        }
    } catch (e: Exception) {
        Log.d(TAG, "fromScreenLocation() used from outside UI thread on map with tilt or bearing, expect bugs")
        LatLng(0.0, 0.0)
    }

    override fun toScreenLocation(latLng: LatLng?): IObjectWrapper = try {
        ObjectWrapper.wrap(latLng?.toMapbox()?.let {
            if (withoutTiltOrBearing && farLeft != null && farRight != null && nearLeft != null) {
                val xPercent = (it.longitude - (visibleRegion.farLeft?.longitude ?: 0.0)) /
                            ((visibleRegion.farRight?.longitude ?: 0.0) - (visibleRegion.farLeft?.longitude ?: 0.0))
                val yPercent = (it.latitude - (visibleRegion.farLeft?.latitude ?: 0.0)) /
                        ((visibleRegion.nearLeft?.latitude ?: 0.0) - (visibleRegion.farLeft?.latitude ?: 0.0))
                val x = farLeft.x + xPercent * (farRight.x - farLeft.x)
                val y = farLeft.y + yPercent * (nearLeft.y - farLeft.y)
                Point(x.roundToInt(), y.roundToInt()).also { p -> Log.d(TAG, "$p vs.\n${projection.toScreenLocation(it).let { Point(it.x.roundToInt(), it.y.roundToInt()) }}") }
            } else {
                projection.toScreenLocation(it).let { Point(it.x.roundToInt(), it.y.roundToInt()) }
            }
        })
    } catch (e: Exception) {
        Log.d(TAG, "toScreenLocation() used from outside UI thread on map with tilt or bearing, expect bugs")
        ObjectWrapper.wrap(Point(0, 0))
    }

    override fun getVisibleRegion(): VisibleRegion = visibleRegion.toGms()

    companion object {
        private val TAG = "GmsMapProjection"
    }
}

class LiteProjection(private val snapshot: MetaSnapshot) : IProjectionDelegate.Stub() {

    private fun fromScreenLocationAfterPadding(point: Point?): LatLng =
        point?.let { snapshot.latLngForPixelFixed(PointF(point)).toGms() } ?: LatLng(0.0, 0.0)

    override fun fromScreenLocation(obj: IObjectWrapper?): LatLng = fromScreenLocationAfterPadding(obj.unwrap<Point?>()?.let {
        Point((it.x - snapshot.paddingRight), (it.y - snapshot.paddingRight))
    })

    override fun toScreenLocation(latLng: LatLng?): IObjectWrapper =
        ObjectWrapper.wrap(snapshot.snapshot.pixelForLatLng(latLng?.toMapbox()).let {
            Point(it.x.roundToInt() + snapshot.paddingRight, it.y.roundToInt() + snapshot.paddingTop)
        })

    override fun getVisibleRegion(): VisibleRegion {
        val nearLeft = fromScreenLocationAfterPadding(Point(0, snapshot.height))
        val nearRight = fromScreenLocationAfterPadding(Point(snapshot.width, snapshot.height))
        val farLeft = fromScreenLocationAfterPadding(Point(0, 0))
        val farRight = fromScreenLocationAfterPadding(Point(snapshot.width, 0))

        return VisibleRegion(nearLeft, nearRight, farLeft, farRight, LatLngBounds(nearLeft, farRight))
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

    companion object {
        private val TAG = "GmsMapDummyProjection"
    }
}