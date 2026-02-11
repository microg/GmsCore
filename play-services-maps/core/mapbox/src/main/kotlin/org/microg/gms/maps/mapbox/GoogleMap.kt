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
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.annotation.IdRes
import androidx.annotation.Keep
import androidx.collection.LongSparseArray
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.internal.ICancelableCallback
import com.google.android.gms.maps.internal.ILocationSourceDelegate
import com.google.android.gms.maps.internal.IOnCameraChangeListener
import com.google.android.gms.maps.internal.IOnCameraIdleListener
import com.google.android.gms.maps.internal.IOnCameraMoveCanceledListener
import com.google.android.gms.maps.internal.IOnCameraMoveListener
import com.google.android.gms.maps.internal.IOnCameraMoveStartedListener
import com.google.android.gms.maps.internal.IOnMapLoadedCallback
import com.google.android.gms.maps.internal.IOnMapReadyCallback
import com.google.android.gms.maps.internal.IOnMarkerDragListener
import com.google.android.gms.maps.internal.IProjectionDelegate
import com.google.android.gms.maps.internal.ISnapshotReadyCallback
import com.google.android.gms.maps.internal.IUiSettingsDelegate
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.internal.ICircleDelegate
import com.google.android.gms.maps.model.internal.IGroundOverlayDelegate
import com.google.android.gms.maps.model.internal.IMarkerDelegate
import com.google.android.gms.maps.model.internal.IPolygonDelegate
import com.google.android.gms.maps.model.internal.IPolylineDelegate
import com.google.android.gms.maps.model.internal.ITileOverlayDelegate
import com.mapbox.mapboxsdk.LibraryLoader
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.R
import com.mapbox.mapboxsdk.WellKnownTileServer
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.constants.MapboxConstants
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.engine.LocationEngine
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.annotation.Annotation
import com.mapbox.mapboxsdk.plugins.annotation.AnnotationManager
import com.mapbox.mapboxsdk.plugins.annotation.Fill
import com.mapbox.mapboxsdk.plugins.annotation.FillManager
import com.mapbox.mapboxsdk.plugins.annotation.FillOptions
import com.mapbox.mapboxsdk.plugins.annotation.Line
import com.mapbox.mapboxsdk.plugins.annotation.LineManager
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolDragListener
import com.mapbox.mapboxsdk.plugins.annotation.Options
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.Property.LINE_CAP_ROUND
import org.microg.gms.maps.mapbox.model.AbstractMarker
import org.microg.gms.maps.mapbox.model.AnnotationType
import org.microg.gms.maps.mapbox.model.AnnotationType.FILL
import org.microg.gms.maps.mapbox.model.AnnotationType.LINE
import org.microg.gms.maps.mapbox.model.AnnotationType.SYMBOL
import org.microg.gms.maps.mapbox.model.BitmapDescriptorFactoryImpl
import org.microg.gms.maps.mapbox.model.CircleImpl
import org.microg.gms.maps.mapbox.model.GroundOverlayImpl
import org.microg.gms.maps.mapbox.model.InfoWindow
import org.microg.gms.maps.mapbox.model.MarkerImpl
import org.microg.gms.maps.mapbox.model.Markup
import org.microg.gms.maps.mapbox.model.PolygonImpl
import org.microg.gms.maps.mapbox.model.PolylineImpl
import org.microg.gms.maps.mapbox.model.TileOverlayImpl
import org.microg.gms.maps.mapbox.model.getInfoWindowViewFor
import org.microg.gms.maps.mapbox.utils.ComparablePair
import org.microg.gms.maps.mapbox.utils.MultiArchLoader
import org.microg.gms.maps.mapbox.utils.toGms
import org.microg.gms.maps.mapbox.utils.toMapbox
import java.util.TreeMap
import java.util.concurrent.atomic.AtomicBoolean

private fun <T : Any> LongSparseArray<T>.values() = (0 until size()).mapNotNull { valueAt(it) }

fun runOnMainLooper(forceQueue: Boolean = false, method: () -> Unit) {
    if (!forceQueue && Looper.myLooper() == Looper.getMainLooper()) {
        method()
    } else {
        Handler(Looper.getMainLooper()).post {
            method()
        }
    }
}

class GoogleMapImpl(context: Context, var options: GoogleMapOptions) : AbstractGoogleMap(context) {

    val view: FrameLayout
    var map: MapboxMap? = null
        private set

