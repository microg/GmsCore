/*
 * SPDX-FileCopyrightText: 2019 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.mapbox.model

import android.os.Parcel
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.internal.IPolygonDelegate
import com.mapbox.mapboxsdk.plugins.annotation.AnnotationManager
import com.mapbox.mapboxsdk.plugins.annotation.Fill
import com.mapbox.mapboxsdk.plugins.annotation.FillOptions
import com.mapbox.mapboxsdk.utils.ColorUtils
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.maps.mapbox.utils.toMapbox
import org.microg.gms.utils.warnOnTransactionIssues

class PolygonImpl(private val map: GoogleMapImpl, private val id: String, options: PolygonOptions) : IPolygonDelegate.Stub(), Markup<Fill, FillOptions> {
    private var points = ArrayList(options.points.orEmpty())
    private var holes: List<List<LatLng>> = ArrayList(options.holes.map { ArrayList(it.orEmpty()) })
    private var fillColor = options.fillColor
    private var strokeColor = options.strokeColor
    private var strokeWidth = options.strokeWidth
    private var strokeJointType = options.strokeJointType
    private var strokePattern = ArrayList(options.strokePattern.orEmpty())
    private var visible: Boolean = options.isVisible
    private var clickable: Boolean = options.isClickable
    private var tag: IObjectWrapper? = null

    private var strokes = (listOf(PolylineImpl(map, "$id-stroke-main", PolylineOptions().color(strokeColor).width(strokeWidth).addAll(points)))
            + holes.mapIndexed { idx, it -> PolylineImpl(map, "$id-stroke-hole-$idx", PolylineOptions().color(strokeColor).width(strokeWidth).addAll(it)) }).toMutableList()

    override var annotation: Fill? = null
    override var removed: Boolean = false
    override val annotationOptions: FillOptions
        get() = FillOptions()
                .withLatLngs(mutableListOf(points.map { it.toMapbox() }).plus(holes.map { it.map { it.toMapbox() } }))
                .withFillColor(ColorUtils.colorToRgbaString(fillColor))
                .withFillOpacity(if (visible) 1f else 0f)

    override fun remove() {
        removed = true
        map.fillManager?.let { update(it) }
        strokes.forEach { it.remove() }
    }

    override fun update(manager: AnnotationManager<*, Fill, FillOptions, *, *, *>) {
        super.update(manager)
        map.lineManager?.let { lineManager -> strokes.forEach { it.update(lineManager) } }
    }

    override fun getId(): String = id

    override fun setPoints(points: List<LatLng>) {
        this.points = ArrayList(points)
        annotation?.latLngs = mutableListOf(points.map { it.toMapbox() }).plus(holes.map { it.map { it.toMapbox() } })
        map.fillManager?.let { update(it) }
        strokes[0].points = points
    }

    override fun getPoints(): List<LatLng> = points

    override fun setHoles(holes: List<Any?>?) {
        this.holes = if (holes == null) emptyList() else ArrayList(holes.mapNotNull { if (it is List<*>) it.mapNotNull { if (it is LatLng) it else null }.let { if (it.isNotEmpty()) it else null } else null })
        annotation?.latLngs = mutableListOf(points.map { it.toMapbox() }).plus(this.holes.map { it.map { it.toMapbox() } })
        while (strokes.size > this.holes.size + 1) {
            val last = strokes.last()
            last.remove()
            strokes.remove(last)
        }
        strokes.forEachIndexed { idx, it -> if (idx > 0) it.points = this.holes[idx - 1] }
        if (this.holes.size + 1 > strokes.size) {
            try {
                strokes.addAll(this.holes.subList(strokes.size, this.holes.size - 1).mapIndexed { idx, it -> PolylineImpl(map, "$id-stroke-hole-${strokes.size + idx}", PolylineOptions().color(strokeColor).width(strokeWidth).addAll(it)) })
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        map.fillManager?.let { update(it) }
    }

    override fun getHoles(): List<Any?> = holes

    override fun setStrokeWidth(width: Float) {
        this.strokeWidth = width
        strokes.forEach { it.width = width }
    }

    override fun getStrokeWidth(): Float = strokeWidth

    override fun setStrokeColor(color: Int) {
        this.strokeColor = color
        strokes.forEach { it.color = color }
    }

    override fun getStrokeColor(): Int = strokeColor

    override fun setFillColor(color: Int) {
        this.fillColor = color
        annotation?.setFillColor(color)
        map.fillManager?.let { update(it) }
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
        annotation?.fillOpacity = if (visible) 1f else 0f
        map.fillManager?.let { update(it) }
    }

    override fun isVisible(): Boolean = visible

    override fun setGeodesic(geod: Boolean) {
        Log.d(TAG, "unimplemented Method: setGeodesic")
    }

    override fun isGeodesic(): Boolean {
        Log.d(TAG, "unimplemented Method: isGeodesic")
        return false
    }

    override fun equalsRemote(other: IPolygonDelegate?): Boolean = equals(other)

    override fun hashCodeRemote(): Int = hashCode()

    override fun setClickable(click: Boolean) {
        clickable = click
    }

    override fun setStrokeJointType(type: Int) {
        strokeJointType = type
    }

    override fun getStrokeJointType(): Int = strokeJointType

    override fun setStrokePattern(items: MutableList<PatternItem>?) {
        strokePattern = ArrayList(items.orEmpty())
    }

    override fun getStrokePattern(): MutableList<PatternItem> = strokePattern

    override fun setTag(obj: IObjectWrapper?) {
        tag = obj
    }

    override fun getTag(): IObjectWrapper? = tag

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (other is PolygonImpl) {
            return other.id == id
        }
        return false
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags) { super.onTransact(code, data, reply, flags) }

    companion object {
        private val TAG = "GmsMapPolygon"
    }
}
