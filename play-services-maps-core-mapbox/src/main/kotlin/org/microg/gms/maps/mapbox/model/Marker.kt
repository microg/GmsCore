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
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.internal.IMarkerDelegate
import com.mapbox.mapboxsdk.plugins.annotation.AnnotationManager
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.google.android.gms.dynamic.unwrap
import org.microg.gms.maps.mapbox.AbstractGoogleMap
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.maps.mapbox.LiteGoogleMapImpl
import org.microg.gms.maps.mapbox.utils.toMapbox

abstract class AbstractMarker(
    private val id: String, options: MarkerOptions, private val map: AbstractGoogleMap
) : IMarkerDelegate.Stub() {

    internal var position: LatLng = options.position
    internal var visible: Boolean = options.isVisible
    internal var anchor: FloatArray = floatArrayOf(options.anchorU, options.anchorV)
    internal var infoWindowAnchor: FloatArray = floatArrayOf(0.5f, 1f)
    internal var icon: BitmapDescriptorImpl? = options.icon?.remoteObject.unwrap()
    internal var alpha: Float = options.alpha
    internal var title: String? = options.title
    internal var snippet: String? = options.snippet
    internal var zIndex: Float = options.zIndex
    internal var tag: IObjectWrapper? = null
    internal open var draggable = false

    val annotationOptions: SymbolOptions
        get() {
            val symbolOptions = SymbolOptions()
                .withIconOpacity(if (visible) alpha else 0f)
                .withIconRotate(rotation)
                .withSymbolSortKey(zIndex)
                .withDraggable(draggable)

            position.let { symbolOptions.withLatLng(it.toMapbox()) }
            icon?.applyTo(symbolOptions, anchor, map.dpiFactor)
            return symbolOptions
        }

    internal abstract fun update()

    override fun getId(): String = id

    override fun setPosition(position: LatLng?) {
        this.position = position ?: return
        update()
    }

    override fun getPosition(): LatLng = position

    override fun setIcon(obj: IObjectWrapper?) {
        obj.unwrap<BitmapDescriptorImpl>()?.let { icon ->
            this.icon = icon
            update()
        }
    }

    override fun setVisible(visible: Boolean) {
        this.visible = visible
        update()
    }

    override fun setTitle(title: String?) {
        this.title = title
        update()
    }

    override fun getTitle(): String? = title

    override fun getSnippet(): String? = snippet

    override fun isVisible(): Boolean = visible

    override fun setAnchor(x: Float, y: Float) {
        anchor = floatArrayOf(x, y)
        update()
    }

    override fun setAlpha(alpha: Float) {
        this.alpha = alpha
        update()
    }

    override fun getAlpha(): Float = alpha

    override fun setZIndex(zIndex: Float) {
        this.zIndex = zIndex
        update()
    }

    override fun getZIndex(): Float = zIndex

    fun getIconDimensions(): FloatArray? {
        return icon?.size
    }

    override fun showInfoWindow() {
        if (isInfoWindowShown) {
            // Per docs, don't call `onWindowClose` if info window is re-opened programmatically
            map.currentInfoWindow?.close(silent = true)
        }
        map.showInfoWindow(this)
    }

    override fun hideInfoWindow() {
        if (isInfoWindowShown) {
            map.currentInfoWindow?.close()
            map.currentInfoWindow = null
        }
    }

    override fun isInfoWindowShown(): Boolean {
        return map.currentInfoWindow?.marker == this
    }

    override fun setTag(obj: IObjectWrapper?) {
        this.tag = obj
    }

    override fun getTag(): IObjectWrapper? = tag ?: ObjectWrapper.wrap(null)

    override fun setSnippet(snippet: String?) {
        this.snippet = snippet
    }

    override fun equalsRemote(other: IMarkerDelegate?): Boolean = equals(other)

    override fun hashCodeRemote(): Int = hashCode()

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        if (super.onTransact(code, data, reply, flags)) {
            true
        } else {
            Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
        }

    companion object {
        private val TAG = "GmsMapAbstractMarker"
    }
}

