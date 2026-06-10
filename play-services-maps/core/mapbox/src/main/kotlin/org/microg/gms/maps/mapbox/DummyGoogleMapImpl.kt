package org.microg.gms.maps.mapbox

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.maps.internal.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.internal.*
import org.microg.gms.maps.mapbox.model.AbstractMarker

class DummyMarkerImpl(private val id: String, options: MarkerOptions) : IMarkerDelegate.Stub() {
    private var position = options.position
    override fun remove() {}
    override fun getId(): String = id
    override fun setPosition(pos: LatLng?) { position = pos ?: position }
    override fun getPosition(): LatLng = position
    override fun setTitle(title: String?) {}
    override fun getTitle(): String? = null
    override fun setSnippet(snippet: String?) {}
    override fun getSnippet(): String? = null
    override fun setDraggable(drag: Boolean) {}
    override fun isDraggable(): Boolean = false
    override fun showInfoWindow() {}
    override fun hideInfoWindow() {}
    override fun isInfoWindowShown(): Boolean = false
    override fun setVisible(visible: Boolean) {}
    override fun isVisible(): Boolean = true
    override fun equalsRemote(other: IMarkerDelegate?): Boolean = other?.id == id
    override fun hashCodeRemote(): Int = id.hashCode()
    override fun setIcon(obj: IObjectWrapper?) {}
    override fun setAnchor(x: Float, y: Float) {}
    override fun setFlat(flat: Boolean) {}
    override fun isFlat(): Boolean = false
    override fun setRotation(rotation: Float) {}
    override fun getRotation(): Float = 0f
    override fun setInfoWindowAnchor(x: Float, y: Float) {}
    override fun setAlpha(alpha: Float) {}
    override fun getAlpha(): Float = 1f
    override fun setZIndex(zIndex: Float) {}
    override fun getZIndex(): Float = 0f
    override fun setTag(obj: IObjectWrapper?) {}
    override fun getTag(): IObjectWrapper = ObjectWrapper.wrap(null)
}

class DummyGoogleMapImpl(context: Context) : AbstractGoogleMap(context) {
    val view: View = FrameLayout(mapContext)
    private var markerId = 0

    override fun getCameraPosition(): CameraPosition = CameraPosition(LatLng(0.0, 0.0), 0f, 0f, 0f)
    override fun getMaxZoomLevel(): Float = 20f
    override fun getMinZoomLevel(): Float = 0f
    override fun moveCamera(cameraUpdate: IObjectWrapper?) {}
    override fun animateCamera(cameraUpdate: IObjectWrapper?) {}
    override fun animateCameraWithCallback(cameraUpdate: IObjectWrapper?, callback: ICancelableCallback?) { callback?.onFinish() }
    override fun animateCameraWithDurationAndCallback(cameraUpdate: IObjectWrapper?, duration: Int, callback: ICancelableCallback?) { callback?.onFinish() }
    override fun stopAnimation() {}
    override fun setMapStyle(options: MapStyleOptions?): Boolean = true
    override fun setMinZoomPreference(minZoom: Float) {}
    override fun setMaxZoomPreference(maxZoom: Float) {}
    override fun resetMinMaxZoomPreference() {}
    override fun setLatLngBoundsForCameraTarget(bounds: LatLngBounds?) {}
    override fun addPolyline(options: PolylineOptions): IPolylineDelegate? = null
    override fun addPolygon(options: PolygonOptions): IPolygonDelegate? = null
    override fun addMarker(options: MarkerOptions): IMarkerDelegate = DummyMarkerImpl("m${markerId++}", options)
    override fun addGroundOverlay(options: GroundOverlayOptions): IGroundOverlayDelegate? = null
    override fun addTileOverlay(options: TileOverlayOptions): ITileOverlayDelegate? = null
    override fun addCircle(options: CircleOptions): ICircleDelegate? = null
    override fun clear() {}
    override fun getMapType(): Int = 0
    override fun setMapType(type: Int) {}
    override fun setWatermarkEnabled(watermark: Boolean) {}
    override fun isMyLocationEnabled(): Boolean = false
    override fun setMyLocationEnabled(myLocation: Boolean) {}
    override fun setLocationSource(locationSource: ILocationSourceDelegate?) {}
    override fun getMyLocation(): Location? = null
    override fun onLocationUpdate(location: Location) {}
    override fun setContentDescription(desc: String?) {}
    override fun getUiSettings(): IUiSettingsDelegate = UiSettingsCache()
    override fun getProjection(): IProjectionDelegate = DummyProjection()
    override fun setOnCameraChangeListener(listener: IOnCameraChangeListener?) {}
    override fun setOnMarkerDragListener(listener: IOnMarkerDragListener?) {}
    override fun snapshot(callback: ISnapshotReadyCallback, bitmap: IObjectWrapper?) { callback.onBitmapWrappedReady(ObjectWrapper.wrap(null)) }
    override fun snapshotForTest(callback: ISnapshotReadyCallback?) {}
    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {}
    override fun setOnMapLoadedCallback(callback: IOnMapLoadedCallback?) { callback?.onMapLoaded() }
    override fun setCameraMoveStartedListener(listener: IOnCameraMoveStartedListener?) {}
    override fun setCameraMoveListener(listener: IOnCameraMoveListener?) {}
    override fun setCameraMoveCanceledListener(listener: IOnCameraMoveCanceledListener?) {}
    override fun setCameraIdleListener(listener: IOnCameraIdleListener?) {}
    override fun onCreate(savedInstanceState: Bundle?) {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onDestroy() {}
    override fun onStart() {}
    override fun onStop() {}
    override fun onLowMemory() {}
    override fun onSaveInstanceState(outState: Bundle) {}
    override fun showInfoWindow(marker: AbstractMarker): Boolean = false
    fun getMapAsync(callback: IOnMapReadyCallback) { callback.onMapReady(this) }
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        if (super.onTransact(code, data, reply, flags)) true else { Log.d("GmsMapDummy", "onTransact [unknown]: $code, $data, $flags"); false }
}
