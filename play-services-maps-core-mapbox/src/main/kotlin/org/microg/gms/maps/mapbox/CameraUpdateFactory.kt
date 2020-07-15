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

package org.microg.gms.maps.mapbox

import android.graphics.Point
import android.os.Parcel
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.maps.internal.ICameraUpdateFactoryDelegate
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.maps.MapboxMap
import org.microg.gms.maps.mapbox.utils.toMapbox

class CameraUpdateFactoryImpl : ICameraUpdateFactoryDelegate.Stub() {

    override fun zoomIn(): IObjectWrapper = ObjectWrapper.wrap(CameraUpdateFactory.zoomIn())
    override fun zoomOut(): IObjectWrapper = ObjectWrapper.wrap(CameraUpdateFactory.zoomOut())

    override fun zoomTo(zoom: Float): IObjectWrapper =
            ObjectWrapper.wrap(CameraUpdateFactory.zoomTo(zoom.toDouble() - 1.0))

    override fun zoomBy(zoomDelta: Float): IObjectWrapper =
            ObjectWrapper.wrap(CameraUpdateFactory.zoomBy(zoomDelta.toDouble()))

    override fun zoomByWithFocus(zoomDelta: Float, x: Int, y: Int): IObjectWrapper =
            ObjectWrapper.wrap(CameraUpdateFactory.zoomBy(zoomDelta.toDouble(), Point(x, y)))

    override fun newCameraPosition(cameraPosition: CameraPosition): IObjectWrapper =
            ObjectWrapper.wrap(CameraUpdateFactory.newCameraPosition(cameraPosition.toMapbox()))

    override fun newLatLng(latLng: LatLng): IObjectWrapper =
            ObjectWrapper.wrap(CameraUpdateFactory.newLatLng(latLng.toMapbox()))

    override fun newLatLngZoom(latLng: LatLng, zoom: Float): IObjectWrapper =
            ObjectWrapper.wrap(CameraUpdateFactory.newLatLngZoom(latLng.toMapbox(), zoom.toDouble() - 1.0))

    override fun newLatLngBounds(bounds: LatLngBounds, padding: Int): IObjectWrapper =
            ObjectWrapper.wrap(CameraUpdateFactory.newLatLngBounds(bounds.toMapbox(), padding))

    override fun scrollBy(x: Float, y: Float): IObjectWrapper {
        Log.d(TAG, "unimplemented Method: scrollBy")
        return ObjectWrapper.wrap(NoCameraUpdate())
    }

    override fun newLatLngBoundsWithSize(bounds: LatLngBounds, width: Int, height: Int, padding: Int): IObjectWrapper =
        ObjectWrapper.wrap(CameraBoundsWithSizeUpdate(bounds.toMapbox(), width, height, padding))

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
            if (super.onTransact(code, data, reply, flags)) {
                true
            } else {
                Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
            }

    private inner class NoCameraUpdate : CameraUpdate {
        override fun getCameraPosition(mapboxMap: MapboxMap): com.mapbox.mapboxsdk.camera.CameraPosition? =
                mapboxMap.cameraPosition
    }

    companion object {
        private val TAG = "GmsCameraUpdate"
    }
}


