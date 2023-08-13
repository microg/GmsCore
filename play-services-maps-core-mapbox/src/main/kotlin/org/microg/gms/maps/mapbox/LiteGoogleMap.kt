package org.microg.gms.maps.mapbox

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.graphics.PointF
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.UiThread
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.internal.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.internal.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.WellKnownTileServer
import com.mapbox.mapboxsdk.location.engine.*
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.snapshotter.MapSnapshot
import com.mapbox.mapboxsdk.snapshotter.MapSnapshotter
import com.mapbox.mapboxsdk.style.layers.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.turf.TurfConstants.UNIT_METERS
import com.mapbox.turf.TurfMeasurement
import org.microg.gms.maps.mapbox.model.*
import org.microg.gms.maps.mapbox.utils.toGms
import org.microg.gms.maps.mapbox.utils.toMapbox
import org.microg.gms.maps.mapbox.utils.toPoint
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.roundToInt

// From com.mapbox.mapboxsdk.location.LocationComponent
const val DEFAULT_INTERVAL_MILLIS = 1000L
const val DEFAULT_FASTEST_INTERVAL_MILLIS = 1000L

class MetaSnapshot(
    val snapshot: MapSnapshot,
    val cameraPosition: CameraPosition,
    val cameraBounds: com.mapbox.mapboxsdk.geometry.LatLngBounds?,
    val width: Int,
    val height: Int,
    val paddingRight: Int,
    val paddingTop: Int,
    val dpi: Float
) {
    fun latLngForPixelFixed(point: PointF) = snapshot.latLngForPixel(
        PointF(
            point.x / dpi, point.y / dpi
        )
    )
}

class LiteGoogleMapImpl(context: Context, var options: GoogleMapOptions) : AbstractGoogleMap(context) {

    internal val view: FrameLayout = FrameLayout(mapContext)
    val map: ImageView

    private var created = false

