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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.internal.IPolylineDelegate
import com.mapbox.mapboxsdk.plugins.annotation.Line
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions
import com.mapbox.mapboxsdk.utils.ColorUtils
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.maps.mapbox.utils.toMapbox
import com.google.android.gms.maps.model.PolylineOptions as GmsLineOptions

class PolylineImpl(private val map: GoogleMapImpl, private val id: String, options: GmsLineOptions) : IPolylineDelegate.Stub(), Markup<Line, LineOptions> {
    private var points = ArrayList(options.points)
    private var width = options.width
    private var color = options.color
    private var visible: Boolean = options.isVisible

    override var annotation: Line? = null
    override var removed: Boolean = false
    override val annotationOptions: LineOptions
        get() = LineOptions()
                .withLatLngs(points.map { it.toMapbox() })
                .withLineWidth(width / map.dpiFactor)
                .withLineColor(ColorUtils.colorToRgbaString(color))
                .withLineOpacity(if (visible) 1f else 0f)

    override fun remove() {
        removed = true
        map.lineManager?.let { update(it) }
    }

    override fun getId(): String = id

    override fun setPoints(points: List<LatLng>) {
        this.points = ArrayList(points)
        annotation?.latLngs = points.map { it.toMapbox() }
        map.lineManager?.let { update(it) }
    }

    override fun getPoints(): List<LatLng> = points

    override fun setWidth(width: Float) {
        this.width = width
        annotation?.lineWidth = width / map.dpiFactor
        map.lineManager?.let { update(it) }
    }

    override fun getWidth(): Float = width

    override fun setColor(color: Int) {
        this.color = color
        annotation?.setLineColor(color)
        map.lineManager?.let { update(it) }
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
        annotation?.lineOpacity = if (visible) 1f else 0f
        map.lineManager?.let { update(it) }
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

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (other is PolylineImpl) {
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
        private val TAG = "GmsMapPolyline"
    }
}