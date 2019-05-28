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

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Parcel
import android.os.RemoteException
import android.support.annotation.IdRes
import android.support.annotation.Keep
import android.support.v4.util.LongSparseArray
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.FrameLayout
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

fun <T : Any> LongSparseArray<T>.values() = (0..size()).map { valueAt(it) }.mapNotNull { it }

class GoogleMapImpl(private val context: Context, private val options: GoogleMapOptions) : IGoogleMapDelegate.Stub() {

    val view: FrameLayout
    var map: MapboxMap? = null
        private set
    val dpiFactor: Float
        get() = context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT

    private var mapView: MapView?
    private var initialized = false
    private val initializedCallbackList = mutableListOf<IOnMapReadyCallback>()
    private val mapLock = Object()
    val markers = mutableMapOf<Long, MarkerImpl>()

    private var cameraChangeListener: IOnCameraChangeListener? = null
    private var cameraMoveListener: IOnCameraMoveListener? = null
    private var cameraMoveCanceledListener: IOnCameraMoveCanceledListener? = null
    private var cameraMoveStartedListener: IOnCameraMoveStartedListener? = null
    private var cameraIdleListener: IOnCameraIdleListener? = null
    private var mapClickListener: IOnMapClickListener? = null
    private var mapLongClickListener: IOnMapLongClickListener? = null
    private var markerClickListener: IOnMarkerClickListener? = null
    private var markerDragListener: IOnMarkerDragListener? = null

    var circleManager: CircleManager? = null
    var lineManager: LineManager? = null
    var fillManager: FillManager? = null
    var symbolManager: SymbolManager? = null
    var storedMapType: Int = MAP_TYPE_NORMAL

    init {
        val mapContext = MapContext(context)
        LibraryLoader.setLibraryLoader(MultiArchLoader(mapContext, context))
        Mapbox.getInstance(mapContext, BuildConfig.MAPBOX_KEY)

        this.view = object : FrameLayout(mapContext) {
            @Keep
            fun <T : View> findViewTraversal(@IdRes id: Int): T? {
                return null
            }
        }
        this.mapView = MapView(mapContext)
        this.view.addView(this.mapView)
    }

    override fun getCameraPosition(): CameraPosition? = map?.cameraPosition?.toGms()
    override fun getMaxZoomLevel(): Float = map?.maxZoomLevel?.toFloat() ?: 20f
    override fun getMinZoomLevel(): Float = map?.minZoomLevel?.toFloat() ?: 1f

    override fun moveCamera(cameraUpdate: IObjectWrapper?) =
            cameraUpdate.unwrap<CameraUpdate>()?.let { map?.moveCamera(it) } ?: Unit

    override fun animateCamera(cameraUpdate: IObjectWrapper?) =
            cameraUpdate.unwrap<CameraUpdate>()?.let { map?.animateCamera(it) } ?: Unit

    override fun animateCameraWithCallback(cameraUpdate: IObjectWrapper?, callback: ICancelableCallback?) =
            cameraUpdate.unwrap<CameraUpdate>()?.let { map?.animateCamera(it, callback?.toMapbox()) }
                    ?: Unit

    override fun animateCameraWithDurationAndCallback(cameraUpdate: IObjectWrapper?, duration: Int, callback: ICancelableCallback?) =
            cameraUpdate.unwrap<CameraUpdate>()?.let { map?.animateCamera(it, duration, callback?.toMapbox()) }
                    ?: Unit

    override fun stopAnimation() = map?.cancelTransitions() ?: Unit

    override fun addPolyline(options: PolylineOptions): IPolylineDelegate? {
        val lineOptions = LineOptions()
                .withLatLngs(options.points.map { it.toMapbox() })
                .withLineWidth(options.width / dpiFactor)
                .withLineColor(ColorUtils.colorToRgbaString(options.color))
                .withLineOpacity(if (options.isVisible) 1f else 0f)
        return lineManager?.let { PolylineImpl(this, it.create(lineOptions)) }
    }


    override fun addPolygon(options: PolygonOptions): IPolygonDelegate? {
        Log.d(TAG, "unimplemented Method: addPolygon")
        return null
    }

    override fun addMarker(options: MarkerOptions): IMarkerDelegate? {
        var intBits = java.lang.Float.floatToIntBits(options.zIndex)
        if (intBits < 0) intBits = intBits xor 0x7fffffff

        val symbolOptions = SymbolOptions()
                .withIconOpacity(if (options.isVisible) options.alpha else 0f)
                .withIconRotate(options.rotation)
                .withZIndex(intBits)
                .withDraggable(options.isDraggable)

        options.position?.let { symbolOptions.withLatLng(it.toMapbox()) }
        options.icon?.remoteObject.unwrap<BitmapDescriptorImpl>()?.applyTo(symbolOptions, floatArrayOf(options.anchorU, options.anchorV), dpiFactor)

        val symbol = symbolManager?.create(symbolOptions) ?: return null
        val marker = MarkerImpl(this, symbol, floatArrayOf(options.anchorU, options.anchorV), options.icon?.remoteObject.unwrap<BitmapDescriptorImpl>(), options.alpha, options.title, options.snippet)
        markers.put(symbol.id, marker)
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
        val circleOptions = com.mapbox.mapboxsdk.plugins.annotation.CircleOptions()
                .withLatLng(options.center.toMapbox())
                .withCircleColor(ColorUtils.colorToRgbaString(options.fillColor))
                .withCircleRadius(options.radius.toFloat())
                .withCircleStrokeColor(ColorUtils.colorToRgbaString(options.strokeColor))
                .withCircleStrokeWidth(options.strokeWidth / dpiFactor)
                .withCircleOpacity(if (options.isVisible) 1f else 0f)
                .withCircleStrokeOpacity(if (options.isVisible) 1f else 0f)

        return circleManager?.let { CircleImpl(this, it.create(circleOptions)) }
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
            circles?.let { circleManager?.update(it) }
            lines?.let { lineManager?.update(it) }
            fills?.let { fillManager?.update(it) }
            symbols?.let { symbolManager?.update(it) }
        }