class MarkerImpl(private val map: GoogleMapImpl, private val id: String, options: MarkerOptions) :
    AbstractMarker(id, options, map), Markup<Symbol, SymbolOptions> {

    internal var rotation: Float = options.rotation
    override var draggable: Boolean = options.isDraggable

    override var annotation: Symbol? = null
    override var removed: Boolean = false

    override fun remove() {
        removed = true
        map.symbolManager?.let { update(it) }
    }

    override fun update() {
        annotation?.let {
            it.latLng = position.toMapbox()
            it.isDraggable = draggable
            it.iconOpacity = if (visible) alpha else 0f
            it.symbolSortKey = zIndex
            icon?.applyTo(it, anchor, map.dpiFactor)
        }
        map.symbolManager?.let { update(it) }
    }

    override fun update(manager: AnnotationManager<*, Symbol, SymbolOptions, *, *, *>) {
        synchronized(this) {
            val id = annotation?.id
            if (removed && id != null) {
                map.markers.remove(id)
            }
            super.update(manager)
            val annotation = annotation
            if (annotation != null && id == null) {
                map.markers[annotation.id] = this
            }
        }
    }

    override fun setPosition(position: LatLng?) {
        super.setPosition(position)
        map.currentInfoWindow?.update()
    }

    /**
     * New position is already reflected on map while if drag is in progress. Calling
     * `symbolManager.update` would interrupt the drag.
     */
    internal fun setPositionWhileDragging(position: LatLng) {
        this.position = position
        map.currentInfoWindow?.update()
    }

    override fun setTitle(title: String?) {
        super.setTitle(title)
        map.currentInfoWindow?.let {
            if (it.marker == this) it.close()
        }
    }

    override fun setSnippet(snippet: String?) {
        super.setSnippet(snippet)
        map.currentInfoWindow?.let {
            if (it.marker == this) it.close()
        }
    }

    override fun setDraggable(draggable: Boolean) {
        this.draggable = draggable
        map.symbolManager?.let { update(it) }
    }

    override fun isDraggable(): Boolean = draggable

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is IMarkerDelegate) return other.id == id
        return false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "$id ($title)"
    }

    override fun setAnchor(x: Float, y: Float) {
        super.setAnchor(x, y)
        map.currentInfoWindow?.update()
    }

    override fun setFlat(flat: Boolean) {
        Log.d(TAG, "unimplemented Method: setFlat")
    }

    override fun isFlat(): Boolean {
        Log.d(TAG, "unimplemented Method: isFlat")
        return false
    }

    override fun setRotation(rotation: Float) {
        this.rotation = rotation
        annotation?.iconRotate = rotation
        map.symbolManager?.let { update(it) }
        map.currentInfoWindow?.update()
    }

    override fun getRotation(): Float = rotation

    override fun setInfoWindowAnchor(x: Float, y: Float) {
        infoWindowAnchor = floatArrayOf(x, y)
        map.currentInfoWindow?.update()
    }

    companion object {
        private val TAG = "GmsMapMarker"
    }
}

class LiteMarkerImpl(id: String, options: MarkerOptions, private val map: LiteGoogleMapImpl) :
    AbstractMarker(id, options, map) {
    override fun remove() {
        map.markers.remove(this)
        map.postUpdateSnapshot()
    }

    override fun update() {
        map.postUpdateSnapshot()
    }

    override fun setDraggable(drag: Boolean) {
        Log.d(TAG, "setDraggable: not available in lite mode")
    }

    override fun isDraggable(): Boolean {
        Log.d(TAG, "isDraggable: markers are never draggable in lite mode")
        return false
    }

    override fun setFlat(flat: Boolean) {
        Log.d(TAG, "setFlat: not available in lite mode")
    }

    override fun isFlat(): Boolean {
        Log.d(TAG, "isFlat: markers in lite mode can never be flat")
        return false
    }

    override fun setRotation(rotation: Float) {
        Log.d(TAG, "setRotation: not available in lite mode")
    }

    override fun getRotation(): Float {
        Log.d(TAG, "setRotation: markers in lite mode can never be rotated")
        return 0f
    }

    override fun setInfoWindowAnchor(x: Float, y: Float) {
        infoWindowAnchor = floatArrayOf(x, y)
        map.currentInfoWindow?.update()
    }

    companion object {
        private val TAG = "GmsMapMarkerLite"
    }
}