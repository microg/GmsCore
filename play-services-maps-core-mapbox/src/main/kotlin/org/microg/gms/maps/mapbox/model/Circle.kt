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
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.maps.mapbox.utils.toGms
import org.microg.gms.maps.mapbox.utils.toMapbox

class CircleImpl(private val map: GoogleMapImpl, private val circle: Circle) : ICircleDelegate.Stub() {
    override fun remove() {
        map.circleManager?.delete(circle)
    }

    override fun getId(): String = "c" + circle.id.toString()

    override fun setCenter(center: LatLng) {
        circle.latLng = center.toMapbox()
        map.circleManager?.update(circle)
    }

    override fun getCenter(): LatLng = circle.latLng.toGms()

    override fun setRadius(radius: Double) {
        circle.circleRadius = radius.toFloat()
        map.circleManager?.update(circle)
    }

    override fun getRadius(): Double = circle.circleRadius.toDouble()

    override fun setStrokeWidth(width: Float) {
        circle.circleStrokeWidth = width / map.dpiFactor
        map.circleManager?.update(circle)
    }

    override fun getStrokeWidth(): Float = circle.circleStrokeWidth * map.dpiFactor

    override fun setStrokeColor(color: Int) {
        circle.setCircleStrokeColor(color)
        map.circleManager?.update(circle)
    }

    override fun getStrokeColor(): Int = circle.circleStrokeColorAsInt

    override fun setFillColor(color: Int) {
        circle.setCircleColor(color)
        map.circleManager?.update(circle)
    }

    override fun getFillColor(): Int = circle.circleColorAsInt

    override fun setZIndex(zIndex: Float) {
        Log.d(TAG, "unimplemented Method: setZIndex")
    }

    override fun getZIndex(): Float {
        Log.d(TAG, "unimplemented Method: getZIndex")
        return 0f
    }

    override fun setVisible(visible: Boolean) {
        circle.circleOpacity = if (visible) 1f else 0f
        circle.circleStrokeOpacity = if (visible) 1f else 0f
        map.circleManager?.update(circle)
    }

    override fun isVisible(): Boolean = circle.circleOpacity != 0f || circle.circleStrokeOpacity != 0f

    override fun equalsRemote(other: ICircleDelegate?): Boolean = equals(other)

    override fun hashCodeRemote(): Int = hashCode()

    override fun equals(other: Any?): Boolean {
        if (other is CircleImpl) {
            return other.circle == circle
        }
        return false
    }

    override fun onTransact(code: Int, data: Parcel?, reply: Parcel?, flags: Int): Boolean =
            if (super.onTransact(code, data, reply, flags)) {
                true
            } else {
                Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
            }

    companion object {
        val TAG = "GmsMapCircle"
    }
}