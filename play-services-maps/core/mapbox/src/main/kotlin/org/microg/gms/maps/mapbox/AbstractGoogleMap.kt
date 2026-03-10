package org.microg.gms.maps.mapbox

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.maps.internal.*
import com.mapbox.mapboxsdk.location.engine.LocationEngineCallback
import com.mapbox.mapboxsdk.location.engine.LocationEngineResult
import org.microg.gms.maps.mapbox.model.AbstractMarker
import org.microg.gms.maps.mapbox.model.DefaultInfoWindowAdapter
import org.microg.gms.maps.mapbox.model.InfoWindow
import org.microg.gms.maps.mapbox.utils.MapContext

abstract class AbstractGoogleMap(context: Context) : IGoogleMapDelegate.Stub() {

    internal val mapContext = MapContext(context)

    val dpiFactor: Float
        get() = mapContext.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT

    internal var currentInfoWindow: InfoWindow? = null
    internal var infoWindowAdapter: IInfoWindowAdapter = DefaultInfoWindowAdapter(mapContext)
    internal var onInfoWindowClickListener: IOnInfoWindowClickListener? = null
    internal var onInfoWindowLongClickListener: IOnInfoWindowLongClickListener? = null
    internal var onInfoWindowCloseListener: IOnInfoWindowCloseListener? = null

    internal var mapClickListener: IOnMapClickListener? = null
    internal var mapLongClickListener: IOnMapLongClickListener? = null
    internal var markerClickListener: IOnMarkerClickListener? = null
    internal var circleClickListener: IOnCircleClickListener? = null

    internal var myLocationChangeListener: IOnMyLocationChangeListener? = null

    internal val locationEngineCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult?) {
            result?.lastLocation?.let { location ->
                Log.d(TAG, "myLocationChanged: $location")
                myLocationChangeListener?.onMyLocationChanged(ObjectWrapper.wrap(location))

                onLocationUpdate(location)
            }
        }
        override fun onFailure(e: Exception) {
            Log.e(TAG, "Failed to obtain location update", e)
        }
    }


    internal abstract fun showInfoWindow(marker: AbstractMarker): Boolean

    internal abstract fun onLocationUpdate(location: Location)

    override fun setOnInfoWindowClickListener(listener: IOnInfoWindowClickListener?) {
        onInfoWindowClickListener = listener
    }

    override fun setOnInfoWindowLongClickListener(listener: IOnInfoWindowLongClickListener?) {
        onInfoWindowLongClickListener = listener
    }

    override fun setOnInfoWindowCloseListener(listener: IOnInfoWindowCloseListener?) {
        onInfoWindowCloseListener = listener
    }

    override fun setInfoWindowAdapter(adapter: IInfoWindowAdapter?) {
        infoWindowAdapter = adapter ?: DefaultInfoWindowAdapter(mapContext)
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

    override fun setOnCircleClickListener(listener: IOnCircleClickListener?) {
        circleClickListener = listener
    }

    override fun setOnPolygonClickListener(listener: IOnPolygonClickListener?) {
        Log.d(TAG, "Not yet implemented: setOnPolygonClickListener")
    }

    override fun setOnPolylineClickListener(listener: IOnPolylineClickListener?) {
        Log.d(TAG, "Not yet implemented: setOnPolylineClickListener")
    }

    override fun setOnGroundOverlayClickListener(listener: IOnGroundOverlayClickListener?) {
        Log.d(TAG, "Not yet implemented: setOnGroundOverlayClickListener")
    }

    override fun setOnMyLocationClickListener(listener: IOnMyLocationClickListener?) {
        Log.d(TAG, "Not yet implemented: setOnMyLocationClickListener")
    }

    override fun getMyLocation(): Location? {
        Log.d(TAG, "unimplemented Method: getMyLocation")
        return null
    }

    override fun setLocationSource(locationSource: ILocationSourceDelegate?) {
        Log.d(TAG, "unimplemented Method: setLocationSource")
    }

    override fun setOnMyLocationChangeListener(listener: IOnMyLocationChangeListener?) {
        myLocationChangeListener = listener
    }

    override fun setOnMyLocationButtonClickListener(listener: IOnMyLocationButtonClickListener?) {
        Log.d(TAG, "unimplemented Method: setOnMyLocationButtonClickListener")
    }

    override fun getTestingHelper(): IObjectWrapper {
        Log.d(TAG, "unimplemented Method: getTestingHelper")
        return ObjectWrapper.wrap(null)
    }

    override fun isBuildingsEnabled(): Boolean {
        Log.d(TAG, "unimplemented Method: isBuildingsEnabled")
        return false
    }

    override fun setBuildingsEnabled(buildings: Boolean) {
        Log.d(TAG, "unimplemented Method: setBuildingsEnabled")
    }

    override fun useViewLifecycleWhenInFragment(): Boolean {
        Log.d(TAG, "unimplemented Method: useViewLifecycleWhenInFragment")
        return false
    }

    override fun onEnterAmbient(bundle: Bundle?) {
        Log.d(TAG, "unimplemented Method: onEnterAmbient")
    }

    override fun onExitAmbient() {
        Log.d(TAG, "unimplemented Method: onExitAmbient")
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

    companion object {
        val TAG = "GmsMapAbstract"
    }
}