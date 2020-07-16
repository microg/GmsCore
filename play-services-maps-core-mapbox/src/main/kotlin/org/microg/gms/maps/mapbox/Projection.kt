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
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.maps.internal.IProjectionDelegate
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.VisibleRegion
import com.mapbox.mapboxsdk.maps.Projection
import org.microg.gms.kotlin.unwrap
import org.microg.gms.maps.mapbox.utils.toGms
import org.microg.gms.maps.mapbox.utils.toMapbox

class ProjectionImpl(private val projection: Projection) : IProjectionDelegate.Stub() {
    private val visibleRegion = projection.visibleRegion
    private val farLeft = projection.toScreenLocation(visibleRegion.farLeft)
    private val farRight = projection.toScreenLocation(visibleRegion.farRight)
    private val nearLeft = projection.toScreenLocation(visibleRegion.nearLeft)
    private val nearRight = projection.toScreenLocation(visibleRegion.nearRight)

    override fun fromScreenLocation(obj: IObjectWrapper?): LatLng? =
            obj.unwrap<Point>()?.let { projection.fromScreenLocation(PointF(it)) }?.toGms()

    override fun toScreenLocation(latLng: LatLng?): IObjectWrapper = try {
        ObjectWrapper.wrap(latLng?.toMapbox()?.let { projection.toScreenLocation(it) }?.let { Point(it.x.toInt(), it.y.toInt()) })
    } catch (e: Exception) {
        ObjectWrapper.wrap(Point(0, 0))
    }

    override fun getVisibleRegion(): VisibleRegion = visibleRegion.toGms()

}