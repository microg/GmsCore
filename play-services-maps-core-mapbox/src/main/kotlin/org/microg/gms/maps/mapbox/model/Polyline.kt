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
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.maps.mapbox.utils.toGms
import org.microg.gms.maps.mapbox.utils.toMapbox

class PolylineImpl(private val map: GoogleMapImpl, private val line: Line) : IPolylineDelegate.Stub() {
    override fun remove() {
        map.lineManager?.delete(line)
    }

    override fun getId(): String = "l" + line.id.toString()

    override fun setPoints(points: MutableList<LatLng>) {
        line.latLngs = points.map { it.toMapbox() }
        map.lineManager?.update(line)
    }

    override fun getPoints(): List<LatLng> = line.latLngs.map { it.toGms() }

    override fun setWidth(width: Float) {
        line.lineWidth = width / map.dpiFactor
        map.lineManager?.update(line)
    }

    override fun getWidth(): Float = line.lineWidth * map.dpiFactor

    override fun setColor(color: Int) {
        line.setLineColor(color)
        map.lineManager?.update(line)
    }

    override fun getColor(): Int = line.lineColorAsInt

    override fun setZIndex(zIndex: Float) {
        Log.d(TAG, "unimplemented Method: setZIndex")
    }

    override fun getZIndex(): Float {
        Log.d(TAG, "unimplemented Method: getZIndex")
        return 0f
    }

    override fun setVisible(visible: Boolean) {
        line.lineOpacity = if (visible) 1f else 0f
        map.lineManager?.update(line)
    }

    override fun isVisible(): Boolean = line.lineOpacity != 0f

    override fun setGeodesic(geod: Boolean) {
        Log.d(TAG, "unimplemented Method: setGeodesic")
    }

    override fun isGeodesic(): Boolean {
        Log.d(TAG, "unimplemented Method: isGeodesic")
        return false
    }

    override fun equalsRemote(other: IPolylineDelegate?): Boolean = equals(other)

    override fun hashCodeRemote(): Int = hashCode()

    override fun equals(other: Any?): Boolean {
        if (other is PolylineImpl) {
            return other.line == line
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
        private val TAG = "GmsMapPolyline"
    }
}