        when (storedMapType) {
            MAP_TYPE_NORMAL -> map?.setStyle(Style.Builder().fromUrl("mapbox://styles/microg/cjui4020201oo1fmca7yuwbor"), update)
            MAP_TYPE_SATELLITE -> map?.setStyle(Style.SATELLITE, update)
            MAP_TYPE_TERRAIN -> map?.setStyle(Style.OUTDOORS, update)
            MAP_TYPE_HYBRID -> map?.setStyle(Style.SATELLITE_STREETS, update)
            else -> map?.setStyle(Style.LIGHT, update)
        }

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

    fun onCreate(savedInstanceState: Bundle) {
        mapView?.onCreate(savedInstanceState.toMapbox())
        mapView?.getMapAsync(this::initMap)
    }

    private fun hasSymbolAt(latlng: com.mapbox.mapboxsdk.geometry.LatLng): Boolean {
        val point = map?.projection?.toScreenLocation(latlng) ?: return false
        val features = map?.queryRenderedFeatures(point, SymbolManager.ID_GEOJSON_LAYER)
                ?: return false
        return !features.isEmpty()
    }

    private fun initMap(map: MapboxMap) {
        if (this.map != null) return
        this.map = map

        applyMapType()
        map.getStyle {
            mapView?.let { view ->
                BitmapDescriptorFactoryImpl.registerMap(map)
                circleManager = CircleManager(view, map, it)
                lineManager = LineManager(view, map, it)
                lineManager?.lineCap = LINE_CAP_ROUND
                fillManager = FillManager(view, map, it)
                symbolManager = SymbolManager(view, map, it)
                symbolManager?.iconAllowOverlap = true
                symbolManager?.addClickListener { markers[it.id]?.let { markerClickListener?.onMarkerClick(it) } }
                symbolManager?.addDragListener(object : OnSymbolDragListener {
                    override fun onAnnotationDragStarted(annotation: Symbol?) {
                        markers[annotation?.id]?.let { markerDragListener?.onMarkerDragStart(it) }
                    }

                    override fun onAnnotationDrag(annotation: Symbol?) {
                        markers[annotation?.id]?.let { markerDragListener?.onMarkerDrag(it) }
                    }

                    override fun onAnnotationDragFinished(annotation: Symbol?) {
                        markers[annotation?.id]?.let { markerDragListener?.onMarkerDragEnd(it) }
                    }

                })
                map.addOnCameraIdleListener { cameraChangeListener?.onCameraChange(map.cameraPosition.toGms()) }
                map.addOnCameraIdleListener { cameraIdleListener?.onCameraIdle() }
                map.addOnCameraMoveListener { cameraMoveListener?.onCameraMove() }
                map.addOnCameraMoveStartedListener { cameraMoveStartedListener?.onCameraMoveStarted(it) }
                map.addOnCameraMoveCancelListener { cameraMoveCanceledListener?.onCameraMoveCanceled() }
                map.addOnMapClickListener {
                    val latlng = it
                    mapClickListener?.let { if (!hasSymbolAt(latlng)) it.onMapClick(latlng.toGms()); }
                    false
                }
                map.addOnMapLongClickListener {
                    val latlng = it
                    mapLongClickListener?.let { if (!hasSymbolAt(latlng)) it.onMapLongClick(latlng.toGms()); }
                    false
                }

                synchronized(mapLock) {
                    for (callback in initializedCallbackList) {
                        try {
                            callback.onMapReady(this)
                        } catch (e: RemoteException) {
                            Log.w(TAG, e)
                        }
                    }
                    initialized = true
                }
            }
        }
    }

    fun onResume() = mapView?.onResume()
    fun onPause() = mapView?.onPause()
    fun onDestroy() {
        circleManager?.onDestroy()
        circleManager = null
        lineManager?.onDestroy()
        lineManager = null
        fillManager?.onDestroy()
        fillManager = null
        symbolManager?.onDestroy()
        symbolManager = null
        BitmapDescriptorFactoryImpl.unregisterMap(map)
        view.removeView(mapView)
        // TODO can crash?
        mapView?.onDestroy()
        mapView = null
    }

    fun onLowMemory() = mapView?.onLowMemory()
    fun onSaveInstanceState(outState: Bundle) {
        val newBundle = Bundle()
        mapView?.onSaveInstanceState(newBundle)
        outState.putAll(newBundle.toGms())
    }

    fun getMapAsync(callback: IOnMapReadyCallback) {
        synchronized(mapLock) {
            if (initialized) {
                callback.onMapReady(this)
            } else {
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
