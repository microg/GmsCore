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
import android.graphics.PointF
import android.os.Parcel
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.maps.internal.ICameraUpdateFactoryDelegate
import com.google.android.gms.maps.internal.IGoogleMapDelegate
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.maps.MapboxMap
import org.microg.gms.maps.mapbox.utils.toMapbox

class CameraUpdateFactoryImpl : ICameraUpdateFactoryDelegate.Stub() {

    override fun zoomIn(): IObjectWrapper = ObjectWrapper.wrap(ZoomByCameraUpdate(1f))
    override fun zoomOut(): IObjectWrapper = ObjectWrapper.wrap(ZoomByCameraUpdate(-1f))

    override fun zoomTo(zoom: Float): IObjectWrapper = ObjectWrapper.wrap(ZoomToCameraUpdate(zoom))

    override fun zoomBy(zoomDelta: Float): IObjectWrapper =
            ObjectWrapper.wrap(ZoomByCameraUpdate(zoomDelta)).also {
                Log.d(TAG, "zoomBy")
            }

    override fun zoomByWithFocus(zoomDelta: Float, x: Int, y: Int): IObjectWrapper =
            ObjectWrapper.wrap(ZoomByWithFocusCameraUpdate(zoomDelta, x, y)).also {
                Log.d(TAG, "zoomByWithFocus")
            }

    override fun newCameraPosition(cameraPosition: CameraPosition): IObjectWrapper =
            ObjectWrapper.wrap(NewCameraPositionCameraUpdate(cameraPosition)).also {
                Log.d(TAG, "newCameraPosition")
            }

    override fun newLatLng(latLng: LatLng): IObjectWrapper =
            ObjectWrapper.wrap(NewLatLngCameraUpdate(latLng)).also {
                Log.d(TAG, "newLatLng")
            }

    override fun newLatLngZoom(latLng: LatLng, zoom: Float): IObjectWrapper =
            ObjectWrapper.wrap(NewLatLngZoomCameraUpdate(latLng, zoom)).also {
                Log.d(TAG, "newLatLngZoom")
            }

    override fun newLatLngBounds(bounds: LatLngBounds, padding: Int): IObjectWrapper =
            ObjectWrapper.wrap(NewLatLngBoundsCameraUpdate(bounds, padding)).also {
                Log.d(TAG, "newLatLngBounds")
            }

    override fun scrollBy(x: Float, y: Float): IObjectWrapper {
        Log.d(TAG, "unimplemented Method: scrollBy")
        return ObjectWrapper.wrap(NoCameraUpdate())
    }

    override fun newLatLngBoundsWithSize(bounds: LatLngBounds, width: Int, height: Int, padding: Int): IObjectWrapper =
        ObjectWrapper.wrap(CameraBoundsWithSizeUpdate(bounds.toMapbox(), width, height, padding)).also {
            Log.d(TAG, "newLatLngBoundsWithSize")
        }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
            if (super.onTransact(code, data, reply, flags)) {
                true
            } else {
                Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
            }

    private inner class NoCameraUpdate : CameraUpdate, LiteModeCameraUpdate {
        override fun getCameraPosition(mapboxMap: MapboxMap): com.mapbox.mapboxsdk.camera.CameraPosition? =
                mapboxMap.cameraPosition

        override fun getLiteModeCameraPosition(map: IGoogleMapDelegate): CameraPosition = map.cameraPosition
    }

    companion object {
        private val TAG = "GmsCameraUpdate"
    }
}

interface LiteModeCameraUpdate {
    fun getLiteModeCameraPosition(map: IGoogleMapDelegate): CameraPosition?

    fun getLiteModeCameraBounds(): com.mapbox.mapboxsdk.geometry.LatLngBounds? = null
}

class ZoomToCameraUpdate(private val zoom: Float) : LiteModeCameraUpdate, CameraUpdate {
    override fun getLiteModeCameraPosition(map: IGoogleMapDelegate): CameraPosition =
        CameraPosition.Builder(map.cameraPosition).zoom(zoom).build()

