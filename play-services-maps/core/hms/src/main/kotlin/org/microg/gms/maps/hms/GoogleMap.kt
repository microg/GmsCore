/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.collection.LongSparseArray
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_SATELLITE
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_TERRAIN
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.internal.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.internal.*
import com.huawei.hms.maps.CameraUpdate
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.HuaweiMapOptions
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.MapsInitializer
import com.huawei.hms.maps.internal.IOnIndoorStateChangeListener
import com.huawei.hms.maps.internal.IOnInfoWindowCloseListener
import com.huawei.hms.maps.internal.IOnInfoWindowLongClickListener
import com.huawei.hms.maps.internal.IOnPoiClickListener
import com.huawei.hms.maps.model.Marker
import org.microg.gms.common.Constants
import org.microg.gms.maps.hms.model.*
import org.microg.gms.maps.hms.utils.*


private fun <T : Any> LongSparseArray<T>.values() = (0 until size()).mapNotNull { valueAt(it) }

fun runOnMainLooper(method: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        method()
    } else {
        Handler(Looper.getMainLooper()).post {
            method()
        }
    }
}

class GoogleMapImpl(private val context: Context, var options: GoogleMapOptions) : IGoogleMapDelegate.Stub() {

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

    private val initializedCallbackList = mutableListOf<IOnMapReadyCallback>()
    private var loadedCallback: IOnMapLoadedCallback? = null
    private var cameraChangeListener: IOnCameraChangeListener? = null
    private var cameraMoveListener: IOnCameraMoveListener? = null
    private var cameraMoveCanceledListener: IOnCameraMoveCanceledListener? = null
    private var cameraMoveStartedListener: IOnCameraMoveStartedListener? = null
    private var cameraIdleListener: IOnCameraIdleListener? = null
    private var mapClickListener: IOnMapClickListener? = null
    private var mapLongClickListener: IOnMapLongClickListener? = null

    private val groundOverlays = mutableMapOf<String, GroundOverlayImpl>()
    private val markers = mutableMapOf<String, MarkerImpl>()
    private val polylines = mutableMapOf<String, PolylineImpl>()
    private val polygons = mutableMapOf<String, PolygonImpl>()
    private val circles = mutableMapOf<String, CircleImpl>()
    private val tileOverlays = mutableMapOf<String, TileOverlayImpl>()

    private var storedMapType: Int = options.mapType
    val waitingCameraUpdates = mutableListOf<CameraUpdate>()
    var locationEnabled: Boolean = false

    init {
        val mapContext = MapContext(context)
        BitmapDescriptorFactoryImpl.initialize(context.resources)
        runOnMainLooper {
            MapsInitializer.setApiKey(BuildConfig.HMSMAP_KEY)
        }

        this.view = object : FrameLayout(mapContext) {}
    }

    override fun getCameraPosition(): CameraPosition? = map?.cameraPosition?.toGms()
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

    fun afterInitialized(runnable: () -> Unit) {
        initializedCallbackList.add(object : IOnMapReadyCallback {
            override fun onMapReady(map: IGoogleMapDelegate?) {
                runnable()
            }

            override fun asBinder(): IBinder? = null
        })
    }

    override fun animateCameraWithCallback(cameraUpdate: IObjectWrapper?, callback: ICancelableCallback?) {
        val update = cameraUpdate.unwrap<CameraUpdate>() ?: return
        synchronized(mapLock) {
            if (initialized) {
                this.map?.animateCamera(update, callback?.toHms())
            } else {
                waitingCameraUpdates.add(update)
                afterInitialized { callback?.onFinish() }
            }
        }
    }

    override fun animateCameraWithDurationAndCallback(cameraUpdate: IObjectWrapper?, duration: Int, callback: ICancelableCallback?) {
        val update = cameraUpdate.unwrap<CameraUpdate>() ?: return
        synchronized(mapLock) {
            if (initialized) {
                this.map?.animateCamera(update, duration, callback?.toHms())
            } else {
                waitingCameraUpdates.add(update)
                afterInitialized { callback?.onFinish() }
            }
        }
    }

    override fun stopAnimation() = map?.stopAnimation() ?: Unit

    override fun setMapStyle(options: MapStyleOptions?): Boolean {
        Log.d(TAG, "unimplemented Method: setMapStyle ${options?.getJson()}")
        return true
    }

