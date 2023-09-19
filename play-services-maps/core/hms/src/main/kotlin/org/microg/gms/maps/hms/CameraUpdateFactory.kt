/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms

import android.graphics.Point
import android.os.Parcel
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.maps.internal.ICameraUpdateFactoryDelegate
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.huawei.hms.maps.CameraUpdateFactory
import org.microg.gms.maps.hms.utils.toHms
import org.microg.gms.maps.hms.utils.toHmsZoom

class CameraUpdateFactoryImpl : ICameraUpdateFactoryDelegate.Stub() {

    override fun zoomIn(): IObjectWrapper = ObjectWrapper.wrap(CameraUpdateFactory.zoomIn())
    override fun zoomOut(): IObjectWrapper = ObjectWrapper.wrap(CameraUpdateFactory.zoomOut())

    override fun zoomTo(zoom: Float): IObjectWrapper =
            ObjectWrapper.wrap(CameraUpdateFactory.zoomTo(toHmsZoom(zoom)))

    override fun zoomBy(zoomDelta: Float): IObjectWrapper =
            ObjectWrapper.wrap(CameraUpdateFactory.zoomBy(zoomDelta))

    override fun zoomByWithFocus(zoomDelta: Float, x: Int, y: Int): IObjectWrapper =
            ObjectWrapper.wrap(CameraUpdateFactory.zoomBy(zoomDelta, Point(x, y)))

    override fun newCameraPosition(cameraPosition: CameraPosition): IObjectWrapper =
            ObjectWrapper.wrap(CameraUpdateFactory.newCameraPosition(cameraPosition.toHms()))

    override fun newLatLng(latLng: LatLng): IObjectWrapper =
            ObjectWrapper.wrap(CameraUpdateFactory.newLatLng(latLng.toHms()))

    override fun newLatLngZoom(latLng: LatLng, zoom: Float): IObjectWrapper =
            ObjectWrapper.wrap(CameraUpdateFactory.newLatLngZoom(latLng.toHms(),
                toHmsZoom(zoom)
            ))

    override fun newLatLngBounds(bounds: LatLngBounds, padding: Int): IObjectWrapper =
            ObjectWrapper.wrap(CameraUpdateFactory.newLatLngBounds(bounds.toHms(), padding))

    override fun scrollBy(x: Float, y: Float): IObjectWrapper {
        Log.d(TAG, "scrollBy: $x, $y")
        // gms map: A positive value moves the camera downwards
        // hms map: A positive value moves the camera upwards
        return ObjectWrapper.wrap(CameraUpdateFactory.scrollBy(x, -y))
    }

    override fun newLatLngBoundsWithSize(bounds: LatLngBounds, width: Int, height: Int, padding: Int): IObjectWrapper =
        ObjectWrapper.wrap(CameraUpdateFactory.newLatLngBounds(bounds.toHms(), width, height, padding))

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
            if (super.onTransact(code, data, reply, flags)) {
                true
            } else {
                Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
            }

    companion object {
        private val TAG = "GmsCameraUpdate"
    }
}