    private var mapView: MapView? = null
    private var created = false
    private var initialized = false
    private var loaded = false
    private val mapLock = Object()

    private val internalOnInitializedCallbackList = mutableListOf<OnMapReadyCallback>()
    private val userOnInitializedCallbackList = mutableListOf<IOnMapReadyCallback>()
    private var loadedCallback: IOnMapLoadedCallback? = null
    private var cameraChangeListener: IOnCameraChangeListener? = null
    private var cameraMoveListener: IOnCameraMoveListener? = null
    private var cameraMoveCanceledListener: IOnCameraMoveCanceledListener? = null
    private var cameraMoveStartedListener: IOnCameraMoveStartedListener? = null
    private var cameraIdleListener: IOnCameraIdleListener? = null
    private var markerDragListener: IOnMarkerDragListener? = null

    private val allocatedZLayers: TreeMap<ComparablePair<Float, AnnotationType>, String> = TreeMap()

    var lineManagers: MutableMap<Float, LineManager> = mutableMapOf()
    val pendingLines = mutableSetOf<Pair<Float, Markup<Line, LineOptions>>>()
    var lineId = 0L

    var fillManagers: MutableMap<Float, FillManager> = mutableMapOf()
    val pendingFills = mutableSetOf<Pair<Float, Markup<Fill, FillOptions>>>()
    val circles = mutableMapOf<Long, CircleImpl>()
    var fillId = 0L

    var symbolManagers: MutableMap<Float, SymbolManager> = mutableMapOf()
    val pendingMarkers = mutableSetOf<Pair<Float, MarkerImpl>>()
    val markers = mutableMapOf<Long, MarkerImpl>()
    var markerId = 0L

    val pendingBitmaps = mutableMapOf<String, Bitmap>()

    var groundId = 0L
    var tileId = 0L

    var storedMapType: Int = options.mapType
    var mapStyle: MapStyleOptions? = null
    val waitingCameraUpdates = mutableListOf<CameraUpdate>()
    var locationEnabled: Boolean = false

    val defaultLocationEngine = GoogleLocationEngine(context)
    var locationEngine: LocationEngine = defaultLocationEngine

    var isStarted = false

    init {
        BitmapDescriptorFactoryImpl.initialize(mapContext.resources, context.resources)
        LibraryLoader.setLibraryLoader(MultiArchLoader(mapContext, context))
        runOnMainLooper {
            Mapbox.getInstance(mapContext, BuildConfig.MAPBOX_KEY, WellKnownTileServer.Mapbox)
        }


        val fakeWatermark = View(mapContext)
        fakeWatermark.tag = "GoogleWatermark"
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

            override fun setMargins(left: Int, top: Int, right: Int, bottom: Int) {
                super.setMargins(left, top, right, bottom)
                map?.uiSettings?.setLogoMargins(left, top, right, bottom)
            }
        }
        val fakeCompass = View(mapContext)
        fakeCompass.tag = "GoogleMapCompass"
        fakeCompass.layoutParams = object : RelativeLayout.LayoutParams(0, 0) {
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
                map?.uiSettings?.compassGravity = gravity
            }

