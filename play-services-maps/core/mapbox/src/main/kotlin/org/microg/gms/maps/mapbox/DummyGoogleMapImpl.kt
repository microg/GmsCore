/*
 * Copyright (C) 2024 microG Project Team
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
import android.util.Log
import android.view.View
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.internal.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.internal.*
import org.microg.gms.maps.mapbox.model.AbstractMarker

class DummyGoogleMapImpl(context: Context, var options: GoogleMapOptions) : AbstractGoogleMap(context) {
    val view: View = View(context)

    override fun getCameraPosition(): CameraPosition = options.camera ?: CameraPosition(LatLng(0.0, 0.0), 0f, 0f, 0f)
    override fun getMaxZoomLevel(): Float = 20f
    override fun getMinZoomLevel(): Float = 0f
    override fun moveCamera(cameraUpdate: IObjectWrapper?) {}
    override fun animateCamera(cameraUpdate: IObjectWrapper?) {}
    override fun animateCameraWithCallback(cameraUpdate: IObjectWrapper?, callback: ICancelableCallback?) { callback?.onFinish() }
    override fun animateCameraWithDurationAndCallback(cameraUpdate: IObjectWrapper?, duration: Int, callback: ICancelableCallback?) { callback?.onFinish() }
    override fun stopAnimation() {}
    override fun addPolyline(options: PolylineOptions): IPolylineDelegate = DummyPolylineImpl(options)
    override fun addPolygon(options: PolygonOptions): IPolygonDelegate = DummyPolygonImpl(options)
    override fun addMarker(options: MarkerOptions): IMarkerDelegate = DummyMarkerImpl(options)
    override fun addGroundOverlay(options: GroundOverlayOptions): IGroundOverlayDelegate = DummyGroundOverlayImpl(options)
    override fun addTileOverlay(options: TileOverlayOptions): ITileOverlayDelegate = DummyTileOverlayImpl(options)
    override fun clear() {}
    override fun getMapType(): Int = options.mapType
    override fun setMapType(type: Int) {}
    override fun isTrafficEnabled(): Boolean = false
    override fun setTrafficEnabled(traffic: Boolean) {}
    override fun isIndoorEnabled(): Boolean = false
    override fun setIndoorEnabled(indoor: Boolean) {}
    override fun isMyLocationEnabled(): Boolean = false
    override fun setMyLocationEnabled(myLocation: Boolean) {}
    override fun getMyLocation(): Location? = null
    override fun setLocationSource(locationSource: ILocationSourceDelegate?) {}
    override fun getUiSettings(): IUiSettingsDelegate = UiSettingsCache()
    override fun getProjection(): IProjectionDelegate = DummyProjection()
    override fun setOnCameraChangeListener(listener: IOnCameraChangeListener?) {}
    override fun setOnMapClickListener(listener: IOnMapClickListener?) {}
    override fun setOnMapLongClickListener(listener: IOnMapLongClickListener?) {}
    override fun setOnMarkerClickListener(listener: IOnMarkerClickListener?) {}
    override fun setOnMarkerDragListener(listener: IOnMarkerDragListener?) {}
    override fun setOnInfoWindowClickListener(listener: IOnInfoWindowClickListener?) {}
    override fun setInfoWindowAdapter(adapter: IInfoWindowAdapter?) {}
    override fun getTestingHelper(): IObjectWrapper = ObjectWrapper.wrap(null)
    override fun addCircle(options: CircleOptions): ICircleDelegate = DummyCircleImpl(options)
    override fun setOnMyLocationChangeListener(listener: IOnMyLocationChangeListener?) {}
    override fun setOnMyLocationButtonClickListener(listener: IOnMyLocationButtonClickListener?) {}
    override fun snapshot(callback: ISnapshotReadyCallback, bitmap: IObjectWrapper?) { callback.onBitmapWrappedReady(ObjectWrapper.wrap(null)) }
    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {}
    override fun isBuildingsEnabled(): Boolean = false
    override fun setBuildingsEnabled(buildings: Boolean) {}
    override fun setOnMapLoadedCallback(callback: IOnMapLoadedCallback?) {}
    override fun setWatermarkEnabled(watermark: Boolean) {}
    override fun onCreate(savedInstanceState: Bundle?) {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onDestroy() {}
    override fun onLowMemory() {}
    override fun useViewLifecycleWhenInFragment(): Boolean = false
    override fun onSaveInstanceState(outState: Bundle) {}
    override fun setContentDescription(desc: String?) {}
    override fun snapshotForTest(callback: ISnapshotReadyCallback?) { callback?.onBitmapWrappedReady(ObjectWrapper.wrap(null)) }
    override fun onEnterAmbient(bundle: Bundle?) {}
    override fun onExitAmbient() {}
    override fun setOnGroundOverlayClickListener(listener: IOnGroundOverlayClickListener?) {}
    override fun setOnInfoWindowLongClickListener(listener: IOnInfoWindowLongClickListener?) {}
    override fun setOnPolygonClickListener(listener: IOnPolygonClickListener?) {}
    override fun setOnInfoWindowCloseListener(listener: IOnInfoWindowCloseListener?) {}
    override fun setOnPolylineClickListener(listener: IOnPolylineClickListener?) {}
    override fun setOnCircleClickListener(listener: IOnCircleClickListener?) {}
    override fun setMapStyle(options: MapStyleOptions?): Boolean = true
    override fun setMinZoomPreference(minZoom: Float) {}
    override fun setMaxZoomPreference(maxZoom: Float) {}
    override fun resetMinMaxZoomPreference() {}
    override fun setLatLngBoundsForCameraTarget(bounds: LatLngBounds?) {}
    override fun setCameraMoveStartedListener(listener: IOnCameraMoveStartedListener?) {}
    override fun setCameraMoveListener(listener: IOnCameraMoveListener?) {}
    override fun setCameraMoveCanceledListener(listener: IOnCameraMoveCanceledListener?) {}
    override fun setCameraIdleListener(listener: IOnCameraIdleListener?) {}
    override fun onStart() {}
    override fun onStop() {}
    override fun setOnMyLocationClickListener(listener: IOnMyLocationClickListener?) {}
    override fun showInfoWindow(marker: AbstractMarker): Boolean = false
    override fun onLocationUpdate(location: Location) {}

    fun getMapAsync(callback: IOnMapReadyCallback) {
        callback.onMapReady(this)
    }
}

class DummyMarkerImpl(options: MarkerOptions) : IMarkerDelegate.Stub() {
    private var position = options.position
    private var title = options.title
    private var snippet = options.snippet
    private var draggable = options.isDraggable
    private var visible = options.isVisible
    private var alpha = options.alpha
    private var zIndex = options.zIndex
    private var tag: IObjectWrapper? = null
    private var rotation = options.rotation

    override fun remove() {}
    override fun getId(): String = "dummy"
    override fun setPosition(pos: LatLng) { position = pos }
    override fun getPosition(): LatLng = position
    override fun setTitle(t: String?) { title = t }
    override fun getTitle(): String? = title
    override fun setSnippet(s: String?) { snippet = s }
    override fun getSnippet(): String? = snippet
    override fun setDraggable(drag: Boolean) { draggable = drag }
    override fun isDraggable(): Boolean = draggable
    override fun showInfoWindow() {}
    override fun hideInfoWindow() {}
    override fun isInfoWindowShown(): Boolean = false
    override fun setVisible(v: Boolean) { visible = v }
    override fun isVisible(): Boolean = visible
    override fun equalsRemote(other: IMarkerDelegate?): Boolean = other?.id == id
    override fun hashCodeRemote(): Int = id.hashCode()
    override fun setIcon(obj: IObjectWrapper?) {}
    override fun setAnchor(x: Float, y: Float) {}
    override fun setFlat(flat: Boolean) {}
    override fun isFlat(): Boolean = false
    override fun setRotation(r: Float) { rotation = r }
    override fun getRotation(): Float = rotation
    override fun setInfoWindowAnchor(x: Float, y: Float) {}
    override fun setAlpha(a: Float) { alpha = a }
    override fun getAlpha(): Float = alpha
    override fun setZIndex(z: Float) { zIndex = z }
    override fun getZIndex(): Float = zIndex
    override fun setTag(obj: IObjectWrapper?) { tag = obj }
    override fun getTag(): IObjectWrapper? = tag
}

class DummyPolylineImpl(options: PolylineOptions) : IPolylineDelegate.Stub() {
    private var points = options.points
    private var width = options.width
    private var color = options.color
    private var zIndex = options.zIndex
    private var visible = options.isVisible
    private var geodesic = options.isGeodesic
    private var clickable = options.isClickable
    private var jointType = options.jointType
    private var pattern = options.pattern
    private var tag: IObjectWrapper? = null

    override fun remove() {}
    override fun getId(): String = "dummy"
    override fun setPoints(p: List<LatLng>) { points = p }
    override fun getPoints(): List<LatLng> = points
    override fun setWidth(w: Float) { width = w }
    override fun getWidth(): Float = width
    override fun setColor(c: Int) { color = c }
    override fun getColor(): Int = color
    override fun setZIndex(z: Float) { zIndex = z }
    override fun getZIndex(): Float = zIndex
    override fun setVisible(v: Boolean) { visible = v }
    override fun isVisible(): Boolean = visible
    override fun setGeodesic(geod: Boolean) { geodesic = geod }
    override fun isGeodesic(): Boolean = geodesic
    override fun setStartCap(startCap: Cap?) {}
    override fun getStartCap(): Cap? = null
    override fun setEndCap(endCap: Cap?) {}
    override fun getEndCap(): Cap? = null
    override fun equalsRemote(other: IPolylineDelegate?): Boolean = other?.id == id
    override fun hashCodeRemote(): Int = id.hashCode()
    override fun setClickable(c: Boolean) { clickable = c }
    override fun isClickable(): Boolean = clickable
    override fun setJointType(j: Int) { jointType = j }
    override fun getJointType(): Int = jointType
    override fun setPattern(p: List<PatternItem>?) { pattern = p }
    override fun getPattern(): List<PatternItem>? = pattern
    override fun setTag(t: IObjectWrapper?) { tag = t }
    override fun getTag(): IObjectWrapper? = tag
}

class DummyPolygonImpl(options: PolygonOptions) : IPolygonDelegate.Stub() {
    private var points = options.points
    private var holes = options.holes
    private var strokeWidth = options.strokeWidth
    private var strokeColor = options.strokeColor
    private var fillColor = options.fillColor
    private var zIndex = options.zIndex
    private var visible = options.isVisible
    private var geodesic = options.isGeodesic
    private var clickable = options.isClickable
    private var strokeJointType = options.strokeJointType
    private var strokePattern = options.strokePattern
    private var tag: IObjectWrapper? = null

    override fun remove() {}
    override fun getId(): String = "dummy"
    override fun setPoints(p: List<LatLng>) { points = p }
    override fun getPoints(): List<LatLng> = points
    override fun setHoles(h: List<Any?>) { holes = h as List<List<LatLng>> }
    override fun getHoles(): List<Any?> = holes
    override fun setStrokeWidth(w: Float) { strokeWidth = w }
    override fun getStrokeWidth(): Float = strokeWidth
    override fun setStrokeColor(c: Int) { strokeColor = c }
    override fun getStrokeColor(): Int = strokeColor
    override fun setFillColor(c: Int) { fillColor = c }
    override fun getFillColor(): Int = fillColor
    override fun setZIndex(z: Float) { zIndex = z }
    override fun getZIndex(): Float = zIndex
    override fun setVisible(v: Boolean) { visible = v }
    override fun isVisible(): Boolean = visible
    override fun setGeodesic(geod: Boolean) { geodesic = geod }
    override fun isGeodesic(): Boolean = geodesic
    override fun equalsRemote(other: IPolygonDelegate?): Boolean = other?.id == id
    override fun hashCodeRemote(): Int = id.hashCode()
    override fun setClickable(c: Boolean) { clickable = c }
    override fun isClickable(): Boolean = clickable
    override fun setStrokeJointType(j: Int) { strokeJointType = j }
    override fun getStrokeJointType(): Int = strokeJointType
    override fun setStrokePattern(p: List<PatternItem>?) { strokePattern = p }
    override fun getStrokePattern(): List<PatternItem>? = strokePattern
    override fun setTag(t: IObjectWrapper?) { tag = t }
    override fun getTag(): IObjectWrapper? = tag
}

class DummyCircleImpl(options: CircleOptions) : ICircleDelegate.Stub() {
    private var center = options.center
    private var radius = options.radius
    private var strokeWidth = options.strokeWidth
    private var strokeColor = options.strokeColor
    private var fillColor = options.fillColor
    private var zIndex = options.zIndex
    private var visible = options.isVisible
    private var clickable = options.isClickable
    private var strokePattern = options.strokePattern
    private var tag: IObjectWrapper? = null

    override fun remove() {}
    override fun getId(): String = "dummy"
    override fun setCenter(c: LatLng) { center = c }
    override fun getCenter(): LatLng = center
    override fun setRadius(r: Double) { radius = r }
    override fun getRadius(): Double = radius
    override fun setStrokeWidth(w: Float) { strokeWidth = w }
    override fun getStrokeWidth(): Float = strokeWidth
    override fun setStrokeColor(c: Int) { strokeColor = c }
    override fun getStrokeColor(): Int = strokeColor
    override fun setFillColor(c: Int) { fillColor = c }
    override fun getFillColor(): Int = fillColor
    override fun setZIndex(z: Float) { zIndex = z }
    override fun getZIndex(): Float = zIndex
    override fun setVisible(v: Boolean) { visible = v }
    override fun isVisible(): Boolean = visible
    override fun equalsRemote(other: ICircleDelegate?): Boolean = other?.id == id
    override fun hashCodeRemote(): Int = id.hashCode()
    override fun setClickable(c: Boolean) { clickable = c }
    override fun isClickable(): Boolean = clickable
    override fun setStrokePattern(p: List<PatternItem>?) { strokePattern = p }
    override fun getStrokePattern(): List<PatternItem>? = strokePattern
    override fun setTag(t: IObjectWrapper?) { tag = t }
    override fun getTag(): IObjectWrapper? = tag
}

class DummyGroundOverlayImpl(options: GroundOverlayOptions) : IGroundOverlayDelegate.Stub() {
    private var position = options.location
    private var width = options.width
    private var height = options.height
    private var bounds = options.bounds
    private var bearing = options.bearing
    private var zIndex = options.zIndex
    private var visible = options.isVisible
    private var transparency = options.transparency
    private var clickable = options.isClickable
    private var tag: IObjectWrapper? = null

    override fun remove() {}
    override fun getId(): String = "dummy"
    override fun setPosition(pos: LatLng) { position = pos }
    override fun getPosition(): LatLng = position
    override fun setDimension(w: Float) { width = w }
    override fun setDimensions(w: Float, h: Float) { width = w; height = h }
    override fun getWidth(): Float = width
    override fun getHeight(): Float = height
    override fun setPositionFromBounds(b: LatLngBounds) { bounds = b }
    override fun getBounds(): LatLngBounds = bounds
    override fun setBearing(b: Float) { bearing = b }
    override fun getBearing(): Float = bearing
    override fun setZIndex(z: Float) { zIndex = z }
    override fun getZIndex(): Float = zIndex
    override fun setVisible(v: Boolean) { visible = v }
    override fun isVisible(): Boolean = visible
    override fun setTransparency(t: Float) { transparency = t }
    override fun getTransparency(): Float = transparency
    override fun equalsRemote(other: IGroundOverlayDelegate?): Boolean = other?.id == id
    override fun hashCodeRemote(): Int = id.hashCode()
    override fun setImage(image: IObjectWrapper?) {}
    override fun setClickable(c: Boolean) { clickable = c }
    override fun isClickable(): Boolean = clickable
    override fun setTag(t: IObjectWrapper?) { tag = t }
    override fun getTag(): IObjectWrapper? = tag
}

class DummyTileOverlayImpl(options: TileOverlayOptions) : ITileOverlayDelegate.Stub() {
    private var zIndex = options.zIndex
    private var visible = options.isVisible
    private var fadeIn = options.fadeIn
    private var transparency = options.transparency

    override fun remove() {}
    override fun clearTileCache() {}
    override fun getId(): String = "dummy"
    override fun setZIndex(z: Float) { zIndex = z }
    override fun getZIndex(): Float = zIndex
    override fun setVisible(v: Boolean) { visible = v }
    override fun isVisible(): Boolean = visible
    override fun equalsRemote(other: ITileOverlayDelegate?): Boolean = other?.id == id
    override fun hashCodeRemote(): Int = id.hashCode()
    override fun setFadeIn(f: Boolean) { fadeIn = f }
    override fun getFadeIn(): Boolean = fadeIn
    override fun setTransparency(t: Float) { transparency = t }
    override fun getTransparency(): Float = transparency
}
