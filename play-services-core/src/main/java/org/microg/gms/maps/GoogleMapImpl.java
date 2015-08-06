/*
 * Copyright 2013-2015 µg Project Team
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

package org.microg.gms.maps;

import android.content.Context;
import android.location.Location;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.internal.ICancelableCallback;
import com.google.android.gms.maps.internal.IGoogleMapDelegate;
import com.google.android.gms.maps.internal.IInfoWindowAdapter;
import com.google.android.gms.maps.internal.ILocationSourceDelegate;
import com.google.android.gms.maps.internal.IOnCameraChangeListener;
import com.google.android.gms.maps.internal.IOnInfoWindowClickListener;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.internal.ICircleDelegate;
import com.google.android.gms.maps.model.internal.IGroundOverlayDelegate;
import com.google.android.gms.maps.model.internal.IMarkerDelegate;
import com.google.android.gms.maps.model.internal.IPolygonDelegate;
import com.google.android.gms.maps.model.internal.IPolylineDelegate;
import com.google.android.gms.maps.model.internal.ITileOverlayDelegate;

import org.microg.gms.maps.camera.CameraUpdate;
import org.microg.gms.maps.markup.CircleImpl;
import org.microg.gms.maps.markup.GroundOverlayImpl;
import org.microg.gms.maps.markup.MarkerImpl;
import org.microg.gms.maps.markup.Markup;
import org.microg.gms.maps.markup.PolygonImpl;
import org.microg.gms.maps.markup.PolylineImpl;
import org.microg.gms.maps.markup.TileOverlayImpl;
import org.oscim.event.Event;
import org.oscim.event.MotionEvent;
import org.oscim.map.Map;

public class GoogleMapImpl extends IGoogleMapDelegate.Stub
        implements UiSettingsImpl.UiSettingsListener, Map.InputListener, Markup.MarkupListener {
    private static final String TAG = "GoogleMapImpl";

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

    public GoogleMapImpl(LayoutInflater inflater, GoogleMapOptions options) {
        context = inflater.getContext();
        backendMap = new BackendMap(context);
        backendMap.setInputListener(this);
        uiSettings = new UiSettingsImpl(this);
        projection = new ProjectionImpl(backendMap.getViewport());
        this.options = options;
    }

    public void onDestroy() {
        backendMap.destroy();
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
        return 0;
    }

    @Override
    public float getMinZoomLevel() throws RemoteException {
        return 0;
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
    
    /*
    Markers, polylines, polygons, overlays, etc
     */

    @Override
    public ICircleDelegate addCircle(CircleOptions options) throws RemoteException {
        return backendMap.add(new CircleImpl(getNextCircleId(), options, this));
    }

    @Override
    public IPolylineDelegate addPolyline(PolylineOptions options) throws RemoteException {
        Log.d(TAG, "addPolyline");
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
    }
    
    /*
    Listener and callback setters
     */

    @Override
    public void setOnCameraChangeListener(IOnCameraChangeListener listener) throws RemoteException {

    }

    @Override
    public void setOnMapClickListener(IOnMapClickListener listener) throws RemoteException {

    }

    @Override
    public void setOnMapLongClickListener(IOnMapLongClickListener listener) throws RemoteException {

    }

    @Override
    public void setOnMarkerClickListener(IOnMarkerClickListener listener) throws RemoteException {
        this.onMarkerClickListener = listener;
    }

    @Override
    public void setOnMarkerDragListener(IOnMarkerDragListener listener) throws RemoteException {

    }

    @Override
    public void setOnInfoWindowClickListener(IOnInfoWindowClickListener listener)
            throws RemoteException {

    }

    @Override
    public void setOnMyLocationChangeListener(IOnMyLocationChangeListener listener)
            throws RemoteException {

    }

    @Override
    public void setOnMyLocationButtonClickListener(IOnMyLocationButtonClickListener listener)
            throws RemoteException {

    }

    @Override
    public void setOnMapLoadedCallback(IOnMapLoadedCallback callback) throws RemoteException {
        Log.d(TAG, "not yet usable: setOnMapLoadedCallback");
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

    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) throws RemoteException {
        getView().setPadding(left, top, right, bottom);
    }

    @Override
    public Location getMyLocation() throws RemoteException {
        return null;
    }

    @Override
    public void setLocationSource(ILocationSourceDelegate locationSource) throws RemoteException {

    }

    @Override
    public void onInputEvent(Event event, MotionEvent motionEvent) {
        Log.d(TAG, "onInputEvent(" + event + ", " + motionEvent + ")");
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
