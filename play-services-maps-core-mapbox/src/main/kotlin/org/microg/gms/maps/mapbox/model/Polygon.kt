/*
 * SPDX-FileCopyrightText: 2019 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.mapbox.model

import android.os.Parcel
import android.util.Log
import androidx.annotation.CallSuper
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.internal.IPolygonDelegate
import com.mapbox.mapboxsdk.plugins.annotation.Fill
import com.mapbox.mapboxsdk.plugins.annotation.FillOptions
import com.mapbox.mapboxsdk.utils.ColorUtils
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.maps.mapbox.LiteGoogleMapImpl
import org.microg.gms.maps.mapbox.utils.toMapbox
import org.microg.gms.utils.warnOnTransactionIssues

abstract class AbstractPolygon(private val id: String, options: PolygonOptions) : IPolygonDelegate.Stub() {
    internal var points = ArrayList(options.points.orEmpty())
    internal var holes: List<List<LatLng>> = ArrayList(options.holes.map { ArrayList(it.orEmpty()) })
    internal var fillColor = options.fillColor
    internal var strokeColor = options.strokeColor
    internal var strokeWidth = options.strokeWidth
    internal var strokeJointType = options.strokeJointType
    internal var strokePattern = ArrayList(options.strokePattern.orEmpty())
    internal var visible: Boolean = options.isVisible
    internal var clickable: Boolean = options.isClickable
    internal var tag: IObjectWrapper? = null

    val annotationOptions: FillOptions
        get() = FillOptions()
            .withLatLngs(mutableListOf(points.map { it.toMapbox() }).plus(holes.map { it.map { it.toMapbox() } }))
            .withFillColor(ColorUtils.colorToRgbaString(fillColor))
            .withFillOpacity(if (visible) 1f else 0f)

    internal abstract val strokes: MutableList<out AbstractPolylineImpl>

    internal abstract fun update()

    @CallSuper
    override fun remove() {
        for (stroke in strokes) stroke.remove()
    }

    override fun getId(): String = id

    override fun setPoints(points: List<LatLng>) {
        this.points = ArrayList(points)
        strokes[0].setPoints(points)
        update()
    }

    override fun getPoints(): List<LatLng> = points

    internal abstract fun addPolyline(id: String, options: PolylineOptions)

    override fun setHoles(holes: List<Any?>?) {
        this.holes = if (holes == null) emptyList() else ArrayList(holes.mapNotNull { if (it is List<*>) it.mapNotNull { if (it is LatLng) it else null }.let { if (it.isNotEmpty()) it else null } else null })
        while (strokes.size > this.holes.size + 1) {
            val last = strokes.last()
            last.remove()
            strokes.remove(last)
        }
        strokes.forEachIndexed { idx, it -> if (idx > 0) it.points = this.holes[idx - 1] }
        if (this.holes.size + 1 > strokes.size) {
            try {
                this.holes.subList(strokes.size, this.holes.size - 1).mapIndexed { idx, it ->
                    addPolyline(
                        "$id-stroke-hole-${strokes.size + idx}",
                        PolylineOptions().color(strokeColor).width(strokeWidth).addAll(it)
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun getHoles(): List<List<LatLng>> = holes


    override fun setStrokeWidth(width: Float) {
        strokeWidth = width
        strokes.forEach { it.setWidth(width) }
        update()
    }

    override fun getStrokeWidth(): Float = strokeWidth

    override fun setStrokeColor(color: Int) {
        strokeColor = color
        strokes.forEach { it.setColor(color) }
        update()
    }

    override fun getStrokeColor(): Int = strokeColor

    override fun setFillColor(color: Int) {
        fillColor = color
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

    override fun setGeodesic(geod: Boolean) {
        Log.d(TAG, "unimplemented Method: setGeodesic")
    }

    override fun isGeodesic(): Boolean {
        Log.d(TAG, "unimplemented Method: isGeodesic")
        return false
    }

    override fun setClickable(click: Boolean) {
        clickable = click
    }

    override fun isClickable(): Boolean = clickable

    override fun setStrokeJointType(type: Int) {
        strokeJointType = type
        update()
    }

    override fun getStrokeJointType(): Int = strokeJointType

    override fun setStrokePattern(items: MutableList<PatternItem>?) {
        strokePattern = ArrayList(items.orEmpty())
        update()
    }

    override fun getStrokePattern(): MutableList<PatternItem> = strokePattern

    override fun setTag(obj: IObjectWrapper?) {
        tag = obj
    }

    override fun getTag(): IObjectWrapper = tag ?: ObjectWrapper.wrap(null)

    override fun equalsRemote(other: IPolygonDelegate?): Boolean = equals(other)

    override fun hashCodeRemote(): Int = hashCode()

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is AbstractPolygon) {
            return other.id == id
        }
        return false
    }

    override fun toString(): String {
        return id
    }

    companion object {
        private val TAG = "GmsMapAbstractPolygon"
    }
}

class PolygonImpl(private val map: GoogleMapImpl, id: String, options: PolygonOptions) :
    AbstractPolygon(id, options), Markup<Fill, FillOptions> {


    override val strokes = (listOf(
        PolylineImpl(
            map, "$id-stroke-main", PolylineOptions().color(strokeColor).width(strokeWidth).addAll(points)
        )
    ) + holes.mapIndexed { idx, it ->
        PolylineImpl(
            map, "$id-stroke-hole-$idx", PolylineOptions().color(strokeColor).width(strokeWidth).addAll(it)
        )
    }).toMutableList()

    override var annotation: Fill? = null
    override var removed: Boolean = false

    override fun remove() {
        removed = true
        map.fillManager?.let { update(it) }
        super.remove()
    }

    override fun update() {
        annotation?.let {
            it.latLngs = mutableListOf(points.map { it.toMapbox() }).plus(holes.map { it.map { it.toMapbox() } })
            it.setFillColor(fillColor)
            it.fillOpacity = if (visible) 1f else 0f
            it.latLngs = mutableListOf(points.map { it.toMapbox() }).plus(this.holes.map { it.map { it.toMapbox() } })
        }
        map.fillManager?.let { update(it) }
    }

    override fun addPolyline(id: String, options: PolylineOptions) {
        strokes.add(PolylineImpl(map, id, options))
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags) { super.onTransact(code, data, reply, flags) }

    companion object {
        private val TAG = "GmsMapPolygon"
    }
}

class LitePolygonImpl(id: String, options: PolygonOptions, private val map: LiteGoogleMapImpl) : AbstractPolygon(id, options) {

    override val strokes: MutableList<AbstractPolylineImpl> = (listOf(
        LitePolylineImpl(
            map, "$id-stroke-main", PolylineOptions().color(strokeColor).width(strokeWidth).addAll(points)
        )
    ) + holes.mapIndexed { idx, it ->
        LitePolylineImpl(
            map, "$id-stroke-hole-$idx", PolylineOptions().color(strokeColor).width(strokeWidth).addAll(it)
        )
    }).toMutableList()


    override fun remove() {
        super.remove()
        map.polygons.remove(this)
        map.postUpdateSnapshot()
    }

    override fun update() {
        map.postUpdateSnapshot()
    }

    override fun addPolyline(id: String, options: PolylineOptions) {
        strokes.add(LitePolylineImpl(map, id, options))
    }
}