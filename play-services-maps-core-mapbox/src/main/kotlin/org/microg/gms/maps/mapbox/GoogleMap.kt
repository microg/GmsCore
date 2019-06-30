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

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Parcel
import android.support.annotation.IdRes
import android.support.annotation.Keep
import android.support.v4.util.LongSparseArray
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.internal.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.internal.*
import com.mapbox.mapboxsdk.LibraryLoader
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.R
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.constants.MapboxConstants
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.*
import com.mapbox.mapboxsdk.plugins.annotation.Annotation
import com.mapbox.mapboxsdk.style.layers.Property.LINE_CAP_ROUND
import com.mapbox.mapboxsdk.utils.ColorUtils
import org.microg.gms.kotlin.unwrap
import org.microg.gms.maps.MapsConstants.*
import org.microg.gms.maps.mapbox.model.*
import org.microg.gms.maps.mapbox.utils.MapContext
import org.microg.gms.maps.mapbox.utils.MultiArchLoader
import org.microg.gms.maps.mapbox.utils.toGms
import org.microg.gms.maps.mapbox.utils.toMapbox

private fun <T : Any> LongSparseArray<T>.values() = (0..size()).map { valueAt(it) }.mapNotNull { it }

class GoogleMapImpl(private val context: Context, var options: GoogleMapOptions) : IGoogleMapDelegate.Stub() {

    val view: FrameLayout
    var map: MapboxMap? = null
        private set
    val dpiFactor: Float
        get() = context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT

    private var mapView: MapView?
    private var created = false
    private var initialized = false
    private val initializedCallbackList = mutableListOf<IOnMapReadyCallback>()
    private val mapLock = Object()

    private var cameraChangeListener: IOnCameraChangeListener? = null
    private var cameraMoveListener: IOnCameraMoveListener? = null
    private var cameraMoveCanceledListener: IOnCameraMoveCanceledListener? = null
    private var cameraMoveStartedListener: IOnCameraMoveStartedListener? = null
    private var cameraIdleListener: IOnCameraIdleListener? = null
    private var mapClickListener: IOnMapClickListener? = null
    private var mapLongClickListener: IOnMapLongClickListener? = null
    private var markerClickListener: IOnMarkerClickListener? = null
    private var markerDragListener: IOnMarkerDragListener? = null

    var lineManager: LineManager? = null
    var fillManager: FillManager? = null

    var circleManager: CircleManager? = null
    val pendingCircles = mutableSetOf<CircleImpl>()
    var circleId = 0L

    var symbolManager: SymbolManager? = null
    val pendingMarkers = mutableSetOf<MarkerImpl>()
    val markers = mutableMapOf<Long, MarkerImpl>()
    var markerId = 0L

    var storedMapType: Int = options.mapType
    val waitingCameraUpdates = mutableListOf<CameraUpdate>()

    init {
        val mapContext = MapContext(context)
        LibraryLoader.setLibraryLoader(MultiArchLoader(mapContext, context))
        Mapbox.getInstance(mapContext, BuildConfig.MAPBOX_KEY)

        val fakeWatermark = View(mapContext)
        fakeWatermark.layoutParams = object : RelativeLayout.LayoutParams(0, 0) {
            @SuppressLint("RtlHardcoded")
            override fun addRule(verb: Int, subject: Int) {
                super.addRule(verb, subject)
                val rules = this.rules
                var gravity = 0
                if (rules[RelativeLayout.ALIGN_PARENT_BOTTOM] == RelativeLayout.TRUE) gravity = gravity or Gravity.BOTTOM
                if (rules[RelativeLayout.ALIGN_PARENT_TOP] == RelativeLayout.TRUE) gravity = gravity or Gravity.TOP
                if (rules[RelativeLayout.ALIGN_PARENT_LEFT] == RelativeLayout.TRUE) gravity = gravity or Gravity.LEFT
                if (rules[RelativeLayout.ALIGN_PARENT_RIGHT] == RelativeLayout.TRUE) gravity = gravity or Gravity.RIGHT
                if (rules[RelativeLayout.ALIGN_PARENT_START] == RelativeLayout.TRUE) gravity = gravity or Gravity.START
                if (rules[RelativeLayout.ALIGN_PARENT_END] == RelativeLayout.TRUE) gravity = gravity or Gravity.END
                map?.uiSettings?.logoGravity = gravity
            }
        }
        this.view = object : FrameLayout(mapContext) {
            @Keep
            fun <T : View> findViewTraversal(@IdRes id: Int): T? {
                return null
            }

            @Keep
            fun <T : View> findViewWithTagTraversal(tag: Any): T? {
                if ("GoogleWatermark" == tag) {
                    return try {
                        @Suppress("UNCHECKED_CAST")
                        fakeWatermark as T
                    } catch (e: ClassCastException) {
                        null
                    }
                }
                return null
            }
        }
        this.mapView = MapView(mapContext)
        this.view.addView(this.mapView)
    }

