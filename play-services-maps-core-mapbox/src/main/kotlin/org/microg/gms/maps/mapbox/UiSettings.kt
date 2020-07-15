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

import android.os.Parcel
import android.os.RemoteException
import android.util.Log

import com.google.android.gms.maps.internal.IUiSettingsDelegate
import com.mapbox.mapboxsdk.maps.UiSettings

class UiSettingsImpl(private val uiSettings: UiSettings) : IUiSettingsDelegate.Stub() {

    override fun setZoomControlsEnabled(zoom: Boolean) {
        Log.d(TAG, "unimplemented Method: setZoomControlsEnabled")
    }

    override fun setCompassEnabled(compass: Boolean) {
        uiSettings.isCompassEnabled = compass
    }

    override fun setMyLocationButtonEnabled(locationButton: Boolean) {
        Log.d(TAG, "unimplemented Method: setMyLocationButtonEnabled")

    }

    override fun setScrollGesturesEnabled(scrollGestures: Boolean) {
        uiSettings.isScrollGesturesEnabled = scrollGestures
    }

    override fun setZoomGesturesEnabled(zoomGestures: Boolean) {
        uiSettings.isZoomGesturesEnabled = zoomGestures
    }

    override fun setTiltGesturesEnabled(tiltGestures: Boolean) {
        uiSettings.isTiltGesturesEnabled = tiltGestures
    }

    override fun setRotateGesturesEnabled(rotateGestures: Boolean) {
        uiSettings.isRotateGesturesEnabled = rotateGestures
    }

    override fun setAllGesturesEnabled(gestures: Boolean) {
        uiSettings.setAllGesturesEnabled(gestures)
    }

    override fun isZoomControlsEnabled(): Boolean {
        Log.d(TAG, "unimplemented Method: isZoomControlsEnabled")
        return false
    }

    override fun isCompassEnabled(): Boolean = uiSettings.isCompassEnabled

    override fun isMyLocationButtonEnabled(): Boolean {
        Log.d(TAG, "unimplemented Method: isMyLocationButtonEnabled")
        return false
    }

    override fun isScrollGesturesEnabled(): Boolean = uiSettings.isScrollGesturesEnabled

    override fun isZoomGesturesEnabled(): Boolean = uiSettings.isZoomGesturesEnabled

    override fun isTiltGesturesEnabled(): Boolean = uiSettings.isTiltGesturesEnabled

    override fun isRotateGesturesEnabled(): Boolean = uiSettings.isRotateGesturesEnabled

    override fun setIndoorLevelPickerEnabled(indoorLevelPicker: Boolean) {
        Log.d(TAG, "unimplemented Method: setIndoorLevelPickerEnabled")
    }

    override fun isIndoorLevelPickerEnabled(): Boolean {
        Log.d(TAG, "unimplemented Method: isIndoorLevelPickerEnabled")
        return false
    }

    override fun setMapToolbarEnabled(mapToolbar: Boolean) {
        Log.d(TAG, "unimplemented Method: setMapToolbarEnabled")
    }

    override fun isMapToolbarEnabled(): Boolean {
        Log.d(TAG, "unimplemented Method: isMapToolbarEnabled")
        return false
    }

    override fun setScrollGesturesEnabledDuringRotateOrZoom(scrollDuringZoom: Boolean) {
        Log.d(TAG, "unimplemented Method: setScrollGesturesEnabledDuringRotateOrZoom")
    }

    override fun isScrollGesturesEnabledDuringRotateOrZoom(): Boolean {
        Log.d(TAG, "unimplemented Method: isScrollGesturesEnabledDuringRotateOrZoom")
        return true
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
            if (super.onTransact(code, data, reply, flags)) {
                true
            } else {
                Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
            }

    companion object {
        private val TAG = "GmsMapsUi"
    }
}
