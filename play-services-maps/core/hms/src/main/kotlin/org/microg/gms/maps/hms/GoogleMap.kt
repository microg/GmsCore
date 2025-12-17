/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.os.*
import android.os.Build.VERSION.SDK_INT
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.IdRes
import androidx.annotation.Keep
import androidx.collection.LongSparseArray
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_TERRAIN
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.internal.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.internal.*
import com.huawei.hms.maps.CameraUpdate
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.MapsInitializer
import com.huawei.hms.maps.OnMapReadyCallback
import com.huawei.hms.maps.TextureMapView
import com.huawei.hms.maps.internal.IOnIndoorStateChangeListener
import com.huawei.hms.maps.internal.IOnInfoWindowCloseListener
import com.huawei.hms.maps.internal.IOnInfoWindowLongClickListener
import com.huawei.hms.maps.internal.IOnPoiClickListener
import com.huawei.hms.maps.model.Marker
import org.microg.gms.maps.hms.model.*
import org.microg.gms.maps.hms.utils.*
import java.util.concurrent.CopyOnWriteArrayList
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

class GoogleMapImpl(private val context: Context, var options: GoogleMapOptions) : IGoogleMapDelegate.Stub() {

    internal val mapContext = MapContext(context)

    val view: FrameLayout
    var map: HuaweiMap? = null
        private set
    val dpiFactor: Float
        get() = context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT

    private var mapView: MapView? = null
    private var created = false
    private var initialized = false
    private var loaded = false
    private val mapLock = Object()
    private var latLngBounds: LatLngBounds? = null

    private val internalOnInitializedCallbackList = CopyOnWriteArrayList<OnMapReadyCallback>()
    private val initializedCallbackList = CopyOnWriteArrayList<IOnMapReadyCallback>()
    private var loadedCallback: IOnMapLoadedCallback? = null
    private var cameraChangeListener: IOnCameraChangeListener? = null
    private var cameraMoveListener: IOnCameraMoveListener? = null
    private var cameraMoveCanceledListener: IOnCameraMoveCanceledListener? = null
    private var cameraMoveStartedListener: IOnCameraMoveStartedListener? = null
    private var cameraIdleListener: IOnCameraIdleListener? = null
    private var mapClickListener: IOnMapClickListener? = null
    private var mapLongClickListener: IOnMapLongClickListener? = null

    private val groundOverlays = mutableMapOf<String, GroundOverlayImpl>()
    private val polylines = mutableMapOf<String, PolylineImpl>()
    private val polygons = mutableMapOf<String, PolygonImpl>()
    private val circles = mutableMapOf<String, CircleImpl>()
    private val tileOverlays = mutableMapOf<String, TileOverlayImpl>()

    private var storedMapType: Int = options.mapType
    val waitingCameraUpdates = mutableListOf<CameraUpdate>()

    private var markerId = 0L
    val markers = mutableMapOf<String, MarkerImpl>()

    private var projectionImpl: ProjectionImpl? = null
    private var inDeveloperAnimation = false

