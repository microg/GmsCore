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

package org.microg.gms.maps.vtm;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.internal.ICancelableCallback;
import com.google.android.gms.maps.internal.IGoogleMapDelegate;
import com.google.android.gms.maps.internal.IInfoWindowAdapter;
import com.google.android.gms.maps.internal.ILocationSourceDelegate;
import com.google.android.gms.maps.internal.IOnCameraChangeListener;
import com.google.android.gms.maps.internal.IOnCameraIdleListener;
import com.google.android.gms.maps.internal.IOnCameraMoveCanceledListener;
import com.google.android.gms.maps.internal.IOnCameraMoveListener;
import com.google.android.gms.maps.internal.IOnCameraMoveStartedListener;
import com.google.android.gms.maps.internal.IOnCircleClickListener;
import com.google.android.gms.maps.internal.IOnInfoWindowClickListener;
import com.google.android.gms.maps.internal.IOnInfoWindowCloseListener;
import com.google.android.gms.maps.internal.IOnInfoWindowLongClickListener;
import com.google.android.gms.maps.internal.IOnMapClickListener;
import com.google.android.gms.maps.internal.IOnMapLoadedCallback;
import com.google.android.gms.maps.internal.IOnMapLongClickListener;
import com.google.android.gms.maps.internal.IOnMarkerClickListener;
import com.google.android.gms.maps.internal.IOnMarkerDragListener;
import com.google.android.gms.maps.internal.IOnMyLocationButtonClickListener;
import com.google.android.gms.maps.internal.IOnMyLocationChangeListener;
import com.google.android.gms.maps.internal.IProjectionDelegate;
import com.google.android.gms.maps.internal.ISnapshotReadyCallback;
import com.google.android.gms.maps.internal.IUiSettingsDelegate;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.internal.ICircleDelegate;
import com.google.android.gms.maps.model.internal.IGroundOverlayDelegate;
import com.google.android.gms.maps.model.internal.IMarkerDelegate;
import com.google.android.gms.maps.model.internal.IPolygonDelegate;
import com.google.android.gms.maps.model.internal.IPolylineDelegate;
import com.google.android.gms.maps.model.internal.ITileOverlayDelegate;