    override fun getCameraPosition(): CameraPosition? = map?.cameraPosition?.toGms()
    override fun getMaxZoomLevel(): Float = (map?.maxZoomLevel?.toFloat() ?: 20f) + 1f
    override fun getMinZoomLevel(): Float = (map?.minZoomLevel?.toFloat() ?: 0f) + 1f

    override fun moveCamera(cameraUpdate: IObjectWrapper?) {
        val update = cameraUpdate.unwrap<CameraUpdate>() ?: return
        val map = this.map
        if (map != null) {
            map.moveCamera(update)
        } else {
            waitingCameraUpdates.add(update)
        }
    }

    override fun animateCamera(cameraUpdate: IObjectWrapper?) {
        val update = cameraUpdate.unwrap<CameraUpdate>() ?: return
        val map = this.map
        if (map != null) {
            map.animateCamera(update)
        } else {
            waitingCameraUpdates.add(update)
        }
    }

    override fun animateCameraWithCallback(cameraUpdate: IObjectWrapper?, callback: ICancelableCallback?) {
        val update = cameraUpdate.unwrap<CameraUpdate>() ?: return
        val map = this.map
        if (map != null) {
            map.animateCamera(update, callback?.toMapbox())
        } else {
            waitingCameraUpdates.add(update)
            callback?.onFinish()
        }
    }

    override fun animateCameraWithDurationAndCallback(cameraUpdate: IObjectWrapper?, duration: Int, callback: ICancelableCallback?) {
        val update = cameraUpdate.unwrap<CameraUpdate>() ?: return
        val map = this.map
        if (map != null) {
            map.animateCamera(update, duration, callback?.toMapbox())
        } else {
            waitingCameraUpdates.add(update)
            callback?.onFinish()
        }
    }

    override fun stopAnimation() = map?.cancelTransitions() ?: Unit

    override fun setMinZoomPreference(minZoom: Float) {
        map?.setMinZoomPreference(minZoom.toDouble() - 1)
    }

    override fun setMaxZoomPreference(maxZoom: Float) {
        map?.setMaxZoomPreference(maxZoom.toDouble() - 1)
    }

    override fun resetMinMaxZoomPreference() {
        map?.setMinZoomPreference(MapboxConstants.MINIMUM_ZOOM.toDouble())
        map?.setMaxZoomPreference(MapboxConstants.MAXIMUM_ZOOM.toDouble())
    }

    override fun setLatLngBoundsForCameraTarget(bounds: LatLngBounds?) {
        map?.setLatLngBoundsForCameraTarget(bounds?.toMapbox())
    }

    override fun addPolyline(options: PolylineOptions): IPolylineDelegate? {
        val lineOptions = LineOptions()
                .withLatLngs(options.points.map { it.toMapbox() })
                .withLineWidth(options.width / dpiFactor)
                .withLineColor(ColorUtils.colorToRgbaString(options.color))
                .withLineOpacity(if (options.isVisible) 1f else 0f)
        return lineManager?.let { PolylineImpl(this, it.create(lineOptions)) }
    }


