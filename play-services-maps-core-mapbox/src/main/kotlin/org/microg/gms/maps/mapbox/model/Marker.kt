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
import org.microg.gms.kotlin.unwrap
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.maps.mapbox.utils.toMapbox

class MarkerImpl(private val map: GoogleMapImpl, private val id: String, options: MarkerOptions) : IMarkerDelegate.Stub(), Markup<Symbol, SymbolOptions> {
    private var position: LatLng = options.position
    private var visible: Boolean = options.isVisible
    private var rotation: Float = options.rotation
    private var anchor: FloatArray = floatArrayOf(options.anchorU, options.anchorV)
    private var icon: BitmapDescriptorImpl? = options.icon?.remoteObject.unwrap()
    private var alpha: Float = options.alpha
    private var title: String? = options.title
    private var snippet: String? = options.snippet
    private var zIndex: Float = options.zIndex
    private var draggable: Boolean = options.isDraggable
    private var tag: IObjectWrapper? = null

    private var infoWindowShown = false

    override var annotation: Symbol? = null
    override var removed: Boolean = false
    override val annotationOptions: SymbolOptions
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

    override fun remove() {
        removed = true
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

    override fun getId(): String = id

    override fun setPosition(position: LatLng?) {
        this.position = position ?: return
        annotation?.latLng = position.toMapbox()
        map.symbolManager?.let { update(it) }
    }

    override fun getPosition(): LatLng = position

    override fun setTitle(title: String?) {
        this.title = title
    }

    override fun getTitle(): String? = title

    override fun setSnippet(snippet: String?) {
        this.snippet = snippet
    }

    override fun getSnippet(): String? = snippet

    override fun setDraggable(draggable: Boolean) {
        this.draggable = draggable
        annotation?.isDraggable = draggable
        map.symbolManager?.let { update(it) }
    }

    override fun isDraggable(): Boolean = draggable

    override fun showInfoWindow() {
        Log.d(TAG, "unimplemented Method: showInfoWindow")
        infoWindowShown = true
    }

    override fun hideInfoWindow() {
        Log.d(TAG, "unimplemented Method: hideInfoWindow")
        infoWindowShown = false
    }

    override fun isInfoWindowShown(): Boolean {
        Log.d(TAG, "unimplemented Method: isInfoWindowShow")
        return infoWindowShown
    }

    override fun setVisible(visible: Boolean) {
        this.visible = visible
        annotation?.iconOpacity = if (visible) alpha else 0f
        map.symbolManager?.let { update(it) }
    }

    override fun isVisible(): Boolean = visible

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
        obj.unwrap<BitmapDescriptorImpl>()?.let { icon ->
            this.icon = icon
            annotation?.let { icon.applyTo(it, anchor, map.dpiFactor) }
        }
        map.symbolManager?.let { update(it) }
    }

    override fun setAnchor(x: Float, y: Float) {
        anchor = floatArrayOf(x, y)
        annotation?.let { icon?.applyTo(it, anchor, map.dpiFactor) }
        map.symbolManager?.let { update(it) }
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
    }

    override fun getRotation(): Float = rotation

    override fun setInfoWindowAnchor(x: Float, y: Float) {
        Log.d(TAG, "unimplemented Method: setInfoWindowAnchor")
    }

    override fun setAlpha(alpha: Float) {
        this.alpha = alpha
        annotation?.iconOpacity = if (visible) alpha else 0f
        map.symbolManager?.let { update(it) }
    }

    override fun getAlpha(): Float = alpha

    override fun setZIndex(zIndex: Float) {
        this.zIndex = zIndex
        annotation?.symbolSortKey = zIndex
        map.symbolManager?.let { update(it) }
    }

    override fun getZIndex(): Float = zIndex

    override fun setTag(obj: IObjectWrapper?) {
        this.tag = obj
    }

    override fun getTag(): IObjectWrapper = tag ?: ObjectWrapper.wrap(null)

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
            if (super.onTransact(code, data, reply, flags)) {
                true
            } else {
                Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
            }

    companion object {
        private val TAG = "GmsMapMarker"
    }
}