    override fun setMinZoomPreference(minZoom: Float) {
        map?.setMinZoomPreference(toHmsZoom(minZoom))
    }

    override fun setMaxZoomPreference(maxZoom: Float) {
        map?.setMaxZoomPreference(toHmsZoom(maxZoom))
    }

    override fun resetMinMaxZoomPreference() {
        map?.setMinZoomPreference(3.toFloat())
        map?.setMaxZoomPreference(18.toFloat())
    }

    override fun setLatLngBoundsForCameraTarget(bounds: LatLngBounds?) {
        map?.setLatLngBoundsForCameraTarget(bounds?.toHms())
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

    override fun addMarker(options: MarkerOptions): IMarkerDelegate? {
        val marker = map?.addMarker(options.toHms()) ?: return null
        val markerImpl = MarkerImpl(marker)
        markers[markerImpl.id] = markerImpl
        return markerImpl
    }

    override fun addGroundOverlay(options: GroundOverlayOptions): IGroundOverlayDelegate? {
        Log.d(TAG, "Method: addGroundOverlay")
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
        when (storedMapType) {
            MAP_TYPE_SATELLITE -> map?.mapType = HuaweiMap.MAP_TYPE_SATELLITE
            MAP_TYPE_TERRAIN -> map?.mapType = HuaweiMap.MAP_TYPE_TERRAIN
            MAP_TYPE_HYBRID -> map?.mapType = HuaweiMap.MAP_TYPE_HYBRID
            //MAP_TYPE_NONE, MAP_TYPE_NORMAL,
            else -> map?.mapType = HuaweiMap.MAP_TYPE_NORMAL
        }
        // map?.let { BitmapDescriptorFactoryImpl.registerMap(it) }
    }

    override fun isTrafficEnabled(): Boolean {
        return map?.isTrafficEnabled ?: false
    }

    override fun setTrafficEnabled(traffic: Boolean) {
        Log.d(TAG, "setTrafficEnabled")
        map?.isTrafficEnabled = traffic
    }

    override fun isIndoorEnabled(): Boolean {
        Log.d(TAG, "isIndoorEnabled")
        return map?.isIndoorEnabled ?: false
    }

    override fun setIndoorEnabled(indoor: Boolean) {
        Log.d(TAG, "setIndoorEnabled")
        map?.isIndoorEnabled = indoor
    }

    override fun isMyLocationEnabled(): Boolean {
        return map?.isMyLocationEnabled ?: false
    }

    override fun setMyLocationEnabled(myLocation: Boolean) {
        map?.isMyLocationEnabled = myLocation
    }

    override fun getMyLocation(): Location? {
        Log.d(TAG, "deprecated Method: getMyLocation")
        return null
    }

    override fun setLocationSource(locationSource: ILocationSourceDelegate?) {
        Log.d(TAG, "unimplemented Method: setLocationSource")
    }

    override fun setContentDescription(desc: String?) {
        map?.setContentDescription(desc)
    }

    override fun getUiSettings(): IUiSettingsDelegate? = map?.uiSettings?.let { UiSettingsImpl(it) }

    override fun getProjection(): IProjectionDelegate? = map?.projection?.let {
        Log.d(TAG, "getProjection")
        ProjectionImpl(it)
    }

    override fun setOnCameraChangeListener(listener: IOnCameraChangeListener?) {
        Log.d(TAG, "setOnCameraChangeListener");
        cameraChangeListener = listener
        map?.setOnCameraIdleListener {
            try {
                cameraChangeListener?.onCameraChange(map?.cameraPosition?.toGms())
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun setOnCircleClickListener(listener: IOnCircleClickListener?) {
        Log.d(TAG, "setOnCircleClickListener")
        map?.setOnCircleClickListener { listener?.onCircleClick(circles[it.id]) }
    }

    override fun setOnGroundOverlayClickListener(listener: IOnGroundOverlayClickListener?) {
        Log.d(TAG, "Method: setOnGroundOverlayClickListener")
        map?.setOnGroundOverlayClickListener { listener?.onGroundOverlayClick(groundOverlays[it.id]) }
    }

    override fun setOnInfoWindowLongClickListener(listener: com.google.android.gms.maps.internal.IOnInfoWindowLongClickListener?) {
        Log.d(TAG,"Not yet implemented setInfoWindowLongClickListener")
    }

    fun setOnIndoorStateChangeListener(listener: IOnIndoorStateChangeListener?) {
        Log.d(TAG, "unimplemented Method: setOnIndoorStateChangeListener")
    }

    override fun setOnMapClickListener(listener: IOnMapClickListener?) {
        mapClickListener = listener
        map?.setOnMapClickListener { latlng ->
            try {
                mapClickListener?.onMapClick(latlng.toGms())
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun setOnMapLongClickListener(listener: IOnMapLongClickListener?) {
        mapLongClickListener = listener
        map?.setOnMapLongClickListener { latlng ->
            try {
                mapLongClickListener?.onMapLongClick(latlng.toGms())
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun setOnMarkerClickListener(listener: IOnMarkerClickListener?) {
        map?.setOnMarkerClickListener { listener?.onMarkerClick(markers[it.id]) ?: false }
    }

    override fun setOnMarkerDragListener(listener: IOnMarkerDragListener?) {
        map?.setOnMarkerDragListener(object : HuaweiMap.OnMarkerDragListener{
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

    override fun setOnInfoWindowClickListener(listener: IOnInfoWindowClickListener?) {
        Log.d(TAG, "setOnInfoWindowClickListener")
        map?.setOnInfoWindowClickListener { listener?.onInfoWindowClick(markers[it.id]) }
    }

    fun setOnInfoWindowCloseListener(listener: IOnInfoWindowCloseListener?) {
        Log.d(TAG, "unimplemented Method: setOnInfoWindowCloseListener")
    }

    fun setOnInfoWindowLongClickListener(listener: IOnInfoWindowLongClickListener?) {
        Log.d(TAG, "unimplemented Method: setOnInfoWindowLongClickListener")
    }

    override fun setInfoWindowAdapter(adapter: IInfoWindowAdapter?) {
        Log.d(TAG, "setInfoWindowAdapter")
        map?.setInfoWindowAdapter(object : HuaweiMap.InfoWindowAdapter{
            override fun getInfoContents(p0: Marker?): View? {
                return adapter?.getInfoContents(markers[p0?.id]).unwrap<View>()
            }

            override fun getInfoWindow(p0: Marker?): View? {
                return adapter?.getInfoWindow(markers[p0?.id]).unwrap<View>()
            }

        })
    }

    override fun setOnMyLocationChangeListener(listener: IOnMyLocationChangeListener?) {
        Log.d(TAG, "deprecated Method: setOnMyLocationChangeListener")
    }

    override fun setOnMyLocationButtonClickListener(listener: IOnMyLocationButtonClickListener?) {
        Log.d(TAG, "setOnMyLocationButtonClickListener")
        map?.setOnMyLocationButtonClickListener { listener?.onMyLocationButtonClick() ?: false }
    }

    override fun setOnMyLocationClickListener(listener: IOnMyLocationClickListener?) {
        Log.d(TAG, "setOnMyLocationClickListener")
        map?.setOnMyLocationClickListener { listener?.onMyLocationClick(it) }
    }

    fun setOnPoiClickListener(listener: IOnPoiClickListener?) {
        Log.d(TAG, "unimplemented Method: setOnPoiClickListener")
    }

    override fun setOnPolygonClickListener(listener: IOnPolygonClickListener?) {
        Log.d(TAG, "setOnPolygonClickListener")
        map?.setOnPolygonClickListener { listener?.onPolygonClick(polygons[it.id]) }
    }

    override fun setOnInfoWindowCloseListener(listener: com.google.android.gms.maps.internal.IOnInfoWindowCloseListener?) {
        Log.d(TAG, "Not yet implemented setInfoWindowCloseListener")
    }

    override fun setOnPolylineClickListener(listener: IOnPolylineClickListener?) {
        Log.d(TAG, "unimplemented Method: setOnPolylineClickListener")
        map?.setOnPolylineClickListener { listener?.onPolylineClick(polylines[it.id]) }
    }

    override fun snapshot(callback: ISnapshotReadyCallback, bitmap: IObjectWrapper?) {
        Log.d(TAG, "snapshot")
        val hmsBitmap = bitmap.unwrap<Bitmap>() ?: return
        val hmsCallback = HuaweiMap.SnapshotReadyCallback { p0 -> callback.onBitmapReady(p0) }
        map?.snapshot(hmsCallback, hmsBitmap)
    }

    override fun snapshotForTest(callback: ISnapshotReadyCallback) {
        Log.d(TAG, "snapshotForTest")
        val hmsCallback = HuaweiMap.SnapshotReadyCallback { p0 -> callback.onBitmapReady(p0) }
        map?.snapshot(hmsCallback)
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        Log.d(TAG, "setPadding: $left $top $right $bottom")
        map?.setPadding(left, top, right, bottom)
    }

    override fun isBuildingsEnabled(): Boolean {
        Log.d(TAG, "isBuildingsEnabled")
        return map?.isBuildingsEnabled ?: true
    }

    override fun setBuildingsEnabled(buildings: Boolean) {
        Log.d(TAG, "setBuildingsEnabled: $buildings")
        map?.isBuildingsEnabled = buildings
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

    override fun setCameraMoveStartedListener(listener: IOnCameraMoveStartedListener?) {
        Log.d(TAG, "setCameraMoveStartedListener")
        cameraMoveStartedListener = listener
        map?.setOnCameraMoveStartedListener {
            try {
                cameraMoveStartedListener?.onCameraMoveStarted(it)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun setCameraMoveListener(listener: IOnCameraMoveListener?) {
        Log.d(TAG, "setCameraMoveListener")
        cameraMoveListener = listener
        map?.setOnCameraMoveListener {
            try {
                cameraMoveListener?.onCameraMove()
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun setCameraMoveCanceledListener(listener: IOnCameraMoveCanceledListener?) {
        Log.d(TAG, "setCameraMoveCanceledListener")
        cameraMoveCanceledListener = listener
        map?.setOnCameraMoveCanceledListener {
            try {
                cameraMoveCanceledListener?.onCameraMoveCanceled()
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun setCameraIdleListener(listener: IOnCameraIdleListener?) {
        Log.d(TAG, "onCameraIdle: successful")
        cameraIdleListener = listener
        map?.setOnCameraIdleListener {
            try {
                cameraIdleListener?.onCameraIdle()
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
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
            Log.d(TAG, "create: ${context.packageName},\n$options")
            val mapContext = MapContext(context)
            MapsInitializer.initialize(mapContext)
            val mapView = MapView(mapContext, options.toHms())
            this.mapView = mapView
            view.addView(mapView)
            mapView.onCreate(savedInstanceState?.toHms())
            view.viewTreeObserver.addOnGlobalLayoutListener {
                if (!isFakeWatermark) {
                    fakeWatermark()
                }
            }
            mapView.getMapAsync(this::initMap)

            created = true
        }
    }

    private var isFakeWatermark: Boolean = false
    private fun fakeWatermark() {
        Log.d(TAG_LOGO, "start")
        try {
            val view1 = view.getChildAt(0) as ViewGroup
            val view2 = view1.getChildAt(0) as ViewGroup
            val view4 = view2.getChildAt(1)
            Log.d(TAG_LOGO, view4.toString())
            if (view4 is LinearLayout) {
                view4.visibility = View.GONE
                isFakeWatermark = true
            } else {
                throw Exception("LinearLayout not found")
            }
        } catch (tr: Throwable) {
            Log.d(TAG_LOGO, "Throwable", tr)
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
        if (this.map != null) return

        loaded = true
        this.map = map

        map.setOnCameraIdleListener {
            try {
                cameraChangeListener?.onCameraChange(map.cameraPosition.toGms())
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        map.setOnCameraIdleListener {
            try {
                cameraIdleListener?.onCameraIdle()
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        map.setOnCameraMoveListener {
            try {
                cameraMoveListener?.onCameraMove()
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        map.setOnCameraMoveStartedListener {
            try {
                cameraMoveStartedListener?.onCameraMoveStarted(it)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        map.setOnCameraMoveCanceledListener {
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
            waitingCameraUpdates.forEach { map.moveCamera(it) }
            val initializedCallbackList = ArrayList(initializedCallbackList)
            Log.d(TAG, "Invoking ${initializedCallbackList.size} callbacks delayed, as map is initialized")
            for (callback in initializedCallbackList) {
                try {
                    callback.onMapReady(this)
                } catch (e: Exception) {
                    Log.w(TAG, e)
                }
            }
        }
    }

    override fun onResume() = mapView?.onResume() ?: Unit
    override fun onPause() = mapView?.onPause() ?: Unit
    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
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
