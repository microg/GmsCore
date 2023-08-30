/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.mapbox.model

import android.os.Parcel
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.internal.IGroundOverlayDelegate
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.utils.warnOnTransactionIssues

class GroundOverlayImpl(private val map: GoogleMapImpl, private val id: String, options: GroundOverlayOptions) : IGroundOverlayDelegate.Stub() {
    private var location: LatLng? = options.location
    private var width: Float = options.width
    private var height: Float = options.height
    private var bounds: LatLngBounds? = options.bounds
    private var bearing: Float = options.bearing
    private var zIndex: Float = options.zIndex
    private var visible: Boolean = options.isVisible
    private var transparency: Float = options.transparency
    private var clickable: Boolean = options.isClickable
    private var tag: Any? = null

    override fun getId(): String {
        return id
    }

    override fun getPosition(): LatLng? {
        return location
    }

    override fun setPosition(pos: LatLng?) {
        this.location = pos
    }

    override fun getWidth(): Float {
        return width
    }

    override fun getHeight(): Float {
        return height
    }

    override fun setDimensions(width: Float, height: Float) {
        this.width = width
        this.height = height
    }

    override fun getBounds(): LatLngBounds? {
        return bounds
    }

    override fun getBearing(): Float {
        return bearing
    }

    override fun setBearing(bearing: Float) {
        this.bearing = bearing
    }

    override fun setZIndex(zIndex: Float) {
        this.zIndex = zIndex
    }

    override fun getZIndex(): Float {
        return zIndex
    }

    override fun isVisible(): Boolean {
        return visible
    }

    override fun setVisible(visible: Boolean) {
        this.visible = visible
    }

    override fun getTransparency(): Float {
        return transparency
    }

    override fun setTransparency(transparency: Float) {
        this.transparency = transparency
    }

    override fun setDimension(dimension: Float) {
        Log.w(TAG, "unimplemented Method: setDimension")
    }

    override fun setPositionFromBounds(bounds: LatLngBounds?) {
        this.bounds = bounds
    }

    override fun setClickable(clickable: Boolean) {
        this.clickable = clickable
    }

    override fun isClickable(): Boolean {
        return clickable
    }

    override fun setImage(img: IObjectWrapper?) {
        Log.d(TAG, "Not yet implemented: setImage")
    }

    override fun setTag(o: IObjectWrapper?) {
        this.tag = o.unwrap()
    }

    override fun getTag(): IObjectWrapper = ObjectWrapper.wrap(tag)

    override fun equalsRemote(other: IGroundOverlayDelegate?): Boolean {
        return this.equals(other)
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun hashCodeRemote(): Int {
        return hashCode()
    }

    override fun remove() {
        Log.w(TAG, "unimplemented Method: remove")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }

    companion object {
        private const val TAG = "GroundOverlay"
    }
}