    private var cameraPosition: CameraPosition = options.camera ?: CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 2f)
    private var cameraBounds: com.mapbox.mapboxsdk.geometry.LatLngBounds? = null

    private var mapType: Int = options.mapType
    private var mapStyle: MapStyleOptions? = null

    private var currentSnapshotter: MapSnapshotter? = null

    private var lastSnapshot: MetaSnapshot? = null

    private var lastTouchPosition = PointF(0f, 0f)

    private val afterNextDrawCallback = mutableListOf<() -> Unit>()
    private var cameraChangeListener: IOnCameraChangeListener? = null

    private var myLocationEnabled = false
    private var myLocation: Location? = null
    private val defaultLocationEngine = GoogleLocationEngine(context)
    private var locationEngine: LocationEngine = defaultLocationEngine

    internal val markers: MutableList<LiteMarkerImpl> = mutableListOf()
    internal val polygons: MutableList<LitePolygonImpl> = mutableListOf()
    internal val polylines: MutableList<AbstractPolylineImpl> = mutableListOf()
    internal val circles: MutableList<LiteCircleImpl> = mutableListOf()

    private var nextObjectId = 0

    private var showWatermark = true

    private val updatePosted = AtomicBoolean(false)

    init {
        map = ImageView(mapContext).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }

        view.addView(map)

        view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            postUpdateSnapshot()
            currentInfoWindow?.update()
        }

        BitmapDescriptorFactoryImpl.initialize(mapContext.resources, context.resources)

        // noinspection ClickableViewAccessibility; touch listener only has side effects
        map.setOnTouchListener { _, event ->
            lastTouchPosition = PointF(event.x + map.paddingLeft, event.y + map.paddingTop)
            false
        }

        map.setOnClickListener {

            // Test if clickable
            if ((view.parent as View?)?.isClickable == false) return@setOnClickListener

            lastSnapshot?.let { meta ->
                // Calculate marker hitboxes
                for (marker in markers.filter { it.isVisible }) {

                    marker.getIconDimensions()?.let { iconDimensions -> // consider only markers with icon
                        val anchorPoint = meta.snapshot.pixelForLatLng(marker.position.toMapbox())

                        val leftX = anchorPoint.x - marker.anchor[0] * iconDimensions[0]
                        val topY = anchorPoint.y - marker.anchor[1] * iconDimensions[1]

                        if (lastTouchPosition.x >= leftX && lastTouchPosition.x <= leftX + iconDimensions[0]
                            && lastTouchPosition.y >= topY && lastTouchPosition.y <= topY + iconDimensions[1]) {
                            // Marker was clicked
                            if (markerClickListener?.onMarkerClick(marker) == true) {
                                currentInfoWindow?.close()
                                currentInfoWindow = null
                                return@setOnClickListener
                            } else if (showInfoWindow(marker)) {
                                return@setOnClickListener
                            }
                        }
                    }
                }

                currentInfoWindow?.close()
                currentInfoWindow = null

                // Test if circle was clicked
                for (circle in circles.filter { it.isVisible && it.isClickable }) {
                    Log.d(TAG, "last touch ${lastTouchPosition.x}, ${lastTouchPosition.y}, turf ${TurfMeasurement.distance(
                        circle.center.toPoint(),
                        meta.latLngForPixelFixed(lastTouchPosition).toPoint(),
                        UNIT_METERS
                    )}, radius ${circle.radiusInMeters}")
                    if (TurfMeasurement.distance(
                        circle.center.toPoint(),
                        meta.latLngForPixelFixed(lastTouchPosition).toPoint(),
                        UNIT_METERS
                    ) <= circle.radiusInMeters) {
                        // Circle was clicked
                        circleClickListener?.onCircleClick(circle)
                        return@setOnClickListener
                    }
                }

                val clickedPosition = meta.latLngForPixelFixed(lastTouchPosition)
                val clickListenerConsumedClick = mapClickListener?.let {
                    it.onMapClick(clickedPosition.toGms())
                    true
                } ?: false

                if (clickListenerConsumedClick) return@setOnClickListener

                // else open external map at clicked location
                val intent =
                    Intent(ACTION_VIEW, Uri.parse("geo:${clickedPosition.latitude},${clickedPosition.longitude}"))

                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, "No compatible mapping application installed. Not handling click.")
                }
            }


        }
        map.setOnLongClickListener {
            mapLongClickListener?.onMapLongClick(
                lastSnapshot?.latLngForPixelFixed(lastTouchPosition)?.toGms() ?: LatLng(0.0, 0.0)
            )
            mapLongClickListener != null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!created) {

            Mapbox.getInstance(mapContext, BuildConfig.MAPBOX_KEY, WellKnownTileServer.Mapbox)

            if (savedInstanceState?.containsKey(BUNDLE_CAMERA_POSITION) == true) {
                cameraPosition = savedInstanceState.getParcelable(BUNDLE_CAMERA_POSITION)!!
                cameraBounds = savedInstanceState.getParcelable(BUNDLE_CAMERA_BOUNDS)
            }

            postUpdateSnapshot()

            created = true
        }
    }

    internal fun postUpdateSnapshot() {
        if (updatePosted.compareAndSet(false, true)) {
            Handler(Looper.getMainLooper()).post {
                updatePosted.set(false)
                updateSnapshot()
            }
        }
    }

    @UiThread
    private fun updateSnapshot() {

        val cameraPosition = cameraPosition
        val dpi = dpiFactor

        val cameraBounds = cameraBounds

        val pixelWidth = map.width
        val pixelHeight = map.height

        val styleBuilder = getStyle(mapContext, mapType, mapStyle, styleFromFileWorkaround = true)

        // Add visible polygons (before polylines, so that they are drawn below their strokes)
        for (polygon in polygons.filter { it.isVisible }) {
            styleBuilder.withLayer(
                FillLayer("l${polygon.id}", polygon.id).withProperties(
                    PropertyFactory.fillColor(polygon.fillColor)
                )
            ).withSource(
                GeoJsonSource(polygon.id, polygon.annotationOptions.geometry)
            )
        }

        // Add visible polylines
        for (polyline in polylines.filter { it.isVisible }) {
            styleBuilder.withLayer(
                LineLayer("l${polyline.id}", polyline.id).withProperties(
                    PropertyFactory.lineWidth(polyline.width),
                    PropertyFactory.lineColor(polyline.color),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND)
                )
            ).withSource(
                GeoJsonSource(polyline.id, polyline.annotationOptions.geometry)
            )
        }

        // Add circles
        for (circle in circles.filter { it.isVisible }) {
            styleBuilder.withLayer(FillLayer("l${circle.id}c", circle.id).withProperties(
                PropertyFactory.fillColor(circle.fillColor)
            )).withSource(GeoJsonSource(circle.id, circle.annotationOptions.geometry))

            styleBuilder.withLayer(LineLayer("l${circle.id}s", "${circle.id}s").withProperties(
                PropertyFactory.lineWidth(circle.strokeWidth),
                PropertyFactory.lineColor(circle.strokeColor),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            ).apply {
                circle.strokePattern?.let {
                    val name = it.getName(circle.strokeColor, circle.strokeWidth, dpi)
                    withProperties(PropertyFactory.linePattern(name))
                    styleBuilder.withImage(name, it.makeBitmap(circle.strokeColor, circle.strokeWidth, dpi))
                }
            }).withSource(GeoJsonSource("${circle.id}s", circle.line.annotationOptions.geometry))
        }

        // Add markers
        BitmapDescriptorFactoryImpl.put(styleBuilder)
        for (marker in markers.filter { it.isVisible }) {
            val layer = SymbolLayer("l${marker.id}", marker.id).withProperties(
                PropertyFactory.symbolSortKey(marker.zIndex),
                PropertyFactory.iconAllowOverlap(true)
            )
            marker.icon?.applyTo(layer, marker.anchor, dpi)
            styleBuilder.withLayer(layer).withSource(
                GeoJsonSource(marker.id, marker.annotationOptions.geometry)
            )
        }

        // Add location overlay
        if (myLocationEnabled) myLocation?.let {
            val indicator = ContextCompat.getDrawable(mapContext, R.drawable.location_dot)!!
            styleBuilder.withImage("locationIndicator", indicator)
            val layer = SymbolLayer("location", "locationSource").withProperties(
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconImage("locationIndicator"),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_TOP_LEFT),
                PropertyFactory.iconOffset(arrayOf(
                    0.5f * indicator.minimumWidth / dpi, 0.5f * indicator.minimumHeight / dpi
                ))
            )
            styleBuilder.withLayer(layer).withSource(
                GeoJsonSource(
                    "locationSource",
                    SymbolOptions().withLatLng(com.mapbox.mapboxsdk.geometry.LatLng(it.latitude, it.longitude)).geometry
                )
            )
        }

        val dpiWidth = max(pixelWidth / dpi, 1f).roundToInt()
        val dpiHeight = max(pixelHeight / dpi, 1f).roundToInt()

        val snapshotter = MapSnapshotter(
            mapContext, MapSnapshotter.Options(dpiWidth, dpiHeight)
                .withCameraPosition(this@LiteGoogleMapImpl.cameraPosition.toMapbox())
                .apply {
                    // if camera bounds are set, overwrite camera position
                    cameraBounds?.let { withRegion(it) }
                }
                .withStyleBuilder(styleBuilder)
                .withLogo(showWatermark)
                .withPixelRatio(dpi)
        )

        synchronized(this) {
            this.currentSnapshotter?.cancel()
            this.currentSnapshotter = snapshotter
        }

        snapshotter.start({

            val cameraPositionChanged = cameraPosition != lastSnapshot?.cameraPosition || (cameraBounds != lastSnapshot?.cameraBounds)

            lastSnapshot = MetaSnapshot(
                it, cameraPosition, cameraBounds, pixelWidth, pixelHeight, view.paddingRight, view.paddingTop, dpi
            )
            map.setImageBitmap(it.bitmap)

            for (callback in afterNextDrawCallback) callback()
            afterNextDrawCallback.clear()

            if (cameraPositionChanged) {
                // Notify apps that new projection is now available
                cameraChangeListener?.onCameraChange(cameraPosition)
            }

            currentInfoWindow?.update()

            synchronized(this) {
                this.currentSnapshotter = null
            }

        }, null)
    }

    fun getMapAsync(callback: IOnMapReadyCallback) {
        if (lastSnapshot == null) {
            Log.d(TAG, "Invoking callback instantly, as a snapshot is ready")
            callback.onMapReady(this)
        } else {
            Log.d(TAG, "Delay callback invocation, as snapshot has not been rendered yet")
            afterNextDrawCallback.add { callback.onMapReady(this) }
        }
    }

    override fun getCameraPosition(): CameraPosition = cameraPosition

    override fun getMaxZoomLevel() = 21f

    override fun getMinZoomLevel() = 1f

    override fun moveCamera(cameraUpdate: IObjectWrapper?): Unit = cameraUpdate.unwrap<LiteModeCameraUpdate>()?.let {
        cameraPosition = it.getLiteModeCameraPosition(this) ?: cameraPosition
        cameraBounds = it.getLiteModeCameraBounds()

        postUpdateSnapshot()
    } ?: Unit

    override fun animateCamera(cameraUpdate: IObjectWrapper?) = moveCamera(cameraUpdate)

    override fun animateCameraWithCallback(cameraUpdate: IObjectWrapper?, callback: ICancelableCallback?) {
        moveCamera(cameraUpdate)
        Log.d(TAG, "animateCameraWithCallback: animation not possible in lite mode, invoking callback instantly")
        callback?.onFinish()
    }

    override fun animateCameraWithDurationAndCallback(
        cameraUpdate: IObjectWrapper?, duration: Int, callback: ICancelableCallback?
    ) = animateCameraWithCallback(cameraUpdate, callback)

    override fun stopAnimation() {
        Log.d(TAG, "stopAnimation: animation not possible in lite mode")
    }

    override fun addPolyline(options: PolylineOptions): IPolylineDelegate {
        return LitePolylineImpl(this, "polyline${nextObjectId++}", options).also { polylines.add(it) }
    }

    override fun addPolygon(options: PolygonOptions): IPolygonDelegate {
        return LitePolygonImpl(
            "polygon${nextObjectId++}", options, this
        ).also {
            polygons.add(it)
            polylines.addAll(it.strokes)
            postUpdateSnapshot()
        }
    }

    override fun addMarker(options: MarkerOptions): IMarkerDelegate {
        return LiteMarkerImpl("marker${nextObjectId++}", options, this).also {
            markers.add(it)
            postUpdateSnapshot()
        }
    }

    override fun addGroundOverlay(options: GroundOverlayOptions?): IGroundOverlayDelegate? {
        Log.d(TAG, "addGroundOverlay: not supported in lite mode")
        return null
    }

    override fun addTileOverlay(options: TileOverlayOptions?): ITileOverlayDelegate? {
        Log.d(TAG, "addTileOverlay: not supported in lite mode")
        return null
    }

    override fun clear() {
        polylines.clear()
        polygons.clear()
        markers.clear()
        circles.clear()
        postUpdateSnapshot()
    }

    override fun getMapType(): Int {
        return mapType
    }

    override fun setMapType(type: Int) {
        mapType = type
        postUpdateSnapshot()
    }

    override fun isTrafficEnabled(): Boolean {
        Log.d(TAG, "isTrafficEnabled: traffic not supported in lite mode")
        return false
    }

    override fun setTrafficEnabled(traffic: Boolean) {
        Log.d(TAG, "setTrafficEnabled: traffic not supported in lite mode")
    }

    override fun isIndoorEnabled(): Boolean {
        Log.d(TAG, "isIndoorEnabled: indoor not supported in lite mode")
        return false
    }

    override fun setIndoorEnabled(indoor: Boolean) {
        Log.d(TAG, "setIndoorEnabled: indoor not supported in lite mode")
    }

    override fun isMyLocationEnabled(): Boolean = myLocationEnabled

    override fun setMyLocationEnabled(myLocation: Boolean) {
        if (!myLocationEnabled && myLocation) {
            activateLocationProvider()
        } else if (myLocationEnabled && !myLocation) {
            deactivateLocationProvider()
        } // else situation is unchanged
        myLocationEnabled = myLocation
    }

    private fun activateLocationProvider() {
        // Activate only if sufficient permissions
        if (ActivityCompat.checkSelfPermission(
                mapContext, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                mapContext, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationEngine.requestLocationUpdates(
                LocationEngineRequest.Builder(DEFAULT_INTERVAL_MILLIS)
                    .setFastestInterval(DEFAULT_FASTEST_INTERVAL_MILLIS)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .build(), locationEngineCallback, Looper.getMainLooper()
            )

        } else {
            Log.w(TAG, "Called setMyLocationEnabled(true) without sufficient permissions. Not showing location.")
        }
    }

    private fun deactivateLocationProvider() {
        locationEngine.removeLocationUpdates(locationEngineCallback)
    }

    override fun setLocationSource(locationSource: ILocationSourceDelegate?) {
        if (myLocationEnabled) deactivateLocationProvider()
        locationEngine = locationSource?.let { SourceLocationEngine(it) } ?: defaultLocationEngine
        if (myLocationEnabled) activateLocationProvider()
    }

    override fun onLocationUpdate(location: Location) {
        this@LiteGoogleMapImpl.myLocation = location
        postUpdateSnapshot()
    }

    override fun getUiSettings(): IUiSettingsDelegate {
        Log.d(TAG, "UI settings have no effect")
        return UiSettingsCache()
    }

    /**
     * Gets a projection snapshot. This means that, in accordance to the docs, the projection object
     * will represent the map as it is seen at the point in time that the projection is queried, and
     * not updated later on.
     */
    override fun getProjection(): IProjectionDelegate = lastSnapshot?.let { LiteProjection(it) } ?: DummyProjection()

    override fun setOnCameraChangeListener(listener: IOnCameraChangeListener?) {
        cameraChangeListener = listener
    }

    override fun setOnMarkerDragListener(listener: IOnMarkerDragListener?) {
        Log.d(TAG, "setOnMarkerDragListener: marker drag is not supported in lite mode")
    }

    override fun addCircle(options: CircleOptions): ICircleDelegate {
        return LiteCircleImpl(this, "circle${nextObjectId++}", options).also { circles.add(it) }
    }

    override fun snapshot(callback: ISnapshotReadyCallback?, bitmap: IObjectWrapper?) {
        val lastSnapshot = lastSnapshot
        if (lastSnapshot == null) {
            afterNextDrawCallback.add {
                callback?.onBitmapWrappedReady(ObjectWrapper.wrap(this@LiteGoogleMapImpl.lastSnapshot!!.snapshot.bitmap))
            }
        } else {
            callback?.onBitmapWrappedReady(ObjectWrapper.wrap(lastSnapshot.snapshot.bitmap))
        }
    }

    override fun snapshotForTest(callback: ISnapshotReadyCallback?) {
        Log.d(TAG, "Not yet implemented: snapshotForTest")
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        view.setPadding(left, top, right, bottom)
        postUpdateSnapshot()
    }

    override fun isBuildingsEnabled(): Boolean {
        Log.d(TAG, "isBuildingsEnabled: never enabled in light mode")
        return false
    }

    override fun setBuildingsEnabled(buildings: Boolean) {
        Log.d(TAG, "setBuildingsEnabled: cannot be enabled in light mode")
    }

    override fun setOnMapLoadedCallback(callback: IOnMapLoadedCallback?) = callback?.let { onMapLoadedCallback ->
        if (lastSnapshot != null) {
            Log.d(TAG, "Invoking map loaded callback instantly, as a snapshot is ready")
            onMapLoadedCallback.onMapLoaded()
        }
        else {
            Log.d(TAG, "Delaying map loaded callback, as snapshot has not been taken yet")
            afterNextDrawCallback.add { onMapLoadedCallback.onMapLoaded() }
        }
        Unit
    } ?: Unit

    override fun setWatermarkEnabled(watermark: Boolean) {
        showWatermark = watermark
    }

    override fun showInfoWindow(marker: AbstractMarker): Boolean {
        infoWindowAdapter.getInfoWindowViewFor(marker, mapContext)?.let { infoView ->
            currentInfoWindow?.close()
            currentInfoWindow = InfoWindow(infoView, this, marker).also { infoWindow ->
                infoWindow.open(view)
            }
            return true
        }
        return false
    }

    override fun onResume() {
        if (myLocationEnabled) activateLocationProvider()
    }

    override fun onPause() {
        synchronized(this) {
            currentSnapshotter?.cancel()
            currentSnapshotter = null
        }
        deactivateLocationProvider()
    }

    override fun onDestroy() {
        view.removeView(map)
    }

    override fun onLowMemory() {
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(BUNDLE_CAMERA_POSITION, cameraPosition)
        outState.putParcelable(BUNDLE_CAMERA_BOUNDS, cameraBounds)
    }

    override fun setContentDescription(desc: String?) {
        view.contentDescription = desc
    }

    override fun setMapStyle(options: MapStyleOptions?): Boolean {
        Log.d(TAG, "setMapStyle options: " + options?.getJson())
        mapStyle = options

        return true
    }

    override fun setMinZoomPreference(minZoom: Float) {
        Log.d(TAG, "setMinZoomPreference: no interactivity in lite mode")
    }

    override fun setMaxZoomPreference(maxZoom: Float) {
        Log.d(TAG, "setMaxZoomPreference: no interactivity in lite mode")
    }

    override fun resetMinMaxZoomPreference() {
        Log.d(TAG, "resetMinMaxZoomPreference: no interactivity in lite mode")
    }

    override fun setLatLngBoundsForCameraTarget(bounds: LatLngBounds?) {
        Log.d(TAG, "setLatLngBoundsForCameraTarget: no interactivity in lite mode")
    }

    override fun setCameraMoveStartedListener(listener: IOnCameraMoveStartedListener?) {
        Log.d(TAG, "setCameraMoveStartedListener: event not supported in lite mode")
    }

    override fun setCameraMoveListener(listener: IOnCameraMoveListener?) {
        Log.d(TAG, "setCameraMoveListener: event not supported in lite mode")

    }

    override fun setCameraMoveCanceledListener(listener: IOnCameraMoveCanceledListener?) {
        Log.d(TAG, "setCameraMoveCanceledListener: event not supported in lite mode")
    }

    override fun setCameraIdleListener(listener: IOnCameraIdleListener?) {
        Log.d(TAG, "setCameraIdleListener: event not supported in lite mode")
    }

    override fun onStart() {
    }

    override fun onStop() {
    }

    companion object {
        private val TAG = "GmsMapLite"
        private val BUNDLE_CAMERA_POSITION = "camera"
        private val BUNDLE_CAMERA_BOUNDS = "cameraBounds"
    }
}