    init {
        BitmapDescriptorFactoryImpl.initialize(context.resources)
        runOnMainLooper {
            MapsInitializer.setApiKey(BuildConfig.HMSMAP_KEY)
        }

        this.view = object : FrameLayout(mapContext) {
            private val fakeWatermark = ImageView(mapContext).apply {
                tag = "GoogleWatermark"
                visibility = GONE
            }
            private val fakeCompass = View(mapContext).apply {
                tag = "GoogleMapCompass"
                visibility = GONE
            }
            private val fakeZoomInButton = View(mapContext).apply {
                tag = "GoogleMapZoomInButton"
                visibility = GONE
            }
            private val fakeZoomOutButton = View(mapContext).apply {
                tag = "GoogleMapZoomOutButton"
                visibility = GONE
            }
            private val fakeMyLocationButton = View(mapContext).apply {
                tag = "GoogleMapMyLocationButton"
                visibility = GONE
            }

            private val fakeZoomButtonRoot = LinearLayout(mapContext).apply {
                addView(fakeZoomInButton)
                addView(fakeZoomOutButton)
                visibility = GONE
            }

            private val mapButtonRoot = RelativeLayout(mapContext).apply {
                addView(fakeZoomButtonRoot)
                addView(fakeMyLocationButton)
                addView(fakeCompass)
                addView(fakeWatermark)
                visibility = GONE
            }

            override fun onAttachedToWindow() {
                super.onAttachedToWindow()
                addView(mapButtonRoot)
            }

            override fun onDetachedFromWindow() {
                super.onDetachedFromWindow()
                removeView(mapButtonRoot)
            }

            @Keep
            fun <T : View> findViewTraversal(@IdRes id: Int): T? {
                if (1 == id) {
                    return try {
                        @Suppress("UNCHECKED_CAST")
                        mapButtonRoot as T
                    } catch (e: ClassCastException) {
                        null
                    }
                }
                if (2 == id) {
                    return try {
                        @Suppress("UNCHECKED_CAST")
                        fakeMyLocationButton as T
                    } catch (e: ClassCastException) {
                        null
                    }
                }
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
                if ("GoogleMapZoomInButton" == tag) {
                    return try {
                        @Suppress("UNCHECKED_CAST")
                        fakeZoomInButton as T
                    } catch (e: ClassCastException) {
                        null
                    }
                }
                if ("GoogleMapZoomOutButton" == tag) {
                    return try {
                        @Suppress("UNCHECKED_CAST")
                        fakeZoomOutButton as T
                    } catch (e: ClassCastException) {
                        null
                    }
                }
                if ("GoogleMapMyLocationButton" == tag) {
                    return try {
                        @Suppress("UNCHECKED_CAST")
                        fakeMyLocationButton as T
                    } catch (e: ClassCastException) {
                        null
                    }
                }
                return null
            }
        }
    }

    override fun getCameraPosition(): CameraPosition {
        return map?.cameraPosition?.toGms() ?: CameraPosition(LatLng(0.0, 0.0), 0f, 0f, 0f)
    }
    override fun getMaxZoomLevel(): Float = toHmsZoom(map?.maxZoomLevel ?: 18.toFloat())
    override fun getMinZoomLevel(): Float = toHmsZoom(map?.minZoomLevel ?: 3.toFloat())

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

    override fun animateCameraWithCallback(cameraUpdate: IObjectWrapper?, callback: ICancelableCallback?) {
        val update = cameraUpdate.unwrap<CameraUpdate>() ?: return
        synchronized(mapLock) {
            if (initialized) {
                Log.d(TAG, "animateCameraWithCallback start ")
                inDeveloperAnimation = true
                this.map?.animateCamera(update, object : HuaweiMap.CancelableCallback {
                    override fun onFinish() {
                        Log.d(TAG, "animateCameraWithCallback onFinish: ")
                        inDeveloperAnimation = false
                        callback?.onFinish()
                    }

                    override fun onCancel() {
                        Log.d(TAG, "animateCameraWithCallback onCancel: ")
                        inDeveloperAnimation = false
                        callback?.onCancel()
                    }
                })
            } else {
                waitingCameraUpdates.add(update)
                afterInitialize { callback?.onFinish() }
            }
        }
    }

    override fun animateCameraWithDurationAndCallback(
            cameraUpdate: IObjectWrapper?,
            duration: Int,
            callback: ICancelableCallback?
    ) {
        val update = cameraUpdate.unwrap<CameraUpdate>() ?: return
        synchronized(mapLock) {
            if (initialized) {
                Log.d(TAG, "animateCameraWithDurationAndCallback")
                this.map?.animateCamera(update, duration, callback?.toHms())
            } else {
                waitingCameraUpdates.add(update)
                afterInitialize { callback?.onFinish() }
            }
        }
    }

    override fun stopAnimation() = map?.stopAnimation() ?: Unit

