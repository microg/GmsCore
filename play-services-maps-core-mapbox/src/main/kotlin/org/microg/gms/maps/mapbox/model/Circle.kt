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

package org.microg.gms.maps.mapbox.model

import android.os.Parcel
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.internal.ICircleDelegate
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.plugins.annotation.*
import com.mapbox.mapboxsdk.utils.ColorUtils
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfConstants.UNIT_METERS
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMeta
import com.mapbox.turf.TurfTransformation
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.maps.mapbox.LiteGoogleMapImpl
import org.microg.gms.maps.mapbox.utils.toPoint
import org.microg.gms.maps.mapbox.getName
import org.microg.gms.maps.mapbox.makeBitmap
import com.google.android.gms.maps.model.CircleOptions as GmsCircleOptions

val NORTH_POLE: Point = Point.fromLngLat(0.0, 90.0)
val SOUTH_POLE: Point = Point.fromLngLat(0.0, -90.0)

/**
 * Amount of points to be used in the polygon that approximates the circle.
 */
const val CIRCLE_POLYGON_STEPS = 256

abstract class AbstractCircle(
    private val id: String, options: GmsCircleOptions, private val dpiFactor: Function0<Float>
) : ICircleDelegate.Stub() {

    internal var center: LatLng = options.center
    internal var radiusInMeters: Double = options.radius // unlike MapLibre's circles, which only work with pixel radii
    internal var strokeWidth: Float = options.strokeWidth
    internal var strokeColor: Int = options.strokeColor
    internal var fillColor: Int = options.fillColor
    internal var visible: Boolean = options.isVisible
    internal var clickable: Boolean = options.isClickable
    internal var strokePattern: MutableList<PatternItem>? = options.strokePattern
    internal var tag: Any? = null

    internal val line: Markup<Line, LineOptions> = object : Markup<Line, LineOptions> {
        override var annotation: Line? = null
        override val annotationOptions: LineOptions
            get() = LineOptions()
                .withGeometry(
                    LineString.fromLngLats(
                        makeOutlineLatLngs()
                    )
                ).withLineWidth(strokeWidth / dpiFactor())
                .withLineColor(ColorUtils.colorToRgbaString(strokeColor))
                .withLineOpacity(if (visible) 1f else 0f)
                .apply {
                    strokePattern?.let {
                        withLinePattern(it.getName(strokeColor, strokeWidth))
                    }
                }

        override var removed: Boolean = false
    }

    val annotationOptions: FillOptions
        get() =
            FillOptions()
                .withGeometry(makePolygon())
                .withFillColor(ColorUtils.colorToRgbaString(fillColor))
                .withFillOpacity(if (visible && !wrapsAroundPoles()) 1f else 0f)

    internal abstract fun update()

    internal fun makePolygon() = TurfTransformation.circle(
        Point.fromLngLat(center.longitude, center.latitude), radiusInMeters, CIRCLE_POLYGON_STEPS, TurfConstants.UNIT_METERS
    )

    /**
     * Google's "map renderer is unable to draw the circle fill if the circle encompasses
     * either the North or South pole" (though it does so incorrectly anyway)
     */
    internal fun wrapsAroundPoles() = center.toPoint().let {
        TurfMeasurement.distance(
            it, NORTH_POLE, UNIT_METERS
        ) < radiusInMeters || TurfMeasurement.distance(
            it, SOUTH_POLE, UNIT_METERS
        ) < radiusInMeters
    }

    internal fun makeOutlineLatLngs(): MutableList<Point> {
        val pointList = TurfMeta.coordAll(
            makePolygon(), wrapsAroundPoles()
        )
        // Circles around the poles are tricky to draw (https://github.com/mapbox/mapbox-gl-js/issues/11235).
        // We modify our lines such to match the way Mapbox / MapLibre draws them.
        // This results in a small gap somewhere in the line, but avoids an incorrect horizontal line.

        val centerPoint = center.toPoint()

        if (!centerPoint.equals(NORTH_POLE) && TurfMeasurement.distance(centerPoint, NORTH_POLE, UNIT_METERS) < radiusInMeters) {
            // Wraps around North Pole
            for (i in 0 until pointList.size) {
                // We want to have the north-most points at the start and end
                if (pointList[0].latitude() > pointList[1].latitude() && pointList[pointList.size - 1].latitude() > pointList[pointList.size - 2].latitude()) {
                    return pointList
                } else {
                    // Cycle point list
                    val zero = pointList.removeFirst()
                    pointList.add(zero)
                }
            }
        }

        if (!centerPoint.equals(SOUTH_POLE) && TurfMeasurement.distance(centerPoint, SOUTH_POLE, UNIT_METERS) < radiusInMeters) {
            // Wraps around South Pole
            for (i in 0 until pointList.size) {
                // We want to have the south-most points at the start and end
                if (pointList[0].latitude() < pointList[1].latitude() && pointList[pointList.size - 1].latitude() < pointList[pointList.size - 2].latitude()) {
                    return pointList
                } else {
                    // Cycle point list
                    val last = pointList.removeAt(pointList.size - 1)
                    pointList.add(0, last)
                }
            }
        }

        // In this case no changes were made
        return pointList
    }

    override fun getId(): String = id

    override fun setCenter(center: LatLng) {
        this.center = center
        update()

    }

    override fun getCenter(): LatLng = center

    override fun setRadius(radius: Double) {
        this.radiusInMeters = radius
        update()
    }

    override fun getRadius(): Double = radiusInMeters

    override fun setStrokeWidth(width: Float) {
        this.strokeWidth = width
        update()
    }

    override fun getStrokeWidth(): Float = strokeWidth

    override fun setStrokeColor(color: Int) {
        this.strokeColor = color
        update()
    }

    override fun getStrokeColor(): Int = strokeColor

    override fun setFillColor(color: Int) {
        this.fillColor = color
        update()
    }

    override fun getFillColor(): Int = fillColor

    override fun setZIndex(zIndex: Float) {
        Log.d(TAG, "unimplemented Method: setZIndex")
    }

    override fun getZIndex(): Float {
        Log.d(TAG, "unimplemented Method: getZIndex")
        return 0f
    }

    override fun setVisible(visible: Boolean) {
        this.visible = visible
        update()
    }

    override fun isVisible(): Boolean = visible

    override fun equalsRemote(other: ICircleDelegate?): Boolean = equals(other)

    override fun hashCodeRemote(): Int = hashCode()

    override fun setClickable(clickable: Boolean) {
        this.clickable = clickable
    }

    override fun isClickable(): Boolean {
        return clickable
    }

    override fun setStrokePattern(pattern: MutableList<PatternItem>?) {
        this.strokePattern = pattern
        update()
    }


    override fun getStrokePattern(): MutableList<PatternItem>? {
        return strokePattern
    }

    override fun setTag(o: IObjectWrapper) {
        this.tag = o.unwrap()
    }

    override fun getTag(): IObjectWrapper = ObjectWrapper.wrap(tag)

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (other is CircleImpl) {
            return other.id == id
        }
        return false
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        if (super.onTransact(code, data, reply, flags)) {
            true
        } else {
            Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
        }

    companion object {
        val TAG = "GmsMapAbstractCircle"
    }
}

