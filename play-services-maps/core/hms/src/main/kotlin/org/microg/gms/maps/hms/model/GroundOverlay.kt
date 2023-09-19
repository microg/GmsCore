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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.internal.IGroundOverlayDelegate
import com.huawei.hms.maps.model.GroundOverlay
import org.microg.gms.maps.hms.utils.toGms
import org.microg.gms.maps.hms.utils.toHms

class GroundOverlayImpl(private val groundOverlay: GroundOverlay) : IGroundOverlayDelegate.Stub() {

    override fun getId(): String {
        return groundOverlay.id
    }

    override fun getPosition(): LatLng? {
        return groundOverlay.position?.toGms()
    }

    override fun setPosition(pos: LatLng?) {
        pos?.let { groundOverlay.position = it.toHms() }
    }

    override fun getWidth(): Float {
        return groundOverlay.width
    }

    override fun getHeight(): Float {
        return groundOverlay.height
    }

    override fun setDimensions(width: Float, height: Float) {
        groundOverlay.setDimensions(width, height)
    }

    override fun getBounds(): LatLngBounds? {
        return groundOverlay.bounds?.toGms()
    }

    override fun getBearing(): Float {
        return groundOverlay.bearing
    }

    override fun setBearing(bearing: Float) {
        groundOverlay.bearing = bearing
    }

    override fun setZIndex(zIndex: Float) {
        groundOverlay.zIndex = zIndex
    }

    override fun getZIndex(): Float {
        return groundOverlay.zIndex
    }

    override fun isVisible(): Boolean {
        return groundOverlay.isVisible
    }

    override fun setVisible(visible: Boolean) {
        groundOverlay.isVisible = visible
    }

    override fun getTransparency(): Float {
        return groundOverlay.transparency
    }

    override fun setTransparency(transparency: Float) {
        groundOverlay.transparency = transparency
    }

    override fun setDimension(dimension: Float) {
        groundOverlay.setDimensions(dimension)
    }

    override fun setPositionFromBounds(bounds: LatLngBounds?) {
        bounds?.let { groundOverlay.setPositionFromBounds(it.toHms()) }
    }

    override fun getTag(): IObjectWrapper? {
        return ObjectWrapper.wrap(groundOverlay.tag)
    }

    override fun isClickable(): Boolean = groundOverlay.isClickable

    override fun setClickable(clickable: Boolean) {
        groundOverlay.isClickable = clickable
    }

    override fun setImage(obj: IObjectWrapper?) {
        groundOverlay.setImage(obj.unwrap())
    }

    override fun setTag(tag: IObjectWrapper) {
        groundOverlay.tag = tag.unwrap()
    }

    override fun equalsRemote(other: IGroundOverlayDelegate?): Boolean {
        return this == other
    }

    override fun hashCode(): Int {
        return groundOverlay.hashCode()
    }

    override fun hashCodeRemote(): Int {
        return hashCode()
    }

//    override fun todo(obj: IObjectWrapper?) {
//        Log.d(TAG, "Not yet implemented")
//    }

    override fun equals(other: Any?): Boolean {
        return groundOverlay == other
    }

    override fun remove() {
        Log.d(TAG, "Method: remove")
        groundOverlay.remove()
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        if (super.onTransact(code, data, reply, flags)) {
            true
        } else {
            Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
        }

    companion object {
        private val TAG = "GmsMapGroundOverlay"
    }
}
