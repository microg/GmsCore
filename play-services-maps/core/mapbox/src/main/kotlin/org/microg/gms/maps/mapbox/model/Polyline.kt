/*
 * SPDX-FileCopyrightText: 2019 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.mapbox.model

import android.os.Parcel
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.maps.model.Cap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.internal.IPolylineDelegate
import com.mapbox.mapboxsdk.plugins.annotation.Line
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions
import com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_BEVEL
import com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_MITER
import com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND
import com.mapbox.mapboxsdk.utils.ColorUtils
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.maps.mapbox.LiteGoogleMapImpl
import org.microg.gms.maps.mapbox.model.AnnotationType.LINE
import org.microg.gms.maps.mapbox.utils.toMapbox
import org.microg.gms.utils.warnOnTransactionIssues
import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import com.google.android.gms.maps.model.PolylineOptions as GmsLineOptions
import com.mapbox.mapboxsdk.geometry.LatLng as MapboxLatLng

abstract class AbstractPolylineImpl(private val id: String, options: GmsLineOptions, private val dpiFactor: Function0<Float>) : IPolylineDelegate.Stub() {
    internal var points: List<LatLng> = ArrayList(options.points)
    internal var width = options.width
    internal var jointType = options.jointType
    internal var pattern = ArrayList(options.pattern.orEmpty())
    internal var color = options.color
    internal var visible: Boolean = options.isVisible
    internal var clickable: Boolean = options.isClickable
    internal var tag: IObjectWrapper? = null
    internal var startCap: Cap = options.startCap
    internal var endCap: Cap = options.endCap
    internal var geodesic = options.isGeodesic
    internal var zIndex = options.zIndex
    internal var spans = options.spans

    val baseAnnotationOptions: LineOptions
        get() = LineOptions()
            .withLineJoin(when (jointType) {
                JointType.BEVEL -> LINE_JOIN_BEVEL
                JointType.DEFAULT -> LINE_JOIN_MITER
                else -> LINE_JOIN_ROUND
            })
            .withLineWidth(width / dpiFactor.invoke())
            .withLineColor(ColorUtils.colorToRgbaString(color))
            .withLineOpacity(if (visible) 1f else 0f)

    internal abstract fun update()

    override fun getId(): String = id

    override fun setPoints(points: List<LatLng>) {
        this.points = ArrayList(points)
        update()
    }

    override fun getPoints(): List<LatLng> = points

    override fun setWidth(width: Float) {
        this.width = width
        update()
    }

    override fun getWidth(): Float = width

    override fun setColor(color: Int) {
        this.color = color
        update()
    }

    override fun getColor(): Int = color

    override fun setZIndex(zIndex: Float) {
        this.zIndex = zIndex
    }

    override fun getZIndex(): Float = zIndex

    override fun setVisible(visible: Boolean) {
        this.visible = visible
        update()
    }

    override fun isVisible(): Boolean = visible

    override fun setGeodesic(geod: Boolean) {
        this.geodesic = geod
        update()
    }

    override fun isGeodesic(): Boolean = geodesic

    override fun setStartCap(startCap: Cap) {
        this.startCap = startCap
    }

    override fun getStartCap(): Cap = startCap

    override fun setEndCap(endCap: Cap) {
        this.endCap = endCap
    }

    override fun getEndCap(): Cap = endCap

    override fun equalsRemote(other: IPolylineDelegate?): Boolean = equals(other)

    override fun hashCodeRemote(): Int = hashCode()

    override fun setClickable(clickable: Boolean) {
        this.clickable = clickable
    }

    override fun isClickable(): Boolean = clickable

    override fun setJointType(jointType: Int) {
        this.jointType = jointType
        update()
    }

    override fun getJointType(): Int = jointType

    override fun setPattern(pattern: MutableList<PatternItem>?) {
        this.pattern = ArrayList(pattern.orEmpty())
    }

    override fun getPattern(): MutableList<PatternItem> = pattern

    override fun setTag(tag: IObjectWrapper?) {
        this.tag = tag
    }

    override fun getTag(): IObjectWrapper = tag ?: ObjectWrapper.wrap(null)

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is AbstractPolylineImpl) {
            return other.id == id
        }
        return false
    }

    override fun toString(): String {
        return id
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }

    companion object {
        const val TAG = "GmsPolylineAbstract"
    }
}

class PolylineImpl(private val map: GoogleMapImpl, id: String, options: GmsLineOptions) :
    AbstractPolylineImpl(id, options, { map.dpiFactor }), Markup<Line, LineOptions> {

    override var annotations = computeAnnotations()
    override var removed: Boolean = false

    private fun interpolateGeodesic(points: List<MapboxLatLng>): List<MapboxLatLng> {
        val maxSegmentMeters = 20_000.0
        val curvatureBoost = 0.75

        if (points.size <= 1) return points.toList()

        val r = 6_371_008.8  // mean Earth radius (meters)

        fun toVec(latDeg: Double, lonDeg: Double): DoubleArray {
            val lat = Math.toRadians(latDeg)
            val lon = Math.toRadians(lonDeg)
            val cl = cos(lat)
            return doubleArrayOf(cl * cos(lon), cl * sin(lon), sin(lat))
        }

        fun norm(v: DoubleArray): DoubleArray {
            val m = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
            return doubleArrayOf(v[0] / m, v[1] / m, v[2] / m)
        }

        fun toLatLng(v: DoubleArray): MapboxLatLng {
            val x = v[0];
            val y = v[1];
            val z = v[2]
            val lat = asin(z)
            val lon = atan2(y, x)
            // wrap to [-180,180)
            var lonDeg = Math.toDegrees(lon)
            lonDeg = ((lonDeg + 540.0) % 360.0) - 180.0
            return MapboxLatLng(Math.toDegrees(lat), lonDeg)
        }

        fun centralAngle(a: DoubleArray, b: DoubleArray): Double {
            val dot = (a[0] * b[0] + a[1] * b[1] + a[2] * b[2]).coerceIn(-1.0, 1.0)
            return acos(dot)
        }

        val out = ArrayList<MapboxLatLng>(points.size * 4)

        for (i in 0 until points.lastIndex) {
            val a = points[i]
            val b = points[i + 1]
            val va = norm(toVec(a.latitude, a.longitude))
            val vb = norm(toVec(b.latitude, b.longitude))
            val omega = centralAngle(va, vb)

            // Base segment count from distance
            val distance = omega * r
            var steps = max(1, ceil(distance / maxSegmentMeters).toInt())

            // Heuristic curvature boost (how "curvy" it *looks* in Web-Mercator)
            val meanLatRad = Math.toRadians((a.latitude + b.latitude) / 2.0)
            val dLonRad = abs(
                // shortest ∆lon across antimeridian
                ((Math.toRadians(b.longitude - a.longitude) + Math.PI) % (2 * Math.PI)) - Math.PI
            )
            val mercatorCurviness = abs(sin(meanLatRad)) * (dLonRad / Math.PI) // 0..1
            val boost = 1.0 + curvatureBoost * mercatorCurviness
            steps = max(1, ceil(steps * boost).toInt())

            // Emit points along the great-circle (slerp)
            val sinOmega = sin(omega)
            // Add first point (or skip if already added as previous segment's end)
            if (i == 0) out.add(MapboxLatLng(a.latitude, a.longitude))

            if (omega == 0.0 || sinOmega == 0.0) {
                // identical points: skip interpolation
                out.add(MapboxLatLng(b.latitude, b.longitude))
                continue
            }

            for (k in 1..steps) {
                val t = k.toDouble() / steps
                val s1 = sin((1 - t) * omega) / sinOmega
                val s2 = sin(t * omega) / sinOmega
                val vx = s1 * va[0] + s2 * vb[0]
                val vy = s1 * va[1] + s2 * vb[1]
                val vz = s1 * va[2] + s2 * vb[2]
                var p = toLatLng(doubleArrayOf(vx, vy, vz))

                if (out.isNotEmpty() && abs(p.longitude - out.last().longitude) > 180) {
                    // Make sure the current point crosses the antimeridian not normalized,
                    // i.e. going from +179° to +181° instead of +179° to -179°.
                    // This avoids a long horizontal line across the map.
                    val lon = if (out.last().longitude > 0) p.longitude + 360 else p.longitude - 360
                    p = MapboxLatLng(p.latitude, lon)
                }

                // Avoid duplicating the joint point on the next segment
                if (k < steps || i == points.lastIndex - 1) {
                    out.add(p)
                }
            }
        }
        return out
    }

    private fun List<MapboxLatLng>.mapToGeodesicIfNeeded(): List<MapboxLatLng> {
        if (!geodesic) return this
        return interpolateGeodesic(this)
    }

    private fun computeAnnotations(): List<AnnotationTracker<Line, LineOptions>> {
        val pointsQueue = LinkedList(points)
        val result = mutableListOf<AnnotationTracker<Line, LineOptions>>()

        for (span in spans) {
            val spanPoints = mutableListOf<LatLng>()

            var i = 0
            while (i < span.segments && pointsQueue.isNotEmpty()) {
                spanPoints.add(pointsQueue.removeFirst())
                i++
            }

            val options = baseAnnotationOptions
                // TODO: implement gradient support
                .withLineColor(ColorUtils.colorToRgbaString(span.style.color))
                .withLineOpacity(if (visible and span.style.isVisible) 1f else 0f)
                .withLineWidth((span.style.width) / map.dpiFactor)
                .withLatLngs(
                    spanPoints
                        .map { it.toMapbox() }
                        .mapToGeodesicIfNeeded()
                )
            result.add(AnnotationTracker(options))
        }

        if (pointsQueue.isNotEmpty()) {
            val options = baseAnnotationOptions
                .withLatLngs(
                    pointsQueue
                        .map { it.toMapbox() }
                        .mapToGeodesicIfNeeded()
                )
            result.add(AnnotationTracker(options))
        }

        return result
    }

    override fun remove() {
        removed = true
        map.getManagerForZIndex<Line, LineOptions>(LINE, zIndex)?.let { update(it) }
    }

    override fun update() {
        computeAnnotations().forEachIndexed { i, it ->
            if (i < annotations.size) {
                annotations[i].options = it.options
                annotations[i].annotation?.apply {
                    latLngs = it.options.latLngs
                    lineWidth = it.options.lineWidth
                    lineColor = it.options.lineColor
                    lineOpacity = it.options.lineOpacity
                    lineJoin = it.options.lineJoin
                }
            } else {
                annotations = annotations + it
            }
        }
        map.getManagerForZIndex<Line, LineOptions>(LINE, zIndex)?.let { update(it) }
    }

    override fun setZIndex(zIndex: Float) {
        val oldZIndex = this.zIndex
        if (oldZIndex == zIndex) {
            super.setZIndex(zIndex)
            return
        }

        removed = true
        map.getManagerForZIndex<Line, LineOptions>(LINE, zIndex)?.let { update(it) }
        super.setZIndex(zIndex)
        removed = false
        map.getManagerForZIndex<Line, LineOptions>(LINE, zIndex)?.let { update(it) }
    }

    companion object {
        private val TAG = "GmsMapPolyline"
    }
}

class LitePolylineImpl(private val map: LiteGoogleMapImpl, id: String, options: GmsLineOptions) :
    AbstractPolylineImpl(id, options, { map.dpiFactor }) {
    override fun remove() {
        map.polylines.remove(this)
        map.postUpdateSnapshot()
    }

    override fun update() {
        map.postUpdateSnapshot()
    }
}