    override fun addPolygon(options: PolygonOptions): IPolygonDelegate? {
        val fillOptions = FillOptions()
                .withLatLngs(listOf(options.points.map { it.toMapbox() }))
                .withFillColor(ColorUtils.colorToRgbaString(options.fillColor))
                .withFillOpacity(if (options.isVisible) 1f else 0f)
        return fillManager?.let { PolygonImpl(this, it.create(fillOptions)) }
    }

    override fun addMarker(options: MarkerOptions): IMarkerDelegate? {
        val marker = MarkerImpl(this, "m${markerId++}", options)
        synchronized(this) {
            val symbolManager = symbolManager
            if (symbolManager == null) {
                pendingMarkers.add(marker)
            } else {
                marker.update(symbolManager)
            }
        }
        return marker
    }

    override fun addGroundOverlay(options: GroundOverlayOptions): IGroundOverlayDelegate? {
        Log.d(TAG, "unimplemented Method: addGroundOverlay")
        return null
    }

    override fun addTileOverlay(options: TileOverlayOptions): ITileOverlayDelegate? {
        Log.d(TAG, "unimplemented Method: addTileOverlay")
        return null
    }

    override fun addCircle(options: CircleOptions): ICircleDelegate? {
        val circle = CircleImpl(this, "c${circleId++}", options)
        synchronized(this) {
            val circleManager = circleManager
            if (circleManager == null) {
                pendingCircles.add(circle)
            } else {
                circle.update(circleManager)
            }
        }
        return circle
    }

    override fun clear() {
        circleManager?.let { clear(it) }
        lineManager?.let { clear(it) }
        fillManager?.let { clear(it) }
        symbolManager?.let { clear(it) }
    }

    fun <T : Annotation<*>> clear(manager: AnnotationManager<*, T, *, *, *, *>) {
        val annotations = manager.getAnnotations()
        for (i in 0..annotations.size()) {
            val key = annotations.keyAt(i)
            val value = annotations[key];
            if (value is T) manager.delete(value)
        }
    }

    override fun getMapType(): Int {
        return storedMapType
    }

    override fun setMapType(type: Int) {
        storedMapType = type
        applyMapType()
    }

    fun applyMapType() {
        val circles = circleManager?.annotations?.values()
        val lines = lineManager?.annotations?.values()
        val fills = fillManager?.annotations?.values()
        val symbols = symbolManager?.annotations?.values()
        val update: (Style) -> Unit = {
            circles?.let { runCatching { circleManager?.update(it) } }
            lines?.let { runCatching { lineManager?.update(it) } }
            fills?.let { runCatching { fillManager?.update(it) } }
            symbols?.let { runCatching { symbolManager?.update(it) } }
        }

        // TODO: Serve map styles locally
        when (storedMapType) {
            MAP_TYPE_SATELLITE -> map?.setStyle(Style.Builder().fromUrl("mapbox://styles/microg/cjxgloted25ap1ct4uex7m6hi"), update)
            MAP_TYPE_TERRAIN -> map?.setStyle(Style.OUTDOORS, update)
            MAP_TYPE_HYBRID -> map?.setStyle(Style.Builder().fromUrl("mapbox://styles/microg/cjxgloted25ap1ct4uex7m6hi"), update)
            //MAP_TYPE_NONE, MAP_TYPE_NORMAL,
            else -> map?.setStyle(Style.Builder().fromUrl("mapbox://styles/microg/cjui4020201oo1fmca7yuwbor"), update)
        }

        map?.let { BitmapDescriptorFactoryImpl.registerMap(it) }

    }

    override fun setWatermarkEnabled(watermark: Boolean) {
        map?.uiSettings?.isLogoEnabled = watermark
    }