    override fun setMapStyle(options: MapStyleOptions?): Boolean {
        Log.d(TAG, "setMapStyle: ")
        val bool = options?.toHms(mapContext).let {
            map?.setMapStyle(it)
        }
        Log.d(TAG, "setMapStyle: bool: $bool")
        return true == bool
    }

    override fun setMinZoomPreference(minZoom: Float) = afterInitialize {
        it.setMinZoomPreference(toHmsZoom(minZoom))
    }

    override fun setMaxZoomPreference(maxZoom: Float) = afterInitialize {
        it.setMaxZoomPreference(toHmsZoom(maxZoom))
    }

    override fun resetMinMaxZoomPreference() = afterInitialize {
        it.setMinZoomPreference(3.toFloat())
        it.setMaxZoomPreference(18.toFloat())
    }

    override fun setLatLngBoundsForCameraTarget(bounds: LatLngBounds?) = afterInitialize {
        if (latLngBounds == null || bounds == null || latLngBounds!! != bounds) {
            latLngBounds = bounds
            it.setLatLngBoundsForCameraTarget(bounds?.toHms())
        }
    }

    override fun addPolyline(options: PolylineOptions): IPolylineDelegate? {
        val polyline = map?.addPolyline(options.toHms()) ?: return null
        val polylineImpl = PolylineImpl(polyline, options)
        polylines[polylineImpl.id] = polylineImpl
        return polylineImpl
    }

    override fun addPolygon(options: PolygonOptions): IPolygonDelegate? {
        val polygon = map?.addPolygon(options.toHms()) ?: return null
        val polygonImpl = PolygonImpl(polygon)
        polygons[polygonImpl.id] = polygonImpl
        return polygonImpl
    }

    override fun addMarker(options: MarkerOptions): IMarkerDelegate {
        val marker = MarkerImpl(this, "m${markerId++}", options)
        if (map != null) {
            marker.update()
        } else {
            markers[marker.id] = marker
        }
        return marker
    }

    override fun addGroundOverlay(options: GroundOverlayOptions): IGroundOverlayDelegate? {
        Log.d(TAG, "Method: addGroundOverlay")
        if (options.width <= 0 && options.height <= 0 && options.bounds == null) {
            Log.w(TAG, "addGroundOverlay options Parameters do not meet requirements")
            return null
        }
        val groundOverlay = map?.addGroundOverlay(options.toHms()) ?: return null
        val groundOverlayImpl = GroundOverlayImpl(groundOverlay)
        groundOverlays[groundOverlayImpl.id] = groundOverlayImpl
        return groundOverlayImpl
    }

    override fun addTileOverlay(options: TileOverlayOptions): ITileOverlayDelegate? {
        Log.d(TAG, "Method: addTileOverlay")
        val tileOverlay = map?.addTileOverlay(options.toHms()) ?: return null
        val tileOverlayImpl = TileOverlayImpl(tileOverlay)
        tileOverlays[tileOverlayImpl.id] = tileOverlayImpl
        return tileOverlayImpl
    }

    override fun addCircle(options: CircleOptions): ICircleDelegate? {
        val circle = map?.addCircle(options.toHms()) ?: return null
        val circleImpl = CircleImpl(circle)
        circles[circleImpl.id] = circleImpl
        return circleImpl
    }

    override fun clear() {
        map?.clear()
    }


    override fun getMapType(): Int {
        return map?.mapType ?: storedMapType
    }

    override fun setMapType(type: Int) {
        storedMapType = type
        applyMapType()
    }

    fun applyMapType() {
        // TODO: Serve map styles locally
        Log.d(TAG, "Method: applyMapType -> $storedMapType")
        when (storedMapType) {
            MAP_TYPE_TERRAIN -> map?.mapType = HuaweiMap.MAP_TYPE_TERRAIN
            // MAP_TYPE_SATELLITE, MAP_TYPE_HYBRID, MAP_TYPE_NONE, MAP_TYPE_NORMAL,
            else -> map?.mapType = HuaweiMap.MAP_TYPE_NORMAL
        }
        // map?.let { BitmapDescriptorFactoryImpl.registerMap(it) }
    }