            override fun setMargins(left: Int, top: Int, right: Int, bottom: Int) {
                super.setMargins(left, top, right, bottom)
                map?.uiSettings?.setCompassMargins(left, top, right, bottom)
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
                if ("GoogleMapCompass" == tag) {
                    return try {
                        @Suppress("UNCHECKED_CAST")
                        fakeCompass as T
                    } catch (e: ClassCastException) {
                        null
                    }
                }
                return null
            }
        }
    }

    override fun getCameraPosition(): CameraPosition =
        map?.cameraPosition?.toGms() ?: CameraPosition(LatLng(0.0, 0.0), 0f, 0f, 0f)

    override fun getMaxZoomLevel(): Float = (map?.maxZoomLevel?.toFloat() ?: 20f) + 1f
    override fun getMinZoomLevel(): Float = (map?.minZoomLevel?.toFloat() ?: 0f) + 1f

    override fun moveCamera(cameraUpdate: IObjectWrapper?) {
        val update = cameraUpdate.unwrap<CameraUpdate>() ?: return
        synchronized(mapLock) {
            if (initialized) {
                this.map?.moveCamera(update)
            } else {
                waitingCameraUpdates.add(update)
            }
        }
    }

    override fun animateCamera(cameraUpdate: IObjectWrapper?) {
        val update = cameraUpdate.unwrap<CameraUpdate>() ?: return
        synchronized(mapLock) {
            if (initialized) {
                this.map?.animateCamera(update)
            } else {
                waitingCameraUpdates.add(update)
            }
        }
    }

    fun afterInitialize(runnable: (MapboxMap) -> Unit) {
        synchronized(mapLock) {
            if (initialized) {
                runnable(map!!)
            } else {
                internalOnInitializedCallbackList.add(OnMapReadyCallback {
                    runnable(it)
                })
            }
        }
    }

    override fun animateCameraWithCallback(cameraUpdate: IObjectWrapper?, callback: ICancelableCallback?) {
        val update = cameraUpdate.unwrap<CameraUpdate>() ?: return
        synchronized(mapLock) {
            if (initialized) {
                this.map?.animateCamera(update, callback?.toMapbox())
            } else {
                waitingCameraUpdates.add(update)
                afterInitialize { callback?.onFinish() }
            }
        }
    }

    override fun animateCameraWithDurationAndCallback(cameraUpdate: IObjectWrapper?, duration: Int, callback: ICancelableCallback?) {
        val update = cameraUpdate.unwrap<CameraUpdate>() ?: return
        synchronized(mapLock) {
            if (initialized) {
                this.map?.animateCamera(update, duration, callback?.toMapbox())
            } else {
                waitingCameraUpdates.add(update)
                afterInitialize { callback?.onFinish() }
            }
        }
    }

    override fun stopAnimation() = map?.cancelTransitions() ?: Unit

    override fun setMapStyle(options: MapStyleOptions?): Boolean {
        Log.d(TAG, "setMapStyle options: " + options?.getJson())
        mapStyle = options
        applyMapStyle()
        return true
    }

    override fun setMinZoomPreference(minZoom: Float) = afterInitialize {
        it.setMinZoomPreference(minZoom.toDouble() - 1)
    }

    override fun setMaxZoomPreference(maxZoom: Float) = afterInitialize {
        it.setMaxZoomPreference(maxZoom.toDouble() - 1)
    }

    override fun resetMinMaxZoomPreference() = afterInitialize {
        it.setMinZoomPreference(MapboxConstants.MINIMUM_ZOOM.toDouble())
        it.setMaxZoomPreference(MapboxConstants.MAXIMUM_ZOOM.toDouble())
    }

    override fun setLatLngBoundsForCameraTarget(bounds: LatLngBounds?) = afterInitialize {
        it.setLatLngBoundsForCameraTarget(bounds?.toMapbox())
    }

    override fun addPolyline(options: PolylineOptions): IPolylineDelegate? {
        val line = PolylineImpl(this, "l${lineId++}", options)
        synchronized(this) {
            val lineManager = getManagerForZIndex<Line, LineOptions>(LINE, line.zIndex)
            Log.d(TAG, "addPolyline zIndex=${line.zIndex}, manager=$lineManager")
            if (lineManager == null) {
                pendingLines.add(Pair(line.zIndex, line))
            } else {
                line.update(lineManager)
            }
        }
        return line
    }


    override fun addPolygon(options: PolygonOptions): IPolygonDelegate? {
        val fill = PolygonImpl(this, "p${fillId++}", options)
        synchronized(this) {
            val fillManager = getManagerForZIndex<Fill, FillOptions>(FILL, fill.zIndex)
            if (fillManager == null) {
                pendingFills.add(Pair(fill.zIndex, fill))
            } else {
                fill.update(fillManager)
            }

            val lineManager = getManagerForZIndex<Line, LineOptions>(LINE, fill.zIndex)
            if (lineManager == null) {
                pendingLines.addAll(fill.strokes.map { Pair(fill.zIndex, it) })
            } else {
                for (stroke in fill.strokes) stroke.update(lineManager)
            }
        }
        return fill
    }

    override fun addMarker(options: MarkerOptions): IMarkerDelegate {
        val marker = MarkerImpl(this, "m${markerId++}", options)
        synchronized(this) {
            val symbolManager = getManagerForZIndex<Symbol, SymbolOptions>(SYMBOL, marker.zIndex)
            if (symbolManager == null) {
                pendingMarkers.add(Pair(marker.zIndex, marker))
            } else {
                marker.update(symbolManager)
            }
        }
        return marker
    }

    override fun addGroundOverlay(options: GroundOverlayOptions): IGroundOverlayDelegate? {
        Log.d(TAG, "unimplemented Method: addGroundOverlay")
        return GroundOverlayImpl(this, "g${groundId++}", options)
    }

    override fun addTileOverlay(options: TileOverlayOptions): ITileOverlayDelegate? {
        Log.d(TAG, "unimplemented Method: addTileOverlay")
        return TileOverlayImpl(this, "t${tileId++}", options)
    }

    override fun addCircle(options: CircleOptions): ICircleDelegate {
        val circle = CircleImpl(this, "c${fillId++}", options)
        synchronized(this) {
            val fillManager = getManagerForZIndex<Fill, FillOptions>(FILL, circle.zIndex)
            if (fillManager == null) {
                pendingFills.add(Pair(circle.zIndex, circle))
            } else {
                circle.update(fillManager)
            }
            val lineManager = getManagerForZIndex<Line, LineOptions>(LINE, circle.zIndex)
            if (lineManager == null) {
                pendingLines.add(Pair(circle.zIndex, circle.line))
            } else {
                circle.line.update(lineManager)
            }
            circle.strokePattern?.let {
                addBitmap(
                    it.getName(circle.strokeColor, circle.strokeWidth),
                    it.makeBitmap(circle.strokeColor, circle.strokeWidth)
                )
            }
        }
        return circle
    }

    override fun clear() {
        lineManagers.forEach { (_, v) -> clear(v) }
        fillManagers.forEach { (_, v) -> clear(v) }
        symbolManagers.forEach { (_, v) -> clear(v) }
    }

    fun <T : Annotation<*>> clear(manager: AnnotationManager<*, T, *, *, *, *>) {
        val annotations = manager.getAnnotations()
        var i = 0
        while (i < annotations.size()) {
            val key = annotations.keyAt(i)
            val value = annotations[key]
            if (value is T) manager.delete(value)
            else i++
        }
    }

    override fun getMapType(): Int {
        return storedMapType
    }

    override fun setMapType(type: Int) {
        storedMapType = type
        applyMapStyle()
    }

    fun applyMapStyle() {
        map?.setStyle(getStyle(mapContext, storedMapType, mapStyle)) {
            lineManagers.forEach { (_, manager) ->
                val lines = manager.annotations.values()
                runCatching { manager.update(lines) }
            }
            symbolManagers.forEach { (_, manager) ->
                val symbols = manager.annotations.values()
                runCatching { manager.update(symbols) }
            }
            fillManagers.forEach { (_, manager) ->
                val fills = manager.annotations.values()
                runCatching { manager.update(fills) }
            }
        }

        map?.let { BitmapDescriptorFactoryImpl.registerMap(it) }
    }

    override fun setWatermarkEnabled(watermark: Boolean) = afterInitialize {
        it.uiSettings.isLogoEnabled = watermark
    }

    override fun isMyLocationEnabled(): Boolean {
        return locationEnabled
    }

    override fun setMyLocationEnabled(myLocation: Boolean) {
        synchronized(mapLock) {
            locationEnabled = myLocation
            if (!loaded) return
            try {
                updateLocationEngineListener(myLocation)
            } catch (e: SecurityException) {
                Log.w(TAG, e)
                locationEnabled = false
            }
        }
    }

    private fun updateLocationEngineListener(myLocation: Boolean) {
        map?.locationComponent?.let {
            if (it.isLocationComponentActivated) {
                it.isLocationComponentEnabled = myLocation
                if (myLocation) {
                    it.locationEngine?.requestLocationUpdates(it.locationEngineRequest, locationEngineCallback, Looper.getMainLooper())
                } else {
                    it.locationEngine?.removeLocationUpdates(locationEngineCallback)
                }
            }
        }
    }

    override fun setLocationSource(locationSource: ILocationSourceDelegate?) {
        synchronized(mapLock) {
            updateLocationEngineListener(false)
            locationEngine = locationSource?.let { SourceLocationEngine(it) } ?: defaultLocationEngine
            if (!loaded) return
            if (map?.locationComponent?.isLocationComponentActivated == true) {
                map?.locationComponent?.locationEngine = locationEngine
            }
            updateLocationEngineListener(locationEnabled)
        }
    }

    override fun getMyLocation(): Location? {
        synchronized(mapLock) {
            return map?.locationComponent?.lastKnownLocation
        }
    }

    override fun onLocationUpdate(location: Location) {
        // no action necessary, as the location component will automatically place a marker on the map
    }

    override fun setContentDescription(desc: String?) {
        mapView?.contentDescription = desc
    }

    override fun getUiSettings(): IUiSettingsDelegate =
        map?.uiSettings?.let { UiSettingsImpl(it) } ?: UiSettingsCache().also {
            // Apply cached UI settings after map is initialized
            internalOnInitializedCallbackList.add(it.getMapReadyCallback())
        }

    override fun getProjection(): IProjectionDelegate = map?.projection?.let {
        val experiment = try {
            map?.cameraPosition?.tilt == 0.0 && map?.cameraPosition?.bearing == 0.0
        } catch (e: Exception) {
            Log.w(TAG, e); false
        }
        ProjectionImpl(it, experiment)
    } ?: DummyProjection()

    override fun setOnCameraChangeListener(listener: IOnCameraChangeListener?) {
        cameraChangeListener = listener
    }

    override fun setOnMarkerDragListener(listener: IOnMarkerDragListener?) {
        markerDragListener = listener
    }

    override fun snapshot(callback: ISnapshotReadyCallback, bitmap: IObjectWrapper?) {
        val map = map
        if (map == null) {
            // Snapshot cannot be taken
            Log.e(TAG, "snapshot could not be taken because map is null")
            runOnMainLooper { callback.onBitmapWrappedReady(ObjectWrapper.wrap(null)) }
        } else {
            if (!isStarted) {
                Log.w(TAG, "Caller did not call onStart() before taking snapshot. Calling onStart() now, for snapshot not to fail.")
                // Snapshots fail silently if onStart had not been called. This is the case with Signal.
                onStart()
                isStarted = true
            }

            Log.d(TAG, "taking snapshot now")

            map.snapshot {
                runOnMainLooper {
                    Log.d(TAG, "snapshot ready, providing to application")
                    callback.onBitmapWrappedReady(ObjectWrapper.wrap(it))
                }
            }
        }
    }

    override fun snapshotForTest(callback: ISnapshotReadyCallback?) {
        Log.d(TAG, "Not yet implemented: snapshotForTest")
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) = afterInitialize { map ->
        Log.d(TAG, "setPadding: $left $top $right $bottom")
        val padding = map.cameraPosition.padding
        if (padding == null || padding[0] != left.toDouble() || padding[1] != top.toDouble() || padding[2] != right.toDouble() || padding[3] != bottom.toDouble()) {
            // Don't send camera update if we already got these paddings
            CameraUpdateFactory.paddingTo(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())
                .let { map.moveCamera(it) }
        }

        val fourDp = mapView?.context?.resources?.getDimension(R.dimen.maplibre_four_dp)?.toInt()
                ?: 0
        val ninetyTwoDp = mapView?.context?.resources?.getDimension(R.dimen.maplibre_ninety_two_dp)?.toInt()
                ?: 0
        map.uiSettings.setLogoMargins(left + fourDp, top + fourDp, right + fourDp, bottom + fourDp)
        map.uiSettings.setCompassMargins(left + fourDp, top + fourDp, right + fourDp, bottom + fourDp)
        map.uiSettings.setAttributionMargins(left + ninetyTwoDp, top + fourDp, right + fourDp, bottom + fourDp)
    }

    override fun setOnMapLoadedCallback(callback: IOnMapLoadedCallback?) {
        if (callback != null) {
            synchronized(mapLock) {
                if (loaded) {
                    callback.scheduleExecute()
                } else {
                    Log.d(TAG, "Delay callback invocation, as map is not yet loaded")
                    loadedCallback = callback
                }
            }
        } else {
            loadedCallback = null
        }
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
            Log.d(TAG, "create");
            val mapView = MapView(mapContext).apply { visibility = View.INVISIBLE }
            this.mapView = mapView
            view.addView(mapView)
            mapView.onCreate(savedInstanceState?.toMapbox())
            mapView.getMapAsync(this::initMap)
            created = true
            runOnMainLooper(forceQueue = true) { tryRunUserInitializedCallbacks("onCreate") }
        }
    }

    private fun hasSymbolAt(latlng: com.mapbox.mapboxsdk.geometry.LatLng): Boolean {
        val point = map?.projection?.toScreenLocation(latlng) ?: return false
        return symbolManagers.values.any { manager ->
            map
                ?.queryRenderedFeatures(point, manager.layerId)
                ?.isNotEmpty()
                ?: false
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Annotation<*>, S : Options<T>> getManagerForZIndex(
        annoType: AnnotationType,
        zIndex: Float
    ): AnnotationManager<*, T, S, *, *, *>? {
        Log.d("GmsMap", "Requested manager for $annoType at zIndex $zIndex")

        val manager = when (annoType) {
            FILL -> fillManagers[zIndex] as AnnotationManager<*, T, S, *, *, *>?
            SYMBOL -> symbolManagers[zIndex] as AnnotationManager<*, T, S, *, *, *>?
            LINE -> lineManagers[zIndex] as AnnotationManager<*, T, S, *, *, *>?
        }
        if (manager != null) return manager
        if (mapView == null || map == null || map?.style == null) return manager

        val keyMap = ComparablePair(-zIndex, annoType)

        synchronized(mapLock) {
            val belowId = allocatedZLayers.lowerEntry(keyMap)?.value
            var aboveId = allocatedZLayers.higherEntry(keyMap)?.value
            if (aboveId == belowId) aboveId = null

            Log.d(
                "GmsMap",
                "Creating new manager for $annoType at zIndex $zIndex, below=$belowId, above=$aboveId"
            )

            val newManager = when (annoType) {
                FILL -> FillManager(
                    mapView!!,
                    map!!,
                    map!!.style!!,
                    belowId,
                    aboveId
                ).apply {
                    addClickListener { fill ->
                        try {
                            circles[fill.id]?.let { circle ->
                                if (circle.isClickable) {
                                    circleClickListener?.let {
                                        it.onCircleClick(circle)
                                        return@addClickListener true
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, e)
                        }
                        false
                    }
                }

                SYMBOL -> SymbolManager(
                    mapView!!,
                    map!!,
                    map!!.style!!,
                    belowId,
                    aboveId
                ).apply {
                    iconAllowOverlap = true
                    addClickListener {
                        val marker = markers[it.id]
                        try {
                            if (markers[it.id]?.let { markerClickListener?.onMarkerClick(it) } == true) {
                                return@addClickListener true
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, e)
                            return@addClickListener false
                        }

                        marker?.let { showInfoWindow(it) } == true
                    }
                    addDragListener(object : OnSymbolDragListener {
                        override fun onAnnotationDragStarted(annotation: Symbol?) {
                            try {
                                markers[annotation?.id]?.let {
                                    markerDragListener?.onMarkerDragStart(
                                        it
                                    )
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, e)
                            }
                        }

                        override fun onAnnotationDrag(annotation: Symbol?) {
                            try {
                                annotation?.let { symbol ->
                                    markers[symbol.id]?.let { marker ->
                                        marker.setPositionWhileDragging(symbol.latLng.toGms())
                                        markerDragListener?.onMarkerDrag(marker)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, e)
                            }
                        }

                        override fun onAnnotationDragFinished(annotation: Symbol?) {
                            mapView?.post {
                                try {
                                    markers[annotation?.id]?.let {
                                        markerDragListener?.onMarkerDragEnd(
                                            it
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, e)
                                }
                            }
                        }
                    })
                }

                LINE -> LineManager(
                    mapView!!,
                    map!!,
                    map!!.style!!,
                    belowId,
                    aboveId
                ).apply {
                    lineCap = LINE_CAP_ROUND
                }
            }

            allocatedZLayers.put(keyMap, newManager.layerId)

            when (annoType) {
                FILL -> fillManagers[zIndex] = newManager as FillManager
                LINE -> lineManagers[zIndex] = newManager as LineManager
                SYMBOL -> symbolManagers[zIndex] = newManager as SymbolManager
            }

            return newManager as AnnotationManager<*, T, S, *, *, *>?
        }
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
            currentInfoWindow?.update()
        }
        map.addOnCameraMoveStartedListener {
            try {
                val reason = when (it) {
                    MapboxMap.OnCameraMoveStartedListener.REASON_API_GESTURE -> GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE
                    MapboxMap.OnCameraMoveStartedListener.REASON_API_ANIMATION -> GoogleMap.OnCameraMoveStartedListener.REASON_API_ANIMATION
                    MapboxMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION -> GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION
                    else -> 0
                }
                cameraMoveStartedListener?.onCameraMoveStarted(reason)
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
                if (!hasSymbolAt(latlng)) {
                    mapClickListener?.onMapClick(latlng.toGms())
                    currentInfoWindow?.close()
                    currentInfoWindow = null
                }
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

        applyMapStyle()
        options.minZoomPreference?.let { if (it != 0f) map.setMinZoomPreference(it.toDouble()) }
        options.maxZoomPreference?.let { if (it != 0f) map.setMaxZoomPreference(it.toDouble()) }
        options.latLngBoundsForCameraTarget?.let { map.setLatLngBoundsForCameraTarget(it.toMapbox()) }
        options.compassEnabled?.let { map.uiSettings.isCompassEnabled = it }
        options.rotateGesturesEnabled?.let { map.uiSettings.isRotateGesturesEnabled = it }
        options.scrollGesturesEnabled?.let { map.uiSettings.isScrollGesturesEnabled = it }
        options.tiltGesturesEnabled?.let { map.uiSettings.isTiltGesturesEnabled = it }
        options.camera?.let { map.cameraPosition = it.toMapbox() }

        synchronized(mapLock) {
            initialized = true
            waitingCameraUpdates.forEach { map.moveCamera(it) }
            val initializedCallbackList = ArrayList(internalOnInitializedCallbackList)
            Log.d(TAG, "Invoking ${initializedCallbackList.size} internal callbacks now that the true map is initialized")
            for (callback in initializedCallbackList) {
                callback.onMapReady(map)
            }
        }

        // No effect if no initialized callbacks are present.
        tryRunUserInitializedCallbacks(tag = "initMap")

        map.getStyle {
            mapView?.let { view ->
                if (loaded) return@let

                pendingFills.forEach { (zIndex, fill) ->
                    fill.update(
                        getManagerForZIndex(FILL, zIndex)!!
                    )
                }
                pendingFills.clear()
                pendingLines.forEach { (zIndex, line) ->
                    line.update(
                        getManagerForZIndex(LINE, zIndex)!!
                    )
                }
                pendingLines.clear()
                pendingMarkers.forEach { (zIndex, marker) ->
                    marker.update(
                        getManagerForZIndex(SYMBOL, zIndex)!!
                    )
                }
                pendingMarkers.clear()

                pendingBitmaps.forEach { map -> it.addImage(map.key, map.value) }
                pendingBitmaps.clear()

                map.locationComponent.apply {
                    activateLocationComponent(LocationComponentActivationOptions.builder(mapContext, it)
                            .locationEngine(this@GoogleMapImpl.locationEngine)
                            .useSpecializedLocationLayer(true)
                            .locationComponentOptions(LocationComponentOptions.builder(mapContext).pulseEnabled(true).build())
                            .build())
                    cameraMode = CameraMode.NONE
                    renderMode = RenderMode.COMPASS
                    setMaxAnimationFps(2)
                }

                synchronized(mapLock) {
                    loaded = true
                    loadedCallback?.scheduleExecute()
                }

                isMyLocationEnabled = locationEnabled

                view.visibility = View.VISIBLE
            }
        }
    }

    override fun showInfoWindow(marker: AbstractMarker): Boolean {
        infoWindowAdapter.getInfoWindowViewFor(marker, mapContext)?.let { infoView ->
            currentInfoWindow?.close()
            currentInfoWindow = InfoWindow(infoView, this, marker).also { infoWindow ->
                mapView?.let { infoWindow.open(it) }
            }
            return true
        }
        return false
    }

    internal fun addBitmap(name: String, bitmap: Bitmap) {
        val map = map
        if (map != null) {
            map.getStyle {
                it.addImage(name, bitmap)
            }
        } else {
            pendingBitmaps[name] = bitmap
        }
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        mapView?.visibility = View.VISIBLE
        if (!isStarted) {
            // onStart was not called, invoke mapView.onStart() now
            mapView?.onStart()
        }
        mapView?.onResume()
        map?.locationComponent?.let {
            if (it.isLocationComponentEnabled) {
                it.locationEngine?.requestLocationUpdates(it.locationEngineRequest, locationEngineCallback, Looper.getMainLooper())
            }
        }
    }
    override fun onPause() {
        Log.d(TAG, "onPause")
        map?.locationComponent?.let {
            if (it.isLocationComponentEnabled) {
                it.locationEngine?.removeLocationUpdates(locationEngineCallback)
            }
        }
        mapView?.onPause()
        if (!isStarted) {
            // onStart was not called, invoke mapView.onStop() now
            mapView?.onStop()
        }
    }
    override fun onDestroy() {
        Log.d(TAG, "onDestroy");
        userOnInitializedCallbackList.clear()
        lineManagers.forEach { (_, manager) -> manager.onDestroy() }
        lineManagers.clear()
        fillManagers.forEach { (_, manager) -> manager.onDestroy() }
        fillManagers.clear()
        circles.clear()
        symbolManagers.forEach { (_, manager) -> manager.onDestroy() }
        symbolManagers.clear()
        currentInfoWindow?.close()
        pendingMarkers.clear()
        markers.clear()
        BitmapDescriptorFactoryImpl.unregisterMap(map)
        view.removeView(mapView)
        // TODO can crash?
        mapView?.onDestroy()
        mapView = null

        map = null

        created = false
        initialized = false
        loaded = false
    }

    override fun onStart() {
        Log.d(TAG, "onStart")
        isStarted = true
        mapView?.onStart()
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        mapView?.visibility = View.INVISIBLE
        isStarted = false
        mapView?.onStop()
    }

    override fun onLowMemory() = mapView?.onLowMemory() ?: Unit

    override fun onSaveInstanceState(outState: Bundle) {
        val newBundle = Bundle()
        mapView?.onSaveInstanceState(newBundle)
        outState.putAll(newBundle.toGms())
    }

    fun getMapAsync(callback: IOnMapReadyCallback) {
        synchronized(mapLock) {
            userOnInitializedCallbackList.add(callback)
        }
        tryRunUserInitializedCallbacks("getMapAsync")
    }

    /**
     * Per docs, `onMapLoaded` shall only be called when the map has finished loading,
     * and some apps like Signal location sharing rely on this to behave accordingly.
     * However, MapLibre does not provide proper `onMapLoaded` callbacks.
     *
     * Workaround: schedule map loaded callback for a certain time in the future.
     */
    private fun IOnMapLoadedCallback.scheduleExecute() {
        Log.d(TAG, "Scheduling executing of OnMapLoadedCallback in ${ON_MAP_LOADED_CALLBACK_DELAY}ms, as map is now initialized.")
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Executing scheduled onMapLoaded callback")

            try {
                this.onMapLoaded()
            } catch (e: Exception) {
                Log.w(TAG, e)
            }

        }, ON_MAP_LOADED_CALLBACK_DELAY)
    }

    private var isInvokingInitializedCallbacks = AtomicBoolean(false)

    fun tryRunUserInitializedCallbacks(tag: String = "") {

        synchronized(mapLock) {
            if (userOnInitializedCallbackList.isEmpty()) return
        }

        val runCallbacks = {
            val callbacks = synchronized(mapLock) {
                ArrayList(userOnInitializedCallbackList)
                    .also { userOnInitializedCallbackList.clear() }
            }

            callbacks.forEach {
                try {
                    it.onMapReady(this)
                } catch (e: Exception) {
                    Log.w(TAG, e)
                }
            }
        }

        val map = map
        if (initialized && map != null) {
            // Call all callbacks immediately, as map is ready
            Log.d("$TAG:$tag", "Invoking callback now, as map is initialized")
            val wasCallbackActive = isInvokingInitializedCallbacks.getAndSet(true)
            runOnMainLooper(forceQueue = wasCallbackActive) {
                runCallbacks()
            }
            if (!wasCallbackActive) isInvokingInitializedCallbacks.set(false)
        } else if (mapView?.isShown == false) {
            /* If map is hidden, an app (e.g. Dott) may expect it to initialize anyway and
             * will not show the map until it is initialized. However, we should not call
             * the callback before onCreate is started (we know this is the case if mapView is
             * null), otherwise that results in other problems (e.g. Gas Now app not
             * initializing).
             */
            runOnMainLooper(forceQueue = true) {
                Log.d("$TAG:$tag", "Invoking callback now: map cannot be initialized because it is not shown (yet)")
                runCallbacks()
            }
        } else {
            Log.d("$TAG:$tag", "Initialized callbacks could not be run at this point, as the map view has not been created yet.")
            // Will be retried after initialization.
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
            if (super.onTransact(code, data, reply, flags)) {
                true
            } else {
                Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
            }

    companion object {
        private const val TAG = "GmsMap"
        private const val ON_MAP_LOADED_CALLBACK_DELAY = 500L
    }
}