    override fun isTrafficEnabled(): Boolean {
        Log.d(TAG, "unimplemented Method: isTrafficEnabled")
        return false
    }

    override fun setTrafficEnabled(traffic: Boolean) {
        Log.d(TAG, "unimplemented Method: setTrafficEnabled")

    }

    override fun isIndoorEnabled(): Boolean {
        Log.d(TAG, "unimplemented Method: isIndoorEnabled")
        return false
    }

    override fun setIndoorEnabled(indoor: Boolean) {
        Log.d(TAG, "unimplemented Method: setIndoorEnabled")

    }

    override fun isMyLocationEnabled(): Boolean {
        Log.d(TAG, "unimplemented Method: isMyLocationEnabled")
        return false
    }

    override fun setMyLocationEnabled(myLocation: Boolean) {
        Log.d(TAG, "unimplemented Method: setMyLocationEnabled")

    }

    override fun getMyLocation(): Location? {
        Log.d(TAG, "unimplemented Method: getMyLocation")
        return null
    }

    override fun setLocationSource(locationSource: ILocationSourceDelegate) {
        Log.d(TAG, "unimplemented Method: setLocationSource")
    }

    override fun setContentDescription(desc: String?) {
        mapView?.contentDescription = desc
    }

    override fun getUiSettings(): IUiSettingsDelegate? = map?.uiSettings?.let { UiSettingsImpl(it) }

    override fun getProjection(): IProjectionDelegate? = map?.projection?.let { ProjectionImpl(it) }

    override fun setOnCameraChangeListener(listener: IOnCameraChangeListener?) {
        cameraChangeListener = listener
    }

    override fun setOnMapClickListener(listener: IOnMapClickListener?) {
        mapClickListener = listener
    }

    override fun setOnMapLongClickListener(listener: IOnMapLongClickListener?) {
        mapLongClickListener = listener
    }

    override fun setOnMarkerClickListener(listener: IOnMarkerClickListener?) {
        markerClickListener = listener
    }

    override fun setOnMarkerDragListener(listener: IOnMarkerDragListener?) {
        markerDragListener = listener
    }

    override fun setOnInfoWindowClickListener(listener: IOnInfoWindowClickListener?) {
        Log.d(TAG, "unimplemented Method: setOnInfoWindowClickListener")

    }

    override fun setInfoWindowAdapter(adapter: IInfoWindowAdapter?) {
        Log.d(TAG, "unimplemented Method: setInfoWindowAdapter")

    }

    override fun getTestingHelper(): IObjectWrapper? {
        Log.d(TAG, "unimplemented Method: getTestingHelper")
        return null
    }

    override fun setOnMyLocationChangeListener(listener: IOnMyLocationChangeListener?) {
        Log.d(TAG, "unimplemented Method: setOnMyLocationChangeListener")

    }

    override fun setOnMyLocationButtonClickListener(listener: IOnMyLocationButtonClickListener?) {
        Log.d(TAG, "unimplemented Method: setOnMyLocationButtonClickListener")

    }

    override fun snapshot(callback: ISnapshotReadyCallback, bitmap: IObjectWrapper) {
        Log.d(TAG, "unimplemented Method: snapshot")

    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        Log.d(TAG, "padding: $left, $top, $right, $bottom")
        map?.setPadding(left, top, right, bottom)
        val fourDp = mapView?.context?.resources?.getDimension(R.dimen.mapbox_four_dp)?.toInt() ?: 0
        val ninetyTwoDp = mapView?.context?.resources?.getDimension(R.dimen.mapbox_ninety_two_dp)?.toInt()
                ?: 0
        map?.uiSettings?.setLogoMargins(left + fourDp, top + fourDp, right + fourDp, bottom + fourDp)
        map?.uiSettings?.setCompassMargins(left + fourDp, top + fourDp, right + fourDp, bottom + fourDp)
        map?.uiSettings?.setAttributionMargins(left + ninetyTwoDp, top + fourDp, right + fourDp, bottom + fourDp)
    }