import org.microg.gms.maps.vtm.camera.CameraUpdate;
import org.microg.gms.maps.vtm.camera.MapPositionCameraUpdate;
import org.microg.gms.maps.vtm.markup.CircleImpl;
import org.microg.gms.maps.vtm.markup.GroundOverlayImpl;
import org.microg.gms.maps.vtm.markup.MarkerImpl;
import org.microg.gms.maps.vtm.markup.Markup;
import org.microg.gms.maps.vtm.markup.PolygonImpl;
import org.microg.gms.maps.vtm.markup.PolylineImpl;
import org.microg.gms.maps.vtm.markup.TileOverlayImpl;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class GoogleMapImpl extends IGoogleMapDelegate.Stub
        implements UiSettingsImpl.UiSettingsListener, Markup.MarkupListener, BackendMap.CameraUpdateListener {
    private static final String TAG = "GmsMapImpl";

    private final GoogleMapOptions options;
    private final Context context;
    private final BackendMap backendMap;
    private final UiSettingsImpl uiSettings;
    private final ProjectionImpl projection;

    private int markerCounter = 0;
    private int circleCounter = 0;
    private int polylineCounter = 0;
    private int polygonCounter = 0;

    private IOnMarkerClickListener onMarkerClickListener;
    private IOnMarkerDragListener onMarkerDragListener;
    private IOnCameraChangeListener onCameraChangeListener;
    private IOnMyLocationChangeListener onMyLocationChangeListener;

    private Criteria criteria;
    private Location myLocation;
    private LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            // TODO: Actually do my location overlay
            myLocation = location;
            if (onMyLocationChangeListener != null && location != null) {
                try {
                    onMyLocationChangeListener.onMyLocationChanged(ObjectWrapper.wrap(location));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private GoogleMapImpl(Context context, GoogleMapOptions options) {
        this.context = context;
        Context appContext = context;
        if (appContext.getApplicationContext() != null)
            appContext = appContext.getApplicationContext();
        Context wrappedContext = ApplicationContextWrapper.gmsContextWithAttachedApplicationContext(appContext);
        backendMap = new BackendMap(wrappedContext, this);
        uiSettings = new UiSettingsImpl(this);
        projection = new ProjectionImpl(backendMap.getViewport());
        this.options = options;

        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setPowerRequirement(Criteria.POWER_MEDIUM);

        if (options != null) initFromOptions();
    }

    public synchronized static GoogleMapImpl create(Context context, GoogleMapOptions options) {
        return new GoogleMapImpl(context, options);
    }

    private void initFromOptions() {
        try {
            uiSettings.setCompassEnabled(options.getCompassEnabled());
            uiSettings.setRotateGesturesEnabled(options.isRotateGesturesEnabled());
            uiSettings.setTiltGesturesEnabled(options.isTiltGesturesEnabled());
            uiSettings.setScrollGesturesEnabled(options.isScrollGesturesEnabled());
            uiSettings.setZoomControlsEnabled(options.isZoomControlsEnabled());
            uiSettings.setZoomGesturesEnabled(options.isZoomGesturesEnabled());
            if (options.getCamera() != null) {
                backendMap.applyCameraUpdate(MapPositionCameraUpdate.directMapPosition(GmsMapsTypeHelper.fromCameraPosition(options.getCamera())));
            }
        } catch (RemoteException e) {
            // Never happens: not remote
        }
    }

    public void onDestroy() {
        backendMap.destroy();
    }

    @Override
    public void onLowMemory() throws RemoteException {
        Log.d(TAG, "unimplemented Method: onLowMemory");

    }

    @Override
    public boolean useViewLifecycleWhenInFragment() throws RemoteException {
        Log.d(TAG, "unimplemented Method: useViewLifecycleWhenInFragment");
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onSaveInstanceState");

    }

    @Override
    public void setContentDescription(String desc) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setContentDescription");

    }

    @Override
    public void onEnterAmbient(Bundle bundle) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onEnterAmbient");

    }

    @Override
    public void onExitAmbient() throws RemoteException {
        Log.d(TAG, "unimplemented Method: onExitAmbient");

    }

    @Override
    public void setCircleClickListener(IOnCircleClickListener listener) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setCircleClickListener");
    }

    @Override
    public boolean setMapStyle(MapStyleOptions options) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setMapStyle");
        return true;
    }

    @Override
    public void setMinZoomPreference(float minZoom) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setMinZoomPreference");

    }

    @Override
    public void setMaxZoomPreference(float maxZoom) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setMaxZoomPreference");

    }

    @Override
    public void resetMinMaxZoomPreference() throws RemoteException {
        Log.d(TAG, "unimplemented Method: resetMinMaxZoomPreference");

    }

    @Override
    public void setLatLngBoundsForCameraTarget(LatLngBounds bounds) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setLatLngBoundsForCameraTarget");

    }

    public void onResume() {
        backendMap.onResume();
    }

    public void onPause() {
        backendMap.onPause();
    }

    public View getView() {
        return backendMap.getView();
    }

    private String getNextMarkerId() {
        return "m" + markerCounter++;
    }

    private String getNextCircleId() {
        return "c" + circleCounter++;
    }

    private String getNextPolylineId() {
        return "l" + polylineCounter++;
    }

    private String getNextPolygonId() {
        return "p" + polygonCounter++;
    }
    
    /*
    Camera
     */

    @Override
    public CameraPosition getCameraPosition() throws RemoteException {
        return GmsMapsTypeHelper.toCameraPosition(backendMap.getMapPosition());
    }

    @Override
    public float getMaxZoomLevel() throws RemoteException {
        return (float) backendMap.getViewport().limitScale(Double.MIN_VALUE);
    }

    @Override
    public float getMinZoomLevel() throws RemoteException {
        return (float) backendMap.getViewport().limitScale(Double.MAX_VALUE);
    }

    @Override
    public void moveCamera(IObjectWrapper cameraUpdate) throws RemoteException {
        CameraUpdate camUpdate = (CameraUpdate) ObjectWrapper.unwrap(cameraUpdate);
        backendMap.applyCameraUpdate(camUpdate);
    }

    @Override
    public void animateCamera(IObjectWrapper cameraUpdate) throws RemoteException {
        CameraUpdate camUpdate = (CameraUpdate) ObjectWrapper.unwrap(cameraUpdate);
        backendMap.applyCameraUpdateAnimated(camUpdate, 1000);
    }

    @Override
    public void animateCameraWithCallback(IObjectWrapper cameraUpdate, ICancelableCallback callback)
            throws RemoteException {
        CameraUpdate camUpdate = (CameraUpdate) ObjectWrapper.unwrap(cameraUpdate);
        backendMap.applyCameraUpdateAnimated(camUpdate, 1000);
    }

    @Override
    public void animateCameraWithDurationAndCallback(IObjectWrapper cameraUpdate, int duration,
                                                     ICancelableCallback callback) throws RemoteException {
        CameraUpdate camUpdate = (CameraUpdate) ObjectWrapper.unwrap(cameraUpdate);
        backendMap.applyCameraUpdateAnimated(camUpdate, duration);
    }

    @Override
    public IProjectionDelegate getProjection() throws RemoteException {
        return projection;
    }

    @Override
    public void stopAnimation() throws RemoteException {
        backendMap.stopAnimation();
    }

    @Override
    public void onCameraUpdate(CameraPosition cameraPosition) {
        if (onCameraChangeListener != null) {
            try {
                onCameraChangeListener.onCameraChange(cameraPosition);
            } catch (RemoteException e) {
                Log.w(TAG, e);
            }
        }
    }
    
    /*
    Markers, polylines, polygons, overlays, etc
     */

    @Override
    public ICircleDelegate addCircle(CircleOptions options) throws RemoteException {
        return backendMap.add(new CircleImpl(getNextCircleId(), options, this));
    }

    @Override
    public IPolylineDelegate addPolyline(PolylineOptions options) throws RemoteException {
        return backendMap.add(new PolylineImpl(getNextPolylineId(), options, this));
    }

    @Override
    public IPolygonDelegate addPolygon(PolygonOptions options) throws RemoteException {
        return backendMap.add(new PolygonImpl(getNextPolygonId(), options, this));
    }

    @Override
    public IMarkerDelegate addMarker(MarkerOptions options) throws RemoteException {
        return backendMap.add(new MarkerImpl(getNextMarkerId(), options, this));
    }

    @Override
    public IGroundOverlayDelegate addGroundOverlay(GroundOverlayOptions options)
            throws RemoteException {
        Log.d(TAG, "not yet usable: addGroundOverlay");
        return new GroundOverlayImpl(options); // TODO
    }

    @Override
    public ITileOverlayDelegate addTileOverlay(TileOverlayOptions options) throws RemoteException {
        Log.d(TAG, "not yet usable: addTileOverlay");
        return new TileOverlayImpl(); // TODO
    }

    @Override
    public void setInfoWindowAdapter(IInfoWindowAdapter adapter) throws RemoteException {
        Log.d(TAG, "not yet usable: setInfoWindowAdapter");
    }

    @Override
    public void clear() throws RemoteException {
        backendMap.clear();
        markerCounter = 0;
        circleCounter = 0;
        polylineCounter = 0;
        polygonCounter = 0;
    }

    @Override
    public void update(Markup markup) {
        backendMap.update(markup);
    }

    @Override
    public void remove(Markup markup) {
        backendMap.remove(markup);
    }

    @Override
    public boolean onClick(Markup markup) {
        if (markup instanceof IMarkerDelegate) {
            if (onMarkerClickListener != null) {
                try {
                    if (onMarkerClickListener.onMarkerClick((IMarkerDelegate) markup))
                        return true;
                } catch (RemoteException e) {
                    Log.w(TAG, e);
                }
            }
            // TODO: open InfoWindow
        }
        return false;
    }

    @Override
    public void onDragStart(Markup markup) {
        backendMap.setScrollGesturesEnabled(false);
        backendMap.setRotateGesturesEnabled(false);
        backendMap.setTiltGesturesEnabled(false);
        backendMap.setZoomGesturesEnabled(false);
        if (markup instanceof IMarkerDelegate) {
            if (onMarkerDragListener != null) {
                try {
                    onMarkerDragListener.onMarkerDragStart((IMarkerDelegate) markup);
                } catch (RemoteException e) {
                    Log.w(TAG, e);
                }
            }
        }
    }

    @Override
    public void onDragStop(Markup markup) {
        try {
            backendMap.setScrollGesturesEnabled(uiSettings.isScrollGesturesEnabled());
            backendMap.setRotateGesturesEnabled(uiSettings.isRotateGesturesEnabled());
            backendMap.setTiltGesturesEnabled(uiSettings.isTiltGesturesEnabled());
            backendMap.setZoomGesturesEnabled(uiSettings.isZoomGesturesEnabled());
        } catch (RemoteException e) {
            // Never happens, is local.
        }
        if (markup instanceof IMarkerDelegate) {
            if (onMarkerDragListener != null) {
                try {
                    onMarkerDragListener.onMarkerDragEnd((IMarkerDelegate) markup);
                } catch (RemoteException e) {
                    Log.w(TAG, e);
                }
            }
        }
    }

    @Override
    public void onDragProgress(Markup markup) {
        if (markup instanceof IMarkerDelegate) {
            if (onMarkerDragListener != null) {
                try {
                    onMarkerDragListener.onMarkerDrag((IMarkerDelegate) markup);
                } catch (RemoteException e) {
                    Log.w(TAG, e);
                }
            }
        }
    }
    
    /*
    Map options
     */

    @Override
    public int getMapType() throws RemoteException {
        return 0;
    }

    @Override
    public void setMapType(int type) throws RemoteException {

    }

    @Override
    public boolean isTrafficEnabled() throws RemoteException {
        return false;
    }

    @Override
    public void setTrafficEnabled(boolean traffic) throws RemoteException {
        Log.w(TAG, "Traffic not yet supported");
    }

    @Override
    public boolean isIndoorEnabled() throws RemoteException {
        return false;
    }

    @Override
    public void setIndoorEnabled(boolean indoor) throws RemoteException {
        Log.w(TAG, "Indoor not yet supported");
    }

    @Override
    public boolean isMyLocationEnabled() throws RemoteException {
        return false;
    }

    @Override
    public void setMyLocationEnabled(boolean myLocation) throws RemoteException {
        Log.w(TAG, "MyLocation not yet supported");
        boolean hasPermission = ContextCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED;
        if (!hasPermission) {
            throw new SecurityException("Neither " + ACCESS_COARSE_LOCATION + " nor " + ACCESS_FINE_LOCATION + " granted.");
        }
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (myLocation) {
            locationManager.requestLocationUpdates(5000, 10, criteria, listener, Looper.getMainLooper());
        } else {
            locationManager.removeUpdates(listener);
        }
    }

    @Override
    public boolean isBuildingsEnabled() throws RemoteException {
        return backendMap.hasBuilding();
    }

    @Override
    public void setBuildingsEnabled(boolean buildingsEnabled) throws RemoteException {
        backendMap.setBuildings(buildingsEnabled);
    }
    
    /*
    Ui Settings
     */

    @Override
    public IUiSettingsDelegate getUiSettings() throws RemoteException {
        Log.d(TAG, "getUiSettings: " + uiSettings);
        return uiSettings;
    }

    @Override
    public void onUiSettingsChanged(UiSettingsImpl settings) throws RemoteException {
        if (settings.isCompassEnabled()) {
            Log.w(TAG, "Compass not yet supported");
        }
        if (settings.isMyLocationButtonEnabled()) {
            Log.w(TAG, "MyLocationButton not yet supported");
        }
        if (settings.isZoomControlsEnabled()) {
            Log.w(TAG, "ZoomControls not yet supported");
        }
        backendMap.setScrollGesturesEnabled(settings.isScrollGesturesEnabled());
        backendMap.setRotateGesturesEnabled(settings.isRotateGesturesEnabled());
        backendMap.setTiltGesturesEnabled(settings.isTiltGesturesEnabled());
        backendMap.setZoomGesturesEnabled(settings.isZoomGesturesEnabled());
    }
    
    /*
    Listener and callback setters
     */

    @Override
    public void setOnCameraChangeListener(IOnCameraChangeListener listener) throws RemoteException {
        Log.d(TAG, "setOnCameraChangeListener");
        this.onCameraChangeListener = listener;
    }

    @Override
    public void setOnMapClickListener(IOnMapClickListener listener) throws RemoteException {
        Log.d(TAG, "setOnMapClickListener: not supported");
    }

    @Override
    public void setOnMapLongClickListener(IOnMapLongClickListener listener) throws RemoteException {
        Log.d(TAG, "setOnMapLongClickListener: not supported");
    }

    @Override
    public void setOnMarkerClickListener(IOnMarkerClickListener listener) throws RemoteException {
        Log.d(TAG, "setOnMarkerClickListener");
        this.onMarkerClickListener = listener;
    }

    @Override
    public void setOnMarkerDragListener(IOnMarkerDragListener listener) throws RemoteException {
        Log.d(TAG, "setOnMarkerDragListener");
        this.onMarkerDragListener = listener;
    }

    @Override
    public void setOnInfoWindowClickListener(IOnInfoWindowClickListener listener)
            throws RemoteException {
        Log.d(TAG, "setOnInfoWindowClickListener: not supported");
    }

    @Override
    public void setOnMyLocationChangeListener(IOnMyLocationChangeListener listener)
            throws RemoteException {
        Log.d(TAG, "setOnMyLocationChangeListener");
        this.onMyLocationChangeListener = listener;
    }

    @Override
    public void setOnMyLocationButtonClickListener(IOnMyLocationButtonClickListener listener)
            throws RemoteException {
        Log.d(TAG, "setOnMyLocationButtonClickListener: not supported");
    }

    @Override
    public void setOnMapLoadedCallback(final IOnMapLoadedCallback callback) throws RemoteException {
        Log.d(TAG, "setOnMapLoadedCallback");
        new Handler(context.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Announce map loaded");
                if (callback != null) {
                    try {
                        callback.onMapLoaded();
                    } catch (RemoteException e) {
                        Log.w(TAG, e);
                    }
                }
            }
        }, 5000);
    }

    @Override
    public void setWatermarkEnabled(boolean watermark) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setWatermarkEnabled");

    }

    @Override
    public void onCreate(Bundle savedInstanceState) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onCreate");

    }

    @Override
    public void setCameraMoveStartedListener(IOnCameraMoveStartedListener listener) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setCameraMoveStartedListener");

    }

    @Override
    public void setCameraMoveListener(IOnCameraMoveListener listener) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setCameraMoveListener");

    }

    @Override
    public void setCameraMoveCanceledListener(IOnCameraMoveCanceledListener listener) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setCameraMoveCanceledListener");

    }

    @Override
    public void setCameraIdleListener(IOnCameraIdleListener listener) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setCameraIdleListener");

    }

    @Override
    public void setInfoWindowLongClickListener(IOnInfoWindowLongClickListener listener) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setInfoWindowLongClickListener");
    }

    @Override
    public void setInfoWindowCloseListener(IOnInfoWindowCloseListener listener) throws RemoteException {
        Log.d(TAG, "unimplemented Method: setInfoWindowCloseListener");
    }

    @Override
    public void onStart() throws RemoteException {
        Log.d(TAG, "unimplemented Method: onStart");

    }

    @Override
    public void onStop() throws RemoteException {
        Log.d(TAG, "unimplemented Method: onStop");

    }
    
    /*
    Misc
     */

    @Override
    public IObjectWrapper getTestingHelper() throws RemoteException {
        return null;
    }

    @Override
    public void snapshot(ISnapshotReadyCallback callback, IObjectWrapper bitmap)
            throws RemoteException {
        Bitmap b = (Bitmap) ObjectWrapper.unwrap(bitmap);
        Log.d(TAG, "snapshot!: " + b);
        backendMap.snapshot(b, callback);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) throws RemoteException {
        getView().setPadding(left, top, right, bottom);
    }

    @Override
    public Location getMyLocation() throws RemoteException {
        return myLocation;
    }

    @Override
    public void setLocationSource(ILocationSourceDelegate locationSource) throws RemoteException {
        Log.d(TAG, "setLocationSource: " + locationSource);
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
