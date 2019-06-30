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
import com.google.android.gms.maps.model.internal.IPolygonDelegate
import com.mapbox.mapboxsdk.plugins.annotation.Fill
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.maps.mapbox.utils.toGms
import org.microg.gms.maps.mapbox.utils.toMapbox

class PolygonImpl(private val map: GoogleMapImpl, private val fill: Fill) : IPolygonDelegate.Stub() {
    override fun remove() {
        map.fillManager?.delete(fill)
    }

    override fun getId(): String = "p" + fill.id.toString()

    override fun setPoints(points: List<LatLng>) {
        fill.latLngs = listOf(points.map { it.toMapbox() })
        map.fillManager?.update(fill)
    }

    override fun getPoints(): List<LatLng> = fill.latLngs[0]?.map { it.toGms() } ?: emptyList()

    override fun setHoles(holes: List<Any?>?) {
        Log.d(TAG, "unimplemented Method: setHoles")
    }

    override fun getHoles(): List<Any?> {
        Log.d(TAG, "unimplemented Method: getHoles")
        return emptyList()
    }

    override fun setStrokeWidth(width: Float) {
        Log.d(TAG, "unimplemented Method: setStrokeWidth")
    }

    override fun getStrokeWidth(): Float {
        Log.d(TAG, "unimplemented Method: getStrokeWidth")
        return 0f
    }

    override fun setStrokeColor(color: Int) {
        fill.setFillOutlineColor(color)
        map.fillManager?.update(fill)
    }

    override fun getStrokeColor(): Int = fill.fillOutlineColorAsInt

    override fun setFillColor(color: Int) {
        fill.setFillColor(color)
        map.fillManager?.update(fill)
    }

    override fun getFillColor(): Int = fill.fillColorAsInt

    override fun setZIndex(zIndex: Float) {
        Log.d(TAG, "unimplemented Method: setZIndex")
    }

    override fun getZIndex(): Float {
        Log.d(TAG, "unimplemented Method: getZIndex")
        return 0f
    }

    override fun setVisible(visible: Boolean) {
        fill.fillOpacity = if (visible) 1f else 0f
        map.fillManager?.update(fill)
    }

    override fun isVisible(): Boolean = fill.fillOpacity != 0f

    override fun setGeodesic(geod: Boolean) {
        Log.d(TAG, "unimplemented Method: setGeodesic")
    }

    override fun isGeodesic(): Boolean {
        Log.d(TAG, "unimplemented Method: isGeodesic")
        return false
    }

    override fun equalsRemote(other: IPolygonDelegate?): Boolean = equals(other)

    override fun hashCodeRemote(): Int = hashCode()

    override fun equals(other: Any?): Boolean {
        if (other is PolygonImpl) {
            return other.fill == fill
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
        private val TAG = "GmsMapPolygon"
    }
}