/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms

import android.os.Parcel
import android.util.Log
import android.view.ViewGroup
import com.google.android.gms.maps.internal.IUiSettingsDelegate
import com.huawei.hms.maps.OnMapReadyCallback
import com.huawei.hms.maps.UiSettings
import org.microg.gms.maps.hms.utils.MapUiController
import org.microg.gms.maps.hms.utils.MapUiElement

private const val TAG = "GmsMapsUiSettings"

/**
 * This class "implements" unimplemented methods to avoid duplication in subclasses
 */
abstract class AbstractUiSettings(rootView: ViewGroup) : IUiSettingsDelegate.Stub() {

    protected val mapUiController = MapUiController(rootView)

    init {
        mapUiController.initUiStates(
            mapOf(
                MapUiElement.MyLocationButton to false,
                MapUiElement.ZoomView to false,
                MapUiElement.CompassView to false
            )
        )
    }

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
}

class UiSettingsImpl(private val uiSettings: UiSettings, rootView: ViewGroup) : AbstractUiSettings(rootView) {

    init {
        uiSettings.isZoomControlsEnabled = false
        uiSettings.isCompassEnabled = false
        uiSettings.isMyLocationButtonEnabled = false
    }

    override fun setZoomControlsEnabled(zoom: Boolean) {
        Log.d(TAG, "setZoomControlsEnabled: $zoom")
        uiSettings.isZoomControlsEnabled = zoom
        mapUiController.setUiEnabled(MapUiElement.ZoomView, zoom)
    }

    override fun setCompassEnabled(compass: Boolean) {
        Log.d(TAG, "setCompassEnabled: $compass")
        uiSettings.isCompassEnabled = compass
        mapUiController.setUiEnabled(MapUiElement.CompassView, compass)
    }

    override fun setMyLocationButtonEnabled(locationButton: Boolean) {
        Log.d(TAG, "setMyLocationButtonEnabled: $locationButton")
        uiSettings.isMyLocationButtonEnabled = locationButton
        mapUiController.setUiEnabled(MapUiElement.MyLocationButton, locationButton)
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
}

class UiSettingsCache(rootView: ViewGroup) : AbstractUiSettings(rootView) {

    private var compass: Boolean? = null
    private var scrollGestures: Boolean? = null
    private var zoomGestures: Boolean? = null
    private var tiltGestures: Boolean? = null
    private var rotateGestures: Boolean? = null
    private var otherGestures: Boolean? = null

    private var isZoomControlsEnabled: Boolean? = null
    private var isMyLocationButtonEnabled: Boolean? = null
    private var isAllGesturesEnabled: Boolean? = null
    private var isIndoorLevelPickerEnabled: Boolean? = null
    private var isMapToolbarEnabled: Boolean? = null
    private var isScrollGesturesEnabledDuringRotateOrZoom: Boolean? = null

    override fun setMapToolbarEnabled(mapToolbar: Boolean) {
        Log.d(TAG, "setMapToolbarEnabled: $mapToolbar")
        this.isMapToolbarEnabled = mapToolbar
    }

    override fun isMapToolbarEnabled(): Boolean {
        Log.d(TAG, "isMapToolbarEnabled")
        return isMapToolbarEnabled ?: true
    }

    override fun setScrollGesturesEnabledDuringRotateOrZoom(scrollDuringZoom: Boolean) {
        Log.d(TAG, "setScrollGesturesEnabledDuringRotateOrZoom: $scrollDuringZoom")
        this.isScrollGesturesEnabledDuringRotateOrZoom = scrollDuringZoom
    }

    override fun isScrollGesturesEnabledDuringRotateOrZoom(): Boolean {
        Log.d(TAG, "isScrollGesturesEnabledDuringRotateOrZoom")
        return isScrollGesturesEnabledDuringRotateOrZoom ?: true
    }

    override fun setIndoorLevelPickerEnabled(indoorLevelPicker: Boolean) {
        Log.d(TAG, "setIndoorLevelPickerEnabled: $indoorLevelPicker")
        this.isIndoorLevelPickerEnabled = indoorLevelPicker
    }

    override fun isIndoorLevelPickerEnabled(): Boolean {
        Log.d(TAG, "isIndoorLevelPickerEnabled")
        return isIndoorLevelPickerEnabled ?: true
    }

    override fun setMyLocationButtonEnabled(locationButton: Boolean) {
        Log.d(TAG, "setMyLocationButtonEnabled: $locationButton")
        this.isMyLocationButtonEnabled = locationButton
    }

    override fun isMyLocationButtonEnabled(): Boolean {
        Log.d(TAG, "isMyLocationButtonEnabled")
        return isMyLocationButtonEnabled ?: true
    }

    override fun setZoomControlsEnabled(zoom: Boolean) {
        Log.d(TAG, "setZoomControlsEnabled: $zoom")
        this.isZoomControlsEnabled = zoom
    }

    override fun isZoomControlsEnabled(): Boolean {
        Log.d(TAG, "isZoomControlsEnabled")
        return isZoomControlsEnabled ?: true
    }

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
        isAllGesturesEnabled = gestures
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
        uiSettings.isZoomControlsEnabled = false
        uiSettings.isCompassEnabled = false
        uiSettings.isMyLocationButtonEnabled = false

        compass?.let {
            uiSettings.isCompassEnabled = it
            mapUiController.setUiEnabled(MapUiElement.CompassView, it)
        }
        scrollGestures?.let { uiSettings.isScrollGesturesEnabled = it }
        zoomGestures?.let { uiSettings.isZoomGesturesEnabled = it }
        tiltGestures?.let { uiSettings.isTiltGesturesEnabled = it }
        rotateGestures?.let { uiSettings.isRotateGesturesEnabled = it }
        isAllGesturesEnabled?.let { uiSettings.setAllGesturesEnabled(it) }

        isZoomControlsEnabled?.let {
            uiSettings.isZoomControlsEnabled = it
            mapUiController.setUiEnabled(MapUiElement.ZoomView, it)
        }
        isMyLocationButtonEnabled?.let {
            uiSettings.isMyLocationButtonEnabled = it
            mapUiController.setUiEnabled(MapUiElement.MyLocationButton, it)
        }
        isIndoorLevelPickerEnabled?.let { uiSettings.isIndoorLevelPickerEnabled = it }
        isMapToolbarEnabled?.let { uiSettings.isMapToolbarEnabled = it }
        isScrollGesturesEnabledDuringRotateOrZoom?.let { uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = it }
    }
}