    override fun getCameraPosition(mapboxMap: MapboxMap): com.mapbox.mapboxsdk.camera.CameraPosition? =
        CameraUpdateFactory.zoomTo(zoom.toDouble() - 1.0).getCameraPosition(mapboxMap)

}

class ZoomByCameraUpdate(private val delta: Float) : LiteModeCameraUpdate, CameraUpdate {
    override fun getLiteModeCameraPosition(map: IGoogleMapDelegate): CameraPosition =
        CameraPosition.Builder(map.cameraPosition).zoom(map.cameraPosition.zoom + delta).build()

    override fun getCameraPosition(mapboxMap: MapboxMap): com.mapbox.mapboxsdk.camera.CameraPosition? =
        CameraUpdateFactory.zoomBy(delta.toDouble()).getCameraPosition(mapboxMap)

}

class ZoomByWithFocusCameraUpdate(private val delta: Float, private val x: Int, private val y: Int) : LiteModeCameraUpdate,
    CameraUpdate {
    override fun getLiteModeCameraPosition(map: IGoogleMapDelegate): CameraPosition =
        CameraPosition.Builder(map.cameraPosition).zoom(map.cameraPosition.zoom + delta)
            .target(map.projection.fromScreenLocation(ObjectWrapper.wrap(PointF(x.toFloat(), y.toFloat())))).build()

    override fun getCameraPosition(mapboxMap: MapboxMap): com.mapbox.mapboxsdk.camera.CameraPosition? =
        CameraUpdateFactory.zoomBy(delta.toDouble(), Point(x, y)).getCameraPosition(mapboxMap)
}

class NewCameraPositionCameraUpdate(private val cameraPosition: CameraPosition) : LiteModeCameraUpdate, CameraUpdate {
    override fun getLiteModeCameraPosition(map: IGoogleMapDelegate): CameraPosition = this.cameraPosition

    override fun getCameraPosition(mapboxMap: MapboxMap): com.mapbox.mapboxsdk.camera.CameraPosition =
        this.cameraPosition.toMapbox()
}

class NewLatLngCameraUpdate(private val latLng: LatLng) : LiteModeCameraUpdate, CameraUpdate {
    override fun getLiteModeCameraPosition(map: IGoogleMapDelegate): CameraPosition =
        CameraPosition.Builder(map.cameraPosition).target(latLng).build()

    override fun getCameraPosition(mapboxMap: MapboxMap): com.mapbox.mapboxsdk.camera.CameraPosition? =
        CameraUpdateFactory.newLatLng(latLng.toMapbox()).getCameraPosition(mapboxMap)
}

class NewLatLngZoomCameraUpdate(private val latLng: LatLng, private val zoom: Float) : LiteModeCameraUpdate, CameraUpdate {
    override fun getLiteModeCameraPosition(map: IGoogleMapDelegate): CameraPosition =
        CameraPosition.Builder(map.cameraPosition).target(latLng).zoom(zoom).build()

    override fun getCameraPosition(mapboxMap: MapboxMap): com.mapbox.mapboxsdk.camera.CameraPosition? =
        CameraUpdateFactory.newLatLngZoom(latLng.toMapbox(), zoom - 1.0).getCameraPosition(mapboxMap)
}

class NewLatLngBoundsCameraUpdate(private val bounds: LatLngBounds, internal val padding: Int) : LiteModeCameraUpdate,
    CameraUpdate {

    override fun getLiteModeCameraPosition(map: IGoogleMapDelegate): CameraPosition? = null

    override fun getLiteModeCameraBounds() = bounds.toMapbox()

    override fun getCameraPosition(mapboxMap: MapboxMap): com.mapbox.mapboxsdk.camera.CameraPosition? =
        CameraUpdateFactory.newLatLngBounds(bounds.toMapbox(), padding).getCameraPosition(mapboxMap)
}