class CircleImpl(private val map: GoogleMapImpl, private val id: String, options: GmsCircleOptions) :
    AbstractCircle(id, options, { map.dpiFactor }), Markup<Fill, FillOptions> {

    override var annotation: Fill? = null
    override var removed: Boolean = false

    override fun update() {
        val polygon = makePolygon()

        // Extracts points from generated polygon in expected format
        annotation?.let {
            it.latLngs = FillOptions().withGeometry(polygon).latLngs
            it.setFillColor(fillColor)
            it.fillOpacity = if (visible && !wrapsAroundPoles()) 1f else 0f
        }

        line.annotation?.let {
            it.latLngs = makeOutlineLatLngs().map { point ->
                com.mapbox.mapboxsdk.geometry.LatLng(
                    point.latitude(),
                    point.longitude()
                )
            }

            it.lineWidth = strokeWidth / map.dpiFactor

            (strokePattern ?: emptyList()).let { pattern ->
                val bitmapName = pattern.getName(strokeColor, strokeWidth)
                map.addBitmap(bitmapName, pattern.makeBitmap(strokeColor, strokeWidth))
                line.annotation?.linePattern = bitmapName
            }
            map.lineManager?.let { line.update(it) }

            it.setLineColor(strokeColor)
        }

        map.fillManager?.let { update(it) }
        map.lineManager?.let { line.update(it) }
    }

    override fun remove() {
        removed = true
        line.removed = true
        map.fillManager?.let { update(it) }
        map.lineManager?.let { line.update(it) }
    }


    override fun update(manager: AnnotationManager<*, Fill, FillOptions, *, *, *>) {
        synchronized(this) {
            val id = annotation?.id
            if (removed && id != null) {
                map.circles.remove(id)
            }
            super.update(manager)
            val annotation = annotation
            if (annotation != null && id == null) {
                map.circles[annotation.id] = this
            }
        }
    }

    companion object {
        val TAG = "GmsMapCircle"
    }
}

class LiteCircleImpl(private val map: LiteGoogleMapImpl, id: String, options: GmsCircleOptions) :
    AbstractCircle(id, options, { map.dpiFactor }) {
    override fun update() {
        map.postUpdateSnapshot()
    }

    override fun remove() {
        map.circles.remove(this)
    }

}