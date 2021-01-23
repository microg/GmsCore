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
import org.microg.gms.maps.mapbox.utils.toGms
import org.microg.gms.maps.mapbox.utils.toMapbox
import kotlin.math.roundToInt

// TODO: Do calculations using backed up locations instead of live (which requires UI thread)
class ProjectionImpl(private val projection: Projection, private val withoutTiltOrBearing: Boolean) : IProjectionDelegate.Stub() {
    private val visibleRegion = projection.visibleRegion
    private val farLeft = projection.toScreenLocation(visibleRegion.farLeft)
    private val farRight = projection.toScreenLocation(visibleRegion.farRight)
    private val nearLeft = projection.toScreenLocation(visibleRegion.nearLeft)
    private val nearRight = projection.toScreenLocation(visibleRegion.nearRight)

    override fun fromScreenLocation(obj: IObjectWrapper?): LatLng? = try {
        obj.unwrap<Point>()?.let {
            if (withoutTiltOrBearing) {
                val xPercent = (it.x.toFloat() - farLeft.x) / (farRight.x - farLeft.x)
                val yPercent = (it.y.toFloat() - farLeft.y) / (nearLeft.y - farLeft.y)
                val lon = visibleRegion.farLeft.longitude + xPercent * (visibleRegion.farRight.longitude - visibleRegion.farLeft.longitude)
                val lat = visibleRegion.farLeft.latitude + yPercent * (visibleRegion.nearLeft.latitude - visibleRegion.farLeft.latitude)
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
            if (withoutTiltOrBearing) {
                val xPercent = (it.longitude - visibleRegion.farLeft.longitude) / (visibleRegion.farRight.longitude - visibleRegion.farLeft.longitude)
                val yPercent = (it.latitude - visibleRegion.farLeft.latitude) / (visibleRegion.nearLeft.latitude - visibleRegion.farLeft.latitude)
                val x = farLeft.x + xPercent * (farRight.x - farLeft.x)
                val y = farLeft.y + yPercent * (nearLeft.y - farLeft.y)
                Point(x.roundToInt(), y.roundToInt())
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
