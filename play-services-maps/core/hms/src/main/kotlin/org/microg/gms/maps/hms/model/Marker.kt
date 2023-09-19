/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms.model

import android.os.Parcel
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.internal.IMarkerDelegate
import com.huawei.hms.maps.model.Marker
import org.microg.gms.maps.hms.utils.toGms
import org.microg.gms.maps.hms.utils.toHms

class MarkerImpl(private val marker: Marker) : IMarkerDelegate.Stub() {

    override fun remove() {
        marker.remove()
    }

    override fun getId(): String = marker.id

    override fun setPosition(position: LatLng?) {
        marker.position = position?.toHms()
    }

    override fun getPosition(): LatLng {
        return marker.position.toGms()
    }

    override fun setTitle(title: String?) {
        marker.title = title
    }

    override fun getTitle(): String? = marker.title

    override fun setSnippet(snippet: String?) {
        marker.snippet = snippet
    }

    override fun getSnippet(): String? = marker.snippet

    override fun setDraggable(draggable: Boolean) {
        marker.isDraggable = draggable
    }

    override fun isDraggable(): Boolean = marker.isDraggable

    override fun showInfoWindow() {
        marker.showInfoWindow()
    }

    override fun hideInfoWindow() {
        marker.hideInfoWindow()
    }

    override fun isInfoWindowShown(): Boolean {
        return marker.isInfoWindowShown
    }

    override fun setVisible(visible: Boolean) {
        marker.isVisible = visible
    }

    override fun isVisible(): Boolean = marker.isVisible

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
        marker.setIcon(obj.unwrap())
    }

    override fun setAnchor(x: Float, y: Float) {
        marker.setMarkerAnchor(x, y)
    }

    override fun setFlat(flat: Boolean) {
        marker.isFlat = flat
    }

    override fun isFlat(): Boolean {
        return marker.isFlat
    }

    override fun setRotation(rotation: Float) {
        marker.rotation = rotation
    }

    override fun getRotation(): Float = marker.rotation

    override fun setInfoWindowAnchor(x: Float, y: Float) {
        marker.setInfoWindowAnchor(x, y)
    }

    override fun setAlpha(alpha: Float) {
        marker.alpha = alpha
    }

    override fun getAlpha(): Float = marker.alpha

    override fun setZIndex(zIndex: Float) {
        marker.zIndex = zIndex
    }

    override fun getZIndex(): Float = marker.zIndex

    override fun setTag(obj: IObjectWrapper?) {
        marker.tag = obj.unwrap()
    }

    override fun getTag(): IObjectWrapper = ObjectWrapper.wrap(marker.tag)

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