    override fun isTrafficEnabled(): Boolean {
        return map?.isTrafficEnabled ?: false
    }

    override fun setTrafficEnabled(traffic: Boolean) = afterInitialize {
        Log.d(TAG, "setTrafficEnabled")
        it.isTrafficEnabled = traffic
    }

    override fun isIndoorEnabled(): Boolean {
        Log.d(TAG, "isIndoorEnabled")
        return map?.isIndoorEnabled ?: false
    }

    override fun setIndoorEnabled(indoor: Boolean) = afterInitialize {
        Log.d(TAG, "setIndoorEnabled")
        it.isIndoorEnabled = indoor
    }

    override fun isMyLocationEnabled(): Boolean {
        return map?.isMyLocationEnabled ?: false
    }

    override fun setMyLocationEnabled(myLocation: Boolean) = afterInitialize {
        Log.d(TAG, "setMyLocationEnabled $myLocation")
        it.isMyLocationEnabled = myLocation
    }

    override fun getMyLocation(): Location? {
        Log.d(TAG, "deprecated Method: getMyLocation")
        return null
    }

    override fun setLocationSource(locationSource: ILocationSourceDelegate?) = afterInitialize {
        Log.d(TAG, "unimplemented Method: setLocationSource")
    }

    override fun setContentDescription(desc: String?) = afterInitialize {
        Log.d(TAG, "setContentDescription desc -> $desc")
        it.setContentDescription(desc)
    }

    override fun getUiSettings(): IUiSettingsDelegate =
        map?.uiSettings?.let { UiSettingsImpl(it, view) } ?: UiSettingsCache(view).also {
            internalOnInitializedCallbackList.add(it.getMapReadyCallback())
        }

    override fun getProjection(): IProjectionDelegate {
        return projectionImpl ?: map?.projection?.let {
            val experiment = try {
                map?.cameraPosition?.tilt == 0.0f && map?.cameraPosition?.bearing == 0.0f
            } catch (e: Exception) {
                Log.w(TAG, e);false
            }
            ProjectionImpl(it, experiment)
        }?.also { projectionImpl = it } ?: DummyProjection()
    }

    override fun setOnCameraChangeListener(listener: IOnCameraChangeListener?) = afterInitialize {
        Log.d(TAG, "setOnCameraChangeListener");
        cameraChangeListener = listener
    }

    override fun setOnCircleClickListener(listener: IOnCircleClickListener?) = afterInitialize { hmap ->
        Log.d(TAG, "setOnCircleClickListener")
        hmap.setOnCircleClickListener { listener?.onCircleClick(circles[it.id]) }
    }

    override fun setOnGroundOverlayClickListener(listener: IOnGroundOverlayClickListener?) =
            afterInitialize { hmap ->
                Log.d(TAG, "Method: setOnGroundOverlayClickListener")
                hmap.setOnGroundOverlayClickListener { listener?.onGroundOverlayClick(groundOverlays[it.id]) }
            }

    override fun setOnInfoWindowLongClickListener(listener: com.google.android.gms.maps.internal.IOnInfoWindowLongClickListener?) =
            afterInitialize {
                Log.d(TAG, "Not yet implemented setInfoWindowLongClickListener")
            }

    fun setOnIndoorStateChangeListener(listener: IOnIndoorStateChangeListener?) {
        Log.d(TAG, "unimplemented Method: setOnIndoorStateChangeListener")
    }

