package com.google.android.gms.maps.internal;

import android.location.Location;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.internal.ICancelableCallback;
import com.google.android.gms.maps.internal.ILocationSourceDelegate;
import com.google.android.gms.maps.internal.IUiSettingsDelegate;
import com.google.android.gms.maps.internal.IProjectionDelegate;
import com.google.android.gms.maps.internal.IOnCameraChangeListener;
import com.google.android.gms.maps.internal.IOnMapClickListener;
import com.google.android.gms.maps.internal.IOnMapLongClickListener;
import com.google.android.gms.maps.internal.IOnMarkerClickListener;
import com.google.android.gms.maps.internal.IOnMarkerDragListener;
import com.google.android.gms.maps.internal.IOnInfoWindowClickListener;
import com.google.android.gms.maps.internal.IInfoWindowAdapter;
import com.google.android.gms.maps.internal.IOnMapLoadedCallback;
import com.google.android.gms.maps.internal.IOnMyLocationChangeListener;
import com.google.android.gms.maps.internal.IOnMyLocationButtonClickListener;
import com.google.android.gms.maps.internal.ISnapshotReadyCallback;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.internal.IPolylineDelegate;
import com.google.android.gms.maps.model.internal.IPolygonDelegate;
import com.google.android.gms.maps.model.internal.IMarkerDelegate;
import com.google.android.gms.maps.model.internal.ICircleDelegate;
import com.google.android.gms.maps.model.internal.IGroundOverlayDelegate;
import com.google.android.gms.maps.model.internal.ITileOverlayDelegate;

interface IGoogleMapDelegate {
    CameraPosition getCameraPosition();

    float getMaxZoomLevel();
    float getMinZoomLevel();

    void moveCamera(IObjectWrapper cameraUpdate);
    void animateCamera(IObjectWrapper cameraUpdate);
    void animateCameraWithCallback(IObjectWrapper cameraUpdate, ICancelableCallback callback);
    void animateCameraWithDurationAndCallback(IObjectWrapper cameraUpdate, int duration, ICancelableCallback callback);
    void stopAnimation();

    IPolylineDelegate addPolyline(in PolylineOptions options);
    IPolygonDelegate addPolygon(in PolygonOptions options);
    IMarkerDelegate addMarker(in MarkerOptions options);
    IGroundOverlayDelegate addGroundOverlay(in GroundOverlayOptions options);
    ITileOverlayDelegate addTileOverlay(in TileOverlayOptions options);

    void clear();

    int getMapType();
    void setMapType(int type);
    boolean isTrafficEnabled();
    void setTrafficEnabled(boolean traffic);
    boolean isIndoorEnabled();
    void setIndoorEnabled(boolean indoor);

    boolean isMyLocationEnabled();
    void setMyLocationEnabled(boolean myLocation);
    Location getMyLocation();
    void setLocationSource(ILocationSourceDelegate locationSource);

    IUiSettingsDelegate getUiSettings();
    IProjectionDelegate getProjection();

    void setOnCameraChangeListener(IOnCameraChangeListener listener);
    void setOnMapClickListener(IOnMapClickListener listener);
    void setOnMapLongClickListener(IOnMapLongClickListener listener);
    void setOnMarkerClickListener(IOnMarkerClickListener listener);
    void setOnMarkerDragListener(IOnMarkerDragListener listener);
    void setOnInfoWindowClickListener(IOnInfoWindowClickListener listener);
    void setInfoWindowAdapter(IInfoWindowAdapter adapter);

    IObjectWrapper getTestingHelper();

    ICircleDelegate addCircle(in CircleOptions options);

    void setOnMyLocationChangeListener(IOnMyLocationChangeListener listener);
    void setOnMyLocationButtonClickListener(IOnMyLocationButtonClickListener listener);

    void snapshot(ISnapshotReadyCallback callback, IObjectWrapper bitmap);

    void setPadding(int left, int top, int right, int bottom);

    boolean isBuildingsEnabled();
    void setBuildingsEnabled(boolean buildings);

    void setOnMapLoadedCallback(IOnMapLoadedCallback callback);
}
