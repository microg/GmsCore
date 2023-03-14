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
import android.util.Log

import com.google.android.gms.maps.internal.IUiSettingsDelegate
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.UiSettings

/**
 * This class "implements" unimplemented methods to avoid duplication in subclasses
 */
abstract class AbstractUiSettings : IUiSettingsDelegate.Stub() {
    override fun setZoomControlsEnabled(zoom: Boolean) {
        Log.d(TAG, "unimplemented Method: setZoomControlsEnabled")
    }

    override fun setMyLocationButtonEnabled(locationButton: Boolean) {
        Log.d(TAG, "unimplemented Method: setMyLocationButtonEnabled")
    }

    override fun isZoomControlsEnabled(): Boolean {
        Log.d(TAG, "unimplemented Method: isZoomControlsEnabled")
        return false
    }

    override fun isMyLocationButtonEnabled(): Boolean {
        Log.d(TAG, "unimplemented Method: isMyLocationButtonEnabled")
        return false
    }

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

    companion object {
        private val TAG = "GmsMapsUi"
    }
}

class UiSettingsImpl(private val uiSettings: UiSettings) : AbstractUiSettings() {


    override fun setCompassEnabled(compass: Boolean) {
        uiSettings.isCompassEnabled = compass
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

    override fun isCompassEnabled(): Boolean = uiSettings.isCompassEnabled

    override fun isScrollGesturesEnabled(): Boolean = uiSettings.isScrollGesturesEnabled

    override fun isZoomGesturesEnabled(): Boolean = uiSettings.isZoomGesturesEnabled

    override fun isTiltGesturesEnabled(): Boolean = uiSettings.isTiltGesturesEnabled

    override fun isRotateGesturesEnabled(): Boolean = uiSettings.isRotateGesturesEnabled

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
            if (super.onTransact(code, data, reply, flags)) {
                true
            } else {
                Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
            }

    companion object {
        private val TAG = "GmsMapsUiImpl"
    }
}

class UiSettingsCache : AbstractUiSettings() {

    private var compass: Boolean? = null
    private var scrollGestures: Boolean? = null
    private var zoomGestures: Boolean? = null
    private var tiltGestures: Boolean? = null
    private var rotateGestures: Boolean? = null
    private var otherGestures: Boolean? = null

    override fun setCompassEnabled(compass: Boolean) {
        this.compass = compass
    }

    override fun setScrollGesturesEnabled(scrollGestures: Boolean) {
        this.scrollGestures = scrollGestures
    }

    override fun setZoomGesturesEnabled(zoomGestures: Boolean) {
        this.zoomGestures = zoomGestures
    }

    override fun setTiltGesturesEnabled(tiltGestures: Boolean) {
        this.tiltGestures = tiltGestures
    }

    override fun setRotateGesturesEnabled(rotateGestures: Boolean) {
        this.rotateGestures = rotateGestures
    }

    override fun setAllGesturesEnabled(gestures: Boolean) {
        // Simulate MapLibre's UiSettings behavior
        isScrollGesturesEnabled = gestures
        isRotateGesturesEnabled = gestures
        isTiltGesturesEnabled = gestures
        isZoomGesturesEnabled = gestures

        // Other gestures toggles double tap and quick zoom gestures
        otherGestures = gestures
    }

    override fun isCompassEnabled(): Boolean {
        return compass ?: true
    }

    override fun isScrollGesturesEnabled(): Boolean {
        return scrollGestures ?: true
    }

    override fun isZoomGesturesEnabled(): Boolean {
        return zoomGestures ?: true
    }

    override fun isTiltGesturesEnabled(): Boolean {
        return tiltGestures ?: true
    }

    override fun isRotateGesturesEnabled(): Boolean {
        return rotateGestures ?: true
    }

    fun getMapReadyCallback(): OnMapReadyCallback = OnMapReadyCallback { map ->
        val uiSettings = map.uiSettings
        compass?.let { uiSettings.isCompassEnabled = it }
        scrollGestures?.let { uiSettings.isScrollGesturesEnabled = it }
        zoomGestures?.let { uiSettings.isZoomGesturesEnabled = it }
        tiltGestures?.let { uiSettings.isTiltGesturesEnabled = it }
        rotateGestures?.let { uiSettings.isRotateGesturesEnabled = it }
        otherGestures?.let {
            uiSettings.isDoubleTapGesturesEnabled = it
            uiSettings.isQuickZoomGesturesEnabled = it
        }
    }
}