    override fun setOnMapClickListener(listener: IOnMapClickListener?) = afterInitialize {
        mapClickListener = listener
        it.setOnMapClickListener { latlng ->
            try {
                mapClickListener?.onMapClick(latlng.toGms())
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun setOnMapLongClickListener(listener: IOnMapLongClickListener?) = afterInitialize {
        mapLongClickListener = listener
        it.setOnMapLongClickListener { latlng ->
            try {
                mapLongClickListener?.onMapLongClick(latlng.toGms())
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun setOnMarkerClickListener(listener: IOnMarkerClickListener?) = afterInitialize { hmap ->
        hmap.setOnMarkerClickListener {
            Log.d("GmsGoogleMap", "setOnMarkerClickListener marker id -> ${it.id}")
            listener?.onMarkerClick(markers[it.id]) ?: false
        }
    }

    override fun setOnMarkerDragListener(listener: IOnMarkerDragListener?) = afterInitialize {
        it.setOnMarkerDragListener(object : HuaweiMap.OnMarkerDragListener {
            override fun onMarkerDragStart(p0: Marker?) {
                listener?.onMarkerDragStart(markers[p0?.id])
            }

            override fun onMarkerDrag(p0: Marker?) {
                listener?.onMarkerDrag(markers[p0?.id])
            }

            override fun onMarkerDragEnd(p0: Marker?) {
                listener?.onMarkerDragEnd(markers[p0?.id])
            }
        })
    }

    override fun setOnInfoWindowClickListener(listener: IOnInfoWindowClickListener?) = afterInitialize { hmap ->
        Log.d(TAG, "setOnInfoWindowClickListener")
        hmap.setOnInfoWindowClickListener { listener?.onInfoWindowClick(markers[it.id]) }
    }

    fun setOnInfoWindowCloseListener(listener: IOnInfoWindowCloseListener?) {
        Log.d(TAG, "unimplemented Method: setOnInfoWindowCloseListener")
    }

    fun setOnInfoWindowLongClickListener(listener: IOnInfoWindowLongClickListener?) {
        Log.d(TAG, "unimplemented Method: setOnInfoWindowLongClickListener")
    }

    override fun setInfoWindowAdapter(adapter: IInfoWindowAdapter?) = afterInitialize {
        Log.d(TAG, "setInfoWindowAdapter")
        it.setInfoWindowAdapter(object : HuaweiMap.InfoWindowAdapter {
            override fun getInfoContents(p0: Marker?): View? {
                return adapter?.getInfoContents(markers[p0?.id]).unwrap<View>()
            }

            override fun getInfoWindow(p0: Marker?): View? {
                return adapter?.getInfoWindow(markers[p0?.id]).unwrap<View>()
            }

        })
    }

    override fun setOnMyLocationChangeListener(listener: IOnMyLocationChangeListener?) = afterInitialize {
        Log.d(TAG, "deprecated Method: setOnMyLocationChangeListener")
    }

    override fun setOnMyLocationButtonClickListener(listener: IOnMyLocationButtonClickListener?) = afterInitialize {
        Log.d(TAG, "setOnMyLocationButtonClickListener")
        it.setOnMyLocationButtonClickListener { listener?.onMyLocationButtonClick() ?: false }
    }

    override fun setOnMyLocationClickListener(listener: IOnMyLocationClickListener?) = afterInitialize { hmap ->
        Log.d(TAG, "setOnMyLocationClickListener")
        hmap.setOnMyLocationClickListener { listener?.onMyLocationClick(it) }
    }

    fun setOnPoiClickListener(listener: IOnPoiClickListener?) {
        Log.d(TAG, "unimplemented Method: setOnPoiClickListener")
    }

    override fun setOnPolygonClickListener(listener: IOnPolygonClickListener?) = afterInitialize { hmap ->
        Log.d(TAG, "setOnPolygonClickListener")
        hmap.setOnPolygonClickListener { listener?.onPolygonClick(polygons[it.id]) }
    }

    override fun setOnInfoWindowCloseListener(listener: com.google.android.gms.maps.internal.IOnInfoWindowCloseListener?) =
            afterInitialize {
                Log.d(TAG, "Not yet implemented setInfoWindowCloseListener")
            }


    override fun setOnPolylineClickListener(listener: IOnPolylineClickListener?) = afterInitialize { hmap ->
        Log.d(TAG, "unimplemented Method: setOnPolylineClickListener")
        hmap.setOnPolylineClickListener { listener?.onPolylineClick(polylines[it.id]) }
    }

    override fun snapshot(callback: ISnapshotReadyCallback, bitmap: IObjectWrapper?) = afterInitialize {
        Log.d(TAG, "snapshot")
        val hmsBitmap = bitmap.unwrap<Bitmap>() ?: return@afterInitialize
        val hmsCallback = HuaweiMap.SnapshotReadyCallback { p0 -> callback.onBitmapReady(p0) }
        it.snapshot(hmsCallback, hmsBitmap)
    }

    override fun snapshotForTest(callback: ISnapshotReadyCallback) = afterInitialize {
        Log.d(TAG, "snapshotForTest")
        val hmsCallback = HuaweiMap.SnapshotReadyCallback { p0 -> callback.onBitmapReady(p0) }
        it.snapshot(hmsCallback)
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) = afterInitialize {
        Log.d(TAG, "setPadding: $left $top $right $bottom")
        it.setPadding(left, top, right, bottom)
    }

    override fun isBuildingsEnabled(): Boolean {
        Log.d(TAG, "isBuildingsEnabled")
        return map?.isBuildingsEnabled ?: true
    }

    override fun setBuildingsEnabled(buildings: Boolean) = afterInitialize {
        Log.d(TAG, "setBuildingsEnabled: $buildings")
        it.isBuildingsEnabled = buildings
    }

    override fun setOnMapLoadedCallback(callback: IOnMapLoadedCallback?) {
        if (callback != null) {
            synchronized(mapLock) {
                if (loaded) {
                    Log.d(TAG, "Invoking callback instantly, as map is loaded")
                    try {
                        callback.onMapLoaded()
                    } catch (e: Exception) {
                        Log.w(TAG, e)
                    }
                } else {
                    Log.d(TAG, "Delay callback invocation, as map is not yet loaded")
                    loadedCallback = callback
                }
            }
        } else {
            loadedCallback = null
        }
    }

    override fun setCameraMoveStartedListener(listener: IOnCameraMoveStartedListener?) = afterInitialize { hmap ->
        Log.d(TAG, "setCameraMoveStartedListener")
        cameraMoveStartedListener = listener
        hmap.setOnCameraMoveStartedListener {
            if (it == HuaweiMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION && inDeveloperAnimation) {
                Log.d(TAG, "onCameraMoveStarted <inDeveloperAnimation> skipped: 1st ")
                return@setOnCameraMoveStartedListener
            }
            try {
                Log.d(TAG, "Listener: onCameraMoveStarted: ")
                cameraMoveStartedListener?.onCameraMoveStarted(it)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun setCameraMoveListener(listener: IOnCameraMoveListener?) = afterInitialize {
        Log.d(TAG, "setCameraMoveListener")
        cameraMoveListener = listener
        it.setOnCameraMoveListener {
            try {
                Log.d(TAG, "Listener: onCameraMove: ")
                if (SDK_INT >= 26) {
                    mapView?.let { it.parent?.onDescendantInvalidated(it, it) }
                }
                map?.let { projectionImpl?.updateProjectionState(it.cameraPosition, it.projection) }
                cameraMoveListener?.onCameraMove()
                cameraChangeListener?.onCameraChange(map?.cameraPosition?.toGms())
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun setCameraMoveCanceledListener(listener: IOnCameraMoveCanceledListener?) = afterInitialize {
        Log.d(TAG, "setCameraMoveCanceledListener")
        cameraMoveCanceledListener = listener
        it.setOnCameraMoveCanceledListener {
            try {
                Log.d(TAG, "setOnCameraMoveCanceledListener: ")
                cameraMoveCanceledListener?.onCameraMoveCanceled()
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun setCameraIdleListener(listener: IOnCameraIdleListener?) = afterInitialize {
        Log.d(TAG, "setCameraIdleListener")
        cameraIdleListener = listener
    }

    override fun getTestingHelper(): IObjectWrapper? {
        Log.d(TAG, "unimplemented Method: getTestingHelper")
        return null
    }

    override fun setWatermarkEnabled(watermark: Boolean) {
        Log.d(TAG, "unimplemented Method: setWatermarkEnabled")
    }

    override fun useViewLifecycleWhenInFragment(): Boolean {
        Log.d(TAG, "unimplemented Method: useViewLifecycleWhenInFragment")
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!created) {
            Log.d(TAG_LOGO, "create: ${context.packageName},\n$options")
            MapsInitializer.initialize(mapContext)
            val mapView = runCatching { 
                TextureMapView(mapContext, options.toHms())
            }.onFailure {
                Log.w(TAG, "onCreate: init TextureMapView error ", it)
            }.getOrDefault(MapView(mapContext, options.toHms())).apply { visibility = View.INVISIBLE }
            this.mapView = mapView
            view.addView(mapView)
            mapView.onCreate(savedInstanceState?.toHms())
            mapView.getMapAsync(this::initMap)

            created = true
            runOnMainLooper(forceQueue = true) { tryRunUserInitializedCallbacks("onCreate") }
        }
    }

    private fun fakeWatermark(method: () -> Unit) {
        Log.d(TAG_LOGO, "start -> $view")
        val view1 = view.getChildAt(0) as? ViewGroup
        val view2 = view1?.getChildAt(0) as? ViewGroup
        val view4 = view2?.getChildAt(1)
        Log.d(TAG_LOGO, view4?.toString() ?: "view4 is null")
        if (view4 is LinearLayout) {
            view4.visibility = View.GONE
            method()
        } else {
            Log.d(TAG_LOGO, "LinearLayout not found")
        }
    }

    private fun getAllChildViews(view: View, index: Int): List<View>? {
        Log.d(TAG_LOGO, "getAllChildViews: $index, $view")
        if (view is LinearLayout) {
            Log.d(TAG_LOGO, "legal: $index")
            view.visibility = View.GONE
        }
        val allChildren: MutableList<View> = ArrayList()
        if (view is ViewGroup) {
            val vp = view
            for (i in 0 until vp.childCount) {
                val viewChild = vp.getChildAt(i)
                Log.d(TAG_LOGO, "child:$index, $i, $viewChild")
                allChildren.add(viewChild)
                allChildren.addAll(getAllChildViews(viewChild, index + 1)!!)
            }
        }
        return allChildren
    }

    private fun initMap(map: HuaweiMap) {
        if (this.map != null && initialized) return

        loaded = true
        this.map = map

        map.setOnCameraIdleListener {
            Log.d(TAG, "initMap: onCameraIdle: ")
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

        map.setOnCameraMoveListener {
            Log.d(TAG, "initMap: onCameraMove: ")
            try {
                if (SDK_INT >= 26) {
                    mapView?.let { it.parent?.onDescendantInvalidated(it, it) }
                }
                map.let { projectionImpl?.updateProjectionState(it.cameraPosition, it.projection) }
                cameraMoveListener?.onCameraMove()
                cameraChangeListener?.onCameraChange(map.cameraPosition?.toGms())
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        map.setOnCameraMoveStartedListener {
            Log.d(TAG, "initMap: onCameraMoveStarted: $it")
            try {
                cameraMoveStartedListener?.onCameraMoveStarted(it)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        map.setOnCameraMoveCanceledListener {
            Log.d(TAG, "initMap: onCameraMoveCanceled: ")
            try {
                cameraMoveCanceledListener?.onCameraMoveCanceled()
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        map.setOnMapClickListener { latlng ->
            try {
                if (options.liteMode) {
                    val parentView = view.parent?.parent
                    // TODO hms not support disable click listener when liteMode, this just fix for teams
                    if (parentView != null && parentView::class.qualifiedName.equals("com.microsoft.teams.location.ui.map.MapViewLite")) {
                        val clickView = parentView as ViewGroup
                        clickView.performClick()
                        return@setOnMapClickListener
                    }
                }
                mapClickListener?.onMapClick(latlng.toGms())
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        map.setOnMapLongClickListener { latlng ->
            try {
                if (options.liteMode) {
                    val parentView = view.parent?.parent
                    // TODO hms not support disable click listener when liteMode, this just fix for teams
                    if (parentView != null && parentView::class.qualifiedName.equals("com.microsoft.teams.location.ui.map.MapViewLite")) {
                        val clickView = parentView as ViewGroup
                        clickView.performLongClick()
                        return@setOnMapLongClickListener
                    }
                }
                mapLongClickListener?.onMapLongClick(latlng.toGms())
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }

        synchronized(mapLock) {
            initialized = true
            markers.filter { it.key.startsWith("m") }.forEach { it.value.update() }
            waitingCameraUpdates.forEach { map.moveCamera(it) }
            val initializedCallbacks = ArrayList(internalOnInitializedCallbackList)
            Log.d(TAG, "Invoking ${initializedCallbacks.size} internal callbacks now that the true map is initialized")
            for (callback in initializedCallbacks) {
                callback.onMapReady(map)
            }
            internalOnInitializedCallbackList.clear()
            fakeWatermark { Log.d(TAG_LOGO, "fakeWatermark success") }

            mapView?.visibility = View.VISIBLE
        }

        tryRunUserInitializedCallbacks(tag = "initMap")
    }

    override fun onResume() {
        mapView?.visibility = View.VISIBLE
        mapView?.onResume()
    }
    override fun onPause() = mapView?.onPause() ?: Unit
    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        initializedCallbackList.clear()
        internalOnInitializedCallbackList.clear()
        circles.map { it.value.remove() }
        circles.clear()
        polylines.map { it.value.remove() }
        polylines.clear()
        polygons.map { it.value.remove() }
        polygons.clear()
        markers.map { it.value.remove() }
        markers.clear()
//        BitmapDescriptorFactoryImpl.unregisterMap(map)
        view.removeView(mapView)
        // TODO can crash?
        mapView?.onDestroy()
        mapView = null

        // Don't make it null; this object is not deleted immediately, and it may want to access map.* stuff
        //map = null

        created = false
        initialized = false
        loaded = false
    }

    override fun onStart() {
        mapView?.onStart()
    }

    override fun onStop() {
        mapView?.visibility = View.INVISIBLE
        mapView?.onStop()
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
            initializedCallbackList.add(callback)
        }
        tryRunUserInitializedCallbacks("getMapAsync")
    }

    private fun afterInitialize(runnable: (HuaweiMap) -> Unit) {
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

    private var isInvokingInitializedCallbacks = AtomicBoolean(false)
    private fun tryRunUserInitializedCallbacks(tag: String = "") {

        synchronized(mapLock) {
            if (initializedCallbackList.isEmpty()) return
        }

        val runCallbacks = {
            val callbacks = synchronized(mapLock) {
                ArrayList(initializedCallbackList)
                    .also { initializedCallbackList.clear() }
            }

            callbacks.forEach {
                try {
                    it.onMapReady(this)
                } catch (e: Exception) {
                    Log.w(TAG, e)
                }
            }
        }

        if (initialized && map != null) {
            Log.d("$TAG:$tag", "Invoking callback now, as map is initialized")
            val wasCallbackActive = isInvokingInitializedCallbacks.getAndSet(true)
            runOnMainLooper(forceQueue = wasCallbackActive) {
                runCallbacks()
            }
            if (!wasCallbackActive) isInvokingInitializedCallbacks.set(false)
        } else {
            Log.d(
                    "$TAG:$tag",
                    "Initialized callbacks could not be run at this point, as the map view has not been created yet."
            )
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
            if (super.onTransact(code, data, reply, flags)) {
                Log.d(TAG, "onTransact: $code, $data, $flags")
                true
            } else {
                Log.w(TAG, "onTransact [unknown]: $code, $data, $flags"); false
            }

    companion object {
        private const val TAG = "GmsGoogleMap"

        private const val TAG_LOGO = "fakeWatermark"
        private const val MAX_TIMES = 300
    }
}
