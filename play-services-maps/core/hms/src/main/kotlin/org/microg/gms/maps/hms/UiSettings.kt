/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms

import android.os.Parcel
import android.util.Log

import com.google.android.gms.maps.internal.IUiSettingsDelegate
import com.huawei.hms.maps.UiSettings

class UiSettingsImpl(private val uiSettings: UiSettings) : IUiSettingsDelegate.Stub() {

    override fun setZoomControlsEnabled(zoom: Boolean) {
        Log.d(TAG, "setZoomControlsEnabled: $zoom")
        uiSettings.isZoomControlsEnabled = zoom
    }

    override fun setCompassEnabled(compass: Boolean) {
        uiSettings.isCompassEnabled = compass
    }

    override fun setMyLocationButtonEnabled(locationButton: Boolean) {
        uiSettings.isMyLocationButtonEnabled = locationButton
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
        Log.d(TAG, "isZoomControlsEnabled")
        return uiSettings.isZoomControlsEnabled
    }

    override fun isCompassEnabled(): Boolean = uiSettings.isCompassEnabled

    override fun isMyLocationButtonEnabled(): Boolean {
        Log.d(TAG, "isMyLocationButtonEnabled")
        return uiSettings.isMyLocationButtonEnabled
    }

    override fun isScrollGesturesEnabled(): Boolean = uiSettings.isScrollGesturesEnabled

    override fun isZoomGesturesEnabled(): Boolean = uiSettings.isZoomGesturesEnabled

    override fun isTiltGesturesEnabled(): Boolean = uiSettings.isTiltGesturesEnabled

    override fun isRotateGesturesEnabled(): Boolean = uiSettings.isRotateGesturesEnabled

    override fun setIndoorLevelPickerEnabled(indoorLevelPicker: Boolean) {
        Log.d(TAG, "setIndoorLevelPickerEnabled: $indoorLevelPicker")
        uiSettings.isIndoorLevelPickerEnabled = indoorLevelPicker
    }

    override fun isIndoorLevelPickerEnabled(): Boolean {
        Log.d(TAG, "isIndoorLevelPickerEnabled")
        return uiSettings.isIndoorLevelPickerEnabled
    }

    override fun setMapToolbarEnabled(mapToolbar: Boolean) {
        Log.d(TAG, "setMapToolbarEnabled: $mapToolbar")
        uiSettings.isMapToolbarEnabled = mapToolbar
    }

    override fun isMapToolbarEnabled(): Boolean {
        Log.d(TAG, "isMapToolbarEnabled")
        return uiSettings.isMapToolbarEnabled
    }

    override fun setScrollGesturesEnabledDuringRotateOrZoom(scrollDuringZoom: Boolean) {
        Log.d(TAG, "setScrollGesturesEnabledDuringRotateOrZoom: $scrollDuringZoom")
        uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = scrollDuringZoom
    }

    override fun isScrollGesturesEnabledDuringRotateOrZoom(): Boolean {
        Log.d(TAG, "isScrollGesturesEnabledDuringRotateOrZoom")
        return uiSettings.isScrollGesturesEnabledDuringRotateOrZoom
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