    override fun isBuildingsEnabled(): Boolean {
        Log.d(TAG, "unimplemented Method: isBuildingsEnabled")
        return false
    }

    override fun setBuildingsEnabled(buildings: Boolean) {
        Log.d(TAG, "unimplemented Method: setBuildingsEnabled")

    }

    override fun setOnMapLoadedCallback(callback: IOnMapLoadedCallback?) {
        Log.d(TAG, "unimplemented Method: setOnMapLoadedCallback")

    }

    override fun setCameraMoveStartedListener(listener: IOnCameraMoveStartedListener?) {
        cameraMoveStartedListener = listener
    }

    override fun setCameraMoveListener(listener: IOnCameraMoveListener?) {
        cameraMoveListener = listener
    }

    override fun setCameraMoveCanceledListener(listener: IOnCameraMoveCanceledListener?) {
        cameraMoveCanceledListener = listener
    }

    override fun setCameraIdleListener(listener: IOnCameraIdleListener?) {
        cameraIdleListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!created) {
            mapView?.onCreate(savedInstanceState?.toMapbox())
            mapView?.getMapAsync(this::initMap)
            created = true
        }
    }

    private fun hasSymbolAt(latlng: com.mapbox.mapboxsdk.geometry.LatLng): Boolean {
        val point = map?.projection?.toScreenLocation(latlng) ?: return false
        val features = map?.queryRenderedFeatures(point, SymbolManager.ID_GEOJSON_LAYER)
                ?: return false
        return features.isNotEmpty()
    }

    private fun initMap(map: MapboxMap) {
        if (this.map != null) return
        this.map = map

        map.addOnCameraIdleListener {
            try {
                cameraChangeListener?.onCameraChange(map.cameraPosition.toGms())
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        map.addOnCameraIdleListener {
            try {
                cameraIdleListener?.onCameraIdle()
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        map.addOnCameraMoveListener {
            try {
                cameraMoveListener?.onCameraMove()
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        map.addOnCameraMoveStartedListener {
            try {
                cameraMoveStartedListener?.onCameraMoveStarted(it)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        map.addOnCameraMoveCancelListener {
            try {
                cameraMoveCanceledListener?.onCameraMoveCanceled()
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        map.addOnMapClickListener { latlng ->
            try {
                mapClickListener?.let { if (!hasSymbolAt(latlng)) it.onMapClick(latlng.toGms()); }
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
            false
        }
        map.addOnMapLongClickListener { latlng ->
            try {
                mapLongClickListener?.let { if (!hasSymbolAt(latlng)) it.onMapLongClick(latlng.toGms()); }
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
            false
        }

        applyMapType()
        options.minZoomPreference?.let { if (it != 0f) map.setMinZoomPreference(it.toDouble()) }
        options.maxZoomPreference?.let { if (it != 0f) map.setMaxZoomPreference(it.toDouble()) }
        options.latLngBoundsForCameraTarget?.let { map.setLatLngBoundsForCameraTarget(it.toMapbox()) }
        options.compassEnabled?.let { map.uiSettings.isCompassEnabled = it }
        options.rotateGesturesEnabled?.let { map.uiSettings.isRotateGesturesEnabled = it }
        options.scrollGesturesEnabled?.let { map.uiSettings.isScrollGesturesEnabled = it }
        options.tiltGesturesEnabled?.let { map.uiSettings.isTiltGesturesEnabled = it }
        options.camera?.let { map.cameraPosition = it.toMapbox() }
        waitingCameraUpdates.forEach { map.moveCamera(it) }

        synchronized(mapLock) {
            Log.d(TAG, "Invoking ${initializedCallbackList.size} callbacks delayed, as map is initialized")
            for (callback in initializedCallbackList) {
                try {
                    callback.onMapReady(this)
                } catch (e: Exception) {
                    Log.w(TAG, e)
                }
            }
            initialized = true
        }

        map.getStyle {
            mapView?.let { view ->
                circleManager = CircleManager(view, map, it)
                lineManager = LineManager(view, map, it)
                fillManager = FillManager(view, map, it)
                lineManager?.lineCap = LINE_CAP_ROUND
                synchronized(this) {
                    val symbolManager = SymbolManager(view, map, it)
                    pendingMarkers.forEach { it.update(symbolManager) }
                    pendingMarkers.clear()
                    this.symbolManager = symbolManager
                }
                symbolManager?.iconAllowOverlap = true
                symbolManager?.addClickListener {
                    try {
                        markers[it.id]?.let { markerClickListener?.onMarkerClick(it) }
                    } catch (e: Exception) {
                        Log.w(TAG, e)
                    }
                }
                symbolManager?.addDragListener(object : OnSymbolDragListener {
                    override fun onAnnotationDragStarted(annotation: Symbol?) {
                        try {
                            markers[annotation?.id]?.let { markerDragListener?.onMarkerDragStart(it) }
                        } catch (e: Exception) {
                            Log.w(TAG, e)
                        }
                    }

                    override fun onAnnotationDrag(annotation: Symbol?) {
                        try {
                            markers[annotation?.id]?.let { markerDragListener?.onMarkerDrag(it) }
                        } catch (e: Exception) {
                            Log.w(TAG, e)
                        }
                    }

                    override fun onAnnotationDragFinished(annotation: Symbol?) {
                        try {
                            markers[annotation?.id]?.let { markerDragListener?.onMarkerDragEnd(it) }
                        } catch (e: Exception) {
                            Log.w(TAG, e)
                        }
                    }

                })

            }
        }
    }

    override fun useViewLifecycleWhenInFragment(): Boolean {
        Log.d(TAG, "unimplemented Method: useViewLifecycleWhenInFragment")
        return false
    }

    override fun onResume() = mapView?.onResume() ?: Unit
    override fun onPause() = mapView?.onPause() ?: Unit
    override fun onDestroy() {
        circleManager?.onDestroy()
        circleManager = null
        lineManager?.onDestroy()
        lineManager = null
        fillManager?.onDestroy()
        fillManager = null
        symbolManager?.onDestroy()
        symbolManager = null
        pendingMarkers.clear()
        markers.clear()
        BitmapDescriptorFactoryImpl.unregisterMap(map)
        view.removeView(mapView)
        // TODO can crash?
        mapView?.onDestroy()
        mapView = null
    }

    override fun onStart() {
        Log.d(TAG, "unimplemented Method: onStart")
    }

    override fun onStop() {
        Log.d(TAG, "unimplemented Method: onStop")
    }

    override fun onEnterAmbient(bundle: Bundle?) {
        Log.d(TAG, "unimplemented Method: onEnterAmbient")
    }

    override fun onExitAmbient() {
        Log.d(TAG, "unimplemented Method: onExitAmbient")
    }

    override fun onLowMemory() = mapView?.onLowMemory() ?: Unit
    override fun onSaveInstanceState(outState: Bundle) {
        val newBundle = Bundle()
        mapView?.onSaveInstanceState(newBundle)
        outState.putAll(newBundle.toGms())
    }

    fun getMapAsync(callback: IOnMapReadyCallback) {
        synchronized(mapLock) {
            if (initialized) {
                Log.d(TAG, "Invoking callback instantly, as map is initialized")
                try {
                    callback.onMapReady(this)
                } catch (e: Exception) {
                    Log.w(TAG, e)
                }
            } else {
                Log.d(TAG, "Delay callback invocation, as map is not yet initialized")
                initializedCallbackList.add(callback)
            }
        }
    }

    override fun onTransact(code: Int, data: Parcel?, reply: Parcel?, flags: Int): Boolean =
            if (super.onTransact(code, data, reply, flags)) {
                true
            } else {
                Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
            }

    companion object {
        private val TAG = "GmsMap"
    }
}
