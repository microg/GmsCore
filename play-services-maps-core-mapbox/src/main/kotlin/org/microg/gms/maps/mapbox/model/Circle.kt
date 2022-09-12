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
import com.google.android.gms.maps.model.internal.ICircleDelegate
import com.mapbox.mapboxsdk.plugins.annotation.Circle
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions
import com.mapbox.mapboxsdk.utils.ColorUtils
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.maps.mapbox.utils.toMapbox
import com.google.android.gms.maps.model.CircleOptions as GmsCircleOptions

class CircleImpl(private val map: GoogleMapImpl, private val id: String, options: GmsCircleOptions) : ICircleDelegate.Stub(), Markup<Circle, CircleOptions> {
    private var center: LatLng = options.center
    private var radius: Double = options.radius
    private var strokeWidth: Float = options.strokeWidth
    private var strokeColor: Int = options.strokeColor
    private var fillColor: Int = options.fillColor
    private var visible: Boolean = options.isVisible

    override var annotation: Circle? = null
    override var removed: Boolean = false
    override val annotationOptions: CircleOptions
        get() = CircleOptions()
                .withLatLng(center.toMapbox())
                .withCircleColor(ColorUtils.colorToRgbaString(fillColor))
                .withCircleRadius(radius.toFloat())
                .withCircleStrokeColor(ColorUtils.colorToRgbaString(strokeColor))
                .withCircleStrokeWidth(strokeWidth / map.dpiFactor)
                .withCircleOpacity(if (visible) 1f else 0f)
                .withCircleStrokeOpacity(if (visible) 1f else 0f)

    override fun remove() {
        removed = true
        map.circleManager?.let { update(it) }
    }

    override fun getId(): String = id

    override fun setCenter(center: LatLng) {
        this.center = center
        annotation?.latLng = center.toMapbox()
        map.circleManager?.let { update(it) }
    }

    override fun getCenter(): LatLng = center

    override fun setRadius(radius: Double) {
        this.radius = radius
        annotation?.circleRadius = radius.toFloat()
        map.circleManager?.let { update(it) }
    }

    override fun getRadius(): Double = radius

    override fun setStrokeWidth(width: Float) {
        this.strokeWidth = width
        annotation?.circleStrokeWidth = width / map.dpiFactor
        map.circleManager?.let { update(it) }
    }

    override fun getStrokeWidth(): Float = strokeWidth

    override fun setStrokeColor(color: Int) {
        this.strokeColor = color
        annotation?.setCircleStrokeColor(color)
        map.circleManager?.let { update(it) }
    }

    override fun getStrokeColor(): Int = strokeColor

    override fun setFillColor(color: Int) {
        this.fillColor = color
        annotation?.setCircleColor(color)
        map.circleManager?.let { update(it) }
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
        annotation?.circleOpacity = if (visible) 1f else 0f
        annotation?.circleStrokeOpacity = if (visible) 1f else 0f
        map.circleManager?.let { update(it) }
    }

    override fun isVisible(): Boolean = visible

    override fun equalsRemote(other: ICircleDelegate?): Boolean = equals(other)

    override fun hashCodeRemote(): Int = hashCode()

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
        val TAG = "GmsMapCircle"
    }
}