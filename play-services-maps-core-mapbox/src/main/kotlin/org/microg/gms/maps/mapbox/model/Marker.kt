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

import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.internal.IMarkerDelegate
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import org.microg.gms.kotlin.unwrap
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.maps.mapbox.utils.toGms
import org.microg.gms.maps.mapbox.utils.toMapbox

class MarkerImpl(private val map: GoogleMapImpl,
                 private val symbol: Symbol,
                 private var anchor: FloatArray,
                 private var icon: BitmapDescriptorImpl?,
                 private var alpha: Float = symbol.iconOpacity,
                 private var title: String? = null,
                 private var snippet: String? = null) : IMarkerDelegate.Stub() {
    private var tag: IObjectWrapper? = null

    override fun remove() {
        map.symbolManager?.delete(symbol)
        map.markers.remove(symbol.id)
    }

    override fun getId(): String = "m" + symbol.id.toString()

    override fun setPosition(pos: LatLng?) {
        pos?.let { symbol.latLng = it.toMapbox() }
        map.symbolManager?.update(symbol)
    }

    override fun getPosition(): LatLng = symbol.latLng.toGms()

    override fun setTitle(title: String?) {
        this.title = title
    }

    override fun getTitle(): String? = title

    override fun setSnippet(snippet: String?) {
        this.snippet = snippet
    }

    override fun getSnippet(): String? = snippet

    override fun setDraggable(drag: Boolean) {
        symbol.isDraggable = drag
        map.symbolManager?.update(symbol)
    }

    override fun isDraggable(): Boolean = symbol.isDraggable

    override fun showInfoWindow() {
        Log.d(TAG, "unimplemented Method: showInfoWindow")
    }

    override fun hideInfoWindow() {
        Log.d(TAG, "unimplemented Method: hideInfoWindow")
    }

    override fun isInfoWindowShown(): Boolean {
        Log.d(TAG, "unimplemented Method: isInfoWindowShow")
        return false
    }

    override fun setVisible(visible: Boolean) {
        symbol.iconOpacity = if (visible) 0f else alpha
        map.symbolManager?.update(symbol)
    }

    override fun isVisible(): Boolean = symbol.iconOpacity != 0f

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is IMarkerDelegate) return other.id == id
        return false
    }

    override fun equalsRemote(other: IMarkerDelegate?): Boolean = equals(other)

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "$id ($title)"
    }

    override fun hashCodeRemote(): Int = hashCode()

    override fun setIcon(obj: IObjectWrapper?) {
        obj.unwrap<BitmapDescriptorImpl>()?.let { icon = it }
        icon?.applyTo(symbol, anchor, map.dpiFactor)
        map.symbolManager?.update(symbol)
    }

    override fun setAnchor(x: Float, y: Float) {
        anchor = floatArrayOf(x, y)
        icon?.applyTo(symbol, anchor, map.dpiFactor)
        map.symbolManager?.update(symbol)
    }

    override fun setFlat(flat: Boolean) {
        Log.d(TAG, "unimplemented Method: setFlat")
    }

    override fun isFlat(): Boolean {
        Log.d(TAG, "unimplemented Method: isFlat")
        return false
    }

    override fun setRotation(rotation: Float) {
        symbol.iconRotate = rotation
        map.symbolManager?.update(symbol)
    }

    override fun getRotation(): Float = symbol.iconRotate

    override fun setInfoWindowAnchor(x: Float, y: Float) {
        Log.d(TAG, "unimplemented Method: setInfoWindowAnchor")
    }

    override fun setAlpha(alpha: Float) {
        this.alpha = alpha
        symbol.iconOpacity = alpha
        map.symbolManager?.update(symbol)
    }

    override fun getAlpha(): Float = alpha

    override fun setZIndex(zIndex: Float) {
        var intBits = java.lang.Float.floatToIntBits(zIndex)
        if (intBits < 0) intBits = intBits xor 0x7fffffff
        symbol.zIndex = intBits
        map.symbolManager?.update(symbol)
    }

    override fun getZIndex(): Float {
        var intBits = symbol.zIndex
        if (intBits < 0) intBits = intBits xor 0x7fffffff
        return java.lang.Float.intBitsToFloat(intBits)
    }

    override fun setTag(obj: IObjectWrapper?) {
        this.tag = obj
    }

    override fun getTag(): IObjectWrapper? = tag

    companion object {
        private val TAG = "GmsMapMarker"
    }
}