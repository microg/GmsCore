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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.internal.IPolylineDelegate
import com.mapbox.mapboxsdk.plugins.annotation.Line
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions
import com.mapbox.mapboxsdk.utils.ColorUtils
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.maps.mapbox.LiteGoogleMapImpl
import org.microg.gms.maps.mapbox.utils.toMapbox
import com.google.android.gms.maps.model.PolylineOptions as GmsLineOptions

abstract class AbstractPolylineImpl(private val id: String, options: GmsLineOptions, private val dpiFactor: Function0<Float>) : IPolylineDelegate.Stub() {
    internal var points: List<LatLng> = ArrayList(options.points)
    internal var width = options.width
    internal var jointType = options.jointType
    internal var pattern = ArrayList(options.pattern.orEmpty())
    internal var color = options.color
    internal var visible: Boolean = options.isVisible
    internal var clickable: Boolean = options.isClickable
    internal var tag: IObjectWrapper? = null

    val annotationOptions: LineOptions
        get() = LineOptions()
            .withLatLngs(points.map { it.toMapbox() })
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

    override fun equalsRemote(other: IPolylineDelegate?): Boolean = equals(other)

    override fun hashCodeRemote(): Int = hashCode()

    override fun setClickable(clickable: Boolean) {
        this.clickable = clickable
    }

    override fun isClickable(): Boolean = clickable

    override fun setJointType(jointType: Int) {
        this.jointType = jointType
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

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        if (super.onTransact(code, data, reply, flags)) {
            true
        } else {
            Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
        }

    companion object {
        const val TAG = "GmsPolylineAbstract"
    }
}

class PolylineImpl(private val map: GoogleMapImpl, id: String, options: GmsLineOptions) :
    AbstractPolylineImpl(id, options, { map.dpiFactor }), Markup<Line, LineOptions> {

    override var annotation: Line? = null
    override var removed: Boolean = false

    override fun remove() {
        removed = true
        map.lineManager?.let { update(it) }
    }

    override fun update() {
        annotation?.apply {
            latLngs = points.map { it.toMapbox() }
            lineWidth = width / map.dpiFactor
            setLineColor(color)
            lineOpacity = if (visible) 1f else 0f
        }
        map.lineManager?.let { update(it) }
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
