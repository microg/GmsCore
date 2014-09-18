/*
 * Copyright (c) 2014 μg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.maps;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.internal.*;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.maps.model.internal.*;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import org.microg.gms.maps.camera.CameraUpdate;
import org.microg.gms.maps.camera.CameraUpdateFactoryImpl;
import org.microg.gms.maps.markup.*;

public class GoogleMapImpl {
	private static final String TAG = GoogleMapImpl.class.getName();

	public static final int MAP_TYPE_NONE = 0;
	public static final int MAP_TYPE_NORMAL = 1;
	public static final int MAP_TYPE_SATELLITE = 2;
	public static final int MAP_TYPE_TERRAIN = 3;
	public static final int MAP_TYPE_HYBRID = 4;

	private final ViewGroup view;
	private final GoogleMapOptions options;

	private final Delegate delegate = new Delegate();
    private final UiSettings uiSettings = new UiSettings();
    private final Projection projection = new Projection();

    private MapView mapView;
    private Context context;
    private InfoWindow infoWindow;
    private int markerCounter = 0;

    private IOnCameraChangeListener cameraChangeListener;
    private IOnMapClickListener mapClickListener;
    private IOnMapLoadedCallback mapLoadedCallback;
    private IOnMapLongClickListener mapLongClickListener;
    private IOnMarkerClickListener markerClickListener;
    private IOnMarkerDragListener markerDragListener;
    private IOnInfoWindowClickListener infoWindowClickListener;

    private int mapType = 1;
    private IInfoWindowAdapter infoWindowAdapter;

    public GoogleMapImpl(LayoutInflater inflater, GoogleMapOptions options) {
        context = inflater.getContext();
        this.view = new RelativeLayout(context);
        try {
            mapView = (MapView) Class.forName("com.google.android.maps.MapView").getConstructor(Context.class, String.class).newInstance(context, null);
            prepareMapView();
            view.addView(mapView);
        } catch (Exception e) {
            Log.d(TAG, "Sorry, can't create legacy MapView");
        }
        this.options = options;
    }

    private void prepareMapView() {
        mapView.getOverlays().add(new com.google.android.maps.Overlay() {
          @Override
          public boolean onTap(com.google.android.maps.GeoPoint p, MapView mapView)
          {
                Log.d(TAG, "onClick");
                IOnMapClickListener listener = mapClickListener;
                if (listener != null) {
                    try {
                        listener.onMapClick(new LatLng(p));
                    } catch (RemoteException e) {
                        Log.w(TAG, e);
                    }
                    return true;
                }
              return false;
          }
        });
        // TODO: this is actually never called
        mapView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.d(TAG, "onLongClick");
                IOnMapLongClickListener listener = mapLongClickListener;
                if (listener != null) {
                    try {
                        // TODO: Handle LatLng right
                        listener.onMapLongClick(new LatLng(0, 0));
                    } catch (RemoteException e) {
                        Log.w(TAG, e);
                    }
                    return true;
                }
                return false;
            }
        });
    }

	public void onCreate(Bundle savedInstanceState) {
        if (options != null) {
            try {
                delegate.animateCamera(new CameraUpdateFactoryImpl().newCameraPosition(options.getCamera()));
                delegate.setMapType(options.getMapType());
                uiSettings.setCompassEnabled(options.isCompassEnabled());
                uiSettings.setZoomControlsEnabled(options.isZoomControlsEnabled());
                uiSettings.setRotateGesturesEnabled(options.isRotateGesturesEnabled());
                uiSettings.setScrollGesturesEnabled(options.isScrollGesturesEnabled());
                uiSettings.setTiltGesturesEnabled(options.isTiltGesturesEnabled());
                uiSettings.setZoomGesturesEnabled(options.isZoomGesturesEnabled());
            } catch (RemoteException ignored) {
                // It's not remote...
            }
        }
        if (savedInstanceState != null) {
            savedInstanceState.setClassLoader(GoogleMapImpl.class.getClassLoader());
            mapView.onRestoreInstanceState(savedInstanceState);
        }
    }

    public IOnMarkerClickListener getMarkerClickListener() {
        return markerClickListener;
    }

    public IOnInfoWindowClickListener getInfoWindowClickListener() {
        return infoWindowClickListener;
    }

    public Context getContext() {
        return context;
    }

    public MapView getMapView() {
		return mapView;
	}

	public MapController getController() {
		return mapView != null ? mapView.getController() : null;
	}

	public View getView() {
		return view;
	}

	public IGoogleMapDelegate getDelegate() {
		return delegate;
	}

	public void remove(MarkerImpl marker) {
        mapView.getOverlays().remove(marker.getOverlay());
        try {
            if (infoWindow != null && infoWindow.getMarker().getId().equals(marker.getId())) {
                hideInfoWindow();
            }
        } catch (RemoteException e) {
            // It's not remote...
        }
    }

    public void redraw() {
        mapView.postInvalidate();
        try {
            ((MapView.WrappedMapView) mapView.getWrapped()).postInvalidate();
        } catch (Exception e) {
            Log.w(TAG, "MapView does not support extended microg features", e);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        mapView.onSaveInstanceState(outState);
    }

    public void hideInfoWindow() {
        if (infoWindow != null) {
            mapView.getOverlays().remove(infoWindow);
            infoWindow.destroy();
        }
        infoWindow = null;
    }

    public void showInfoWindow(final MarkerImpl marker) {
        hideInfoWindow();
        InfoWindow window = new InfoWindow(context, this, marker);
        if (infoWindowAdapter != null) {
            try {
                IObjectWrapper infoWindow = infoWindowAdapter.getInfoWindow(marker);
                window.setWindow((View) ObjectWrapper.unwrap(infoWindow));
            } catch (RemoteException e) {
                Log.w(TAG, e);
            }
        }
        if (!window.isComplete()) {
            if (infoWindowAdapter != null) {
                try {
                    IObjectWrapper contents = infoWindowAdapter.getInfoContents(marker);
                    window.setContent((View) ObjectWrapper.unwrap(contents));
                } catch (RemoteException e) {
                    Log.w(TAG, e);
                }
            }
        }
        if (!window.isComplete()) {
            window.buildDefault();
        }
        if (window.isComplete()) {
            infoWindow = window;
            Log.d(TAG, "Showing info window " + infoWindow + " for marker " + marker);
            mapView.getOverlays().add(infoWindow);
        }
    }

    private void runLater(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    private class Delegate extends IGoogleMapDelegate.Stub {
		@Override
		public CameraPosition getCameraPosition() throws RemoteException {
			if (mapView == null) return null;
			return new CameraPosition(new LatLng(mapView.getMapCenter()), mapView.getZoomLevel(), 0, 0);
		}

		@Override
		public float getMaxZoomLevel() throws RemoteException {
			return mapView.getMaxZoomLevel();
		}

		@Override
		public float getMinZoomLevel() throws RemoteException {
			return 1;
		}

		@Override
		public void moveCamera(final IObjectWrapper cameraUpdate) throws RemoteException {
            runLater(new Runnable() {
                @Override
                public void run() {
                    ((CameraUpdate) ObjectWrapper.unwrap(cameraUpdate)).update(GoogleMapImpl.this);
                }
            });
		}

		@Override
		public void animateCamera(final IObjectWrapper cameraUpdate) throws RemoteException {
            runLater(new Runnable() {
                @Override
                public void run() {
                    ((CameraUpdate) ObjectWrapper.unwrap(cameraUpdate)).update(GoogleMapImpl.this);
                }
            });
		}

		@Override
		public void animateCameraWithCallback(final IObjectWrapper cameraUpdate, final ICancelableCallback callback) throws RemoteException {
            runLater(new Runnable() {
                @Override
                public void run() {
                    ((CameraUpdate) ObjectWrapper.unwrap(cameraUpdate)).update(GoogleMapImpl.this);
                    if (callback != null) try {
                        callback.onFinish();
                    } catch (RemoteException e) {
                        Log.w(TAG, e);
                    }
                }
            });
		}

		@Override
		public void animateCameraWithDurationAndCallback(final IObjectWrapper cameraUpdate, int duration, final ICancelableCallback callback) throws RemoteException {
            runLater(new Runnable() {
                @Override
                public void run() {
                    ((CameraUpdate) ObjectWrapper.unwrap(cameraUpdate)).update(GoogleMapImpl.this);
                    if (callback != null) try {
                        callback.onFinish();
                    } catch (RemoteException e) {
                        Log.w(TAG, e);
                    }
                }
            });
		}

		@Override
		public void stopAnimation() throws RemoteException {
			mapView.getController().stopAnimation(false);
		}

		@Override
		public IPolylineDelegate addPolyline(PolylineOptions options) throws RemoteException {
			return new PolylineImpl(options);
		}

		@Override
		public IPolygonDelegate addPolygon(PolygonOptions options) throws RemoteException {
			return new PolygonImpl(options);
		}

		@Override
		public IMarkerDelegate addMarker(MarkerOptions options) throws RemoteException {
            MarkerImpl marker = new MarkerImpl("m" + markerCounter++, options, GoogleMapImpl.this);
            if (infoWindow != null) mapView.getOverlays().remove(infoWindow);
            mapView.getOverlays().add(marker.getOverlay());
            if (infoWindow != null) mapView.getOverlays().add(infoWindow);
            redraw();
			return marker;
		}

		@Override
		public IGroundOverlayDelegate addGroundOverlay(GroundOverlayOptions options) throws RemoteException {
			return new GroundOverlayImpl(options);
		}

		@Override
		public ITileOverlayDelegate addTileOverlay(TileOverlayOptions options) throws RemoteException {
			return null;
		}

		@Override
		public void clear() throws RemoteException {
            mapView.getOverlays().clear();
            hideInfoWindow();
            redraw();
		}

		@Override
		public int getMapType() throws RemoteException {
			return mapType;
		}

		@Override
		public void setMapType(int type) throws RemoteException {
			mapType = type;
			if (mapType == MAP_TYPE_SATELLITE) {
				mapView.setSatellite(true);
			} else {
				mapView.setSatellite(false);
			}
		}

		@Override
		public boolean isTrafficEnabled() throws RemoteException {
			return false;
		}

		@Override
		public void setTrafficEnabled(boolean traffic) throws RemoteException {

		}

		@Override
		public boolean isIndoorEnabled() throws RemoteException {
			return false;
		}

		@Override
		public void setIndoorEnabled(boolean indoor) throws RemoteException {

		}

		@Override
		public boolean isMyLocationEnabled() throws RemoteException {
			return false;
		}

		@Override
		public void setMyLocationEnabled(boolean myLocation) throws RemoteException {

		}

		@Override
		public Location getMyLocation() throws RemoteException {
			return null;
		}

		@Override
		public void setLocationSource(ILocationSourceDelegate locationSource) throws RemoteException {

		}

		@Override
		public IUiSettingsDelegate getUiSettings() throws RemoteException {
			return uiSettings;
		}

		@Override
		public IProjectionDelegate getProjection() throws RemoteException {
			return projection;
		}

		@Override
		public void setOnCameraChangeListener(IOnCameraChangeListener listener) throws RemoteException {
            cameraChangeListener = listener;
        }

		@Override
		public void setOnMapClickListener(final IOnMapClickListener listener) throws RemoteException {
            mapClickListener = listener;
        }

		@Override
		public void setOnMapLongClickListener(final IOnMapLongClickListener listener) throws RemoteException {
            mapLongClickListener = listener;
        }

		@Override
		public void setOnMarkerClickListener(IOnMarkerClickListener listener) throws RemoteException {
            markerClickListener = listener;
		}

		@Override
		public void setOnMarkerDragListener(IOnMarkerDragListener listener) throws RemoteException {
            markerDragListener = listener;
        }

		@Override
		public void setOnInfoWindowClickListener(IOnInfoWindowClickListener listener) throws RemoteException {
            infoWindowClickListener = listener;
        }

		@Override
		public void setInfoWindowAdapter(IInfoWindowAdapter adapter) throws RemoteException {
            infoWindowAdapter = adapter;
        }

		@Override
		public IObjectWrapper getTestingHelper() throws RemoteException {
			return null;
		}

		@Override
		public ICircleDelegate addCircle(CircleOptions options) throws RemoteException {
			return new CircleImpl(options);
		}

		@Override
		public void setOnMyLocationChangeListener(IOnMyLocationChangeListener listener) throws RemoteException {

		}

		@Override
		public void setOnMyLocationButtonClickListener(IOnMyLocationButtonClickListener listener) throws RemoteException {

		}

		@Override
		public void snapshot(ISnapshotReadyCallback callback, IObjectWrapper bitmap) throws RemoteException {

		}

		@Override
		public void setPadding(int left, int top, int right, int bottom) throws RemoteException {

		}

		@Override
		public boolean isBuildingsEnabled() throws RemoteException {
			return false;
		}

		@Override
		public void setBuildingsEnabled(boolean buildings) throws RemoteException {

		}

		@Override
		public void setOnMapLoadedCallback(IOnMapLoadedCallback callback) throws RemoteException {
            mapLoadedCallback = callback;
        }
    }

	private class UiSettings extends IUiSettingsDelegate.Stub {

		@Override
		public void setZoomControlsEnabled(boolean zoom) throws RemoteException {
			mapView.setBuiltInZoomControls(zoom);
		}

		@Override
		public void setCompassEnabled(boolean compass) throws RemoteException {
			// TODO
		}

		@Override
		public void setMyLocationButtonEnabled(boolean locationButton) throws RemoteException {
			// TODO
		}

		@Override
		public void setScrollGesturesEnabled(boolean scrollGestures) throws RemoteException {
			// TODO
		}

		@Override
		public void setZoomGesturesEnabled(boolean zoomGestures) throws RemoteException {
			// TODO
		}

		@Override
		public void setTiltGesturesEnabled(boolean tiltGestures) throws RemoteException {
			// TODO
		}

		@Override
		public void setRotateGesturesEnabled(boolean rotateGestures) throws RemoteException {
			// TODO
		}

		@Override
		public void setAllGesturesEnabled(boolean gestures) throws RemoteException {
			// TODO
		}

		@Override
		public boolean isZoomControlsEnabled() throws RemoteException {
			return false;
		}

		@Override
		public boolean isCompassEnabled() throws RemoteException {
			return false;
		}

		@Override
		public boolean isMyLocationButtonEnabled() throws RemoteException {
			return false;
		}

		@Override
		public boolean isScrollGesturesEnabled() throws RemoteException {
			return false;
		}

		@Override
		public boolean isZoomGesturesEnabled() throws RemoteException {
			return false;
		}

		@Override
		public boolean isTiltGesturesEnabled() throws RemoteException {
			return false;
		}

		@Override
		public boolean isRotateGesturesEnabled() throws RemoteException {
			return false;
		}
	}

    public class Projection extends IProjectionDelegate.Stub {

        @Override
        public IObjectWrapper toScreenLocation(LatLng latLng) throws RemoteException {
            return ObjectWrapper.wrap(mapView.getProjection().toPixels(latLng.toGeoPoint(), null));
        }

        @Override
        public LatLng fromScreenLocation(IObjectWrapper obj) throws RemoteException {
            Point point = (Point) ObjectWrapper.unwrap(obj);
            return new LatLng(mapView.getProjection().fromPixels(point.x, point.y));
        }

        @Override
        public VisibleRegion getVisibleRegion() throws RemoteException {
            LatLng nearLeft = new LatLng(mapView.getProjection().fromPixels(0, mapView.getHeight()));
            LatLng nearRight = new LatLng(mapView.getProjection().fromPixels(mapView.getWidth(), mapView.getHeight()));
            LatLng farLeft = new LatLng(mapView.getProjection().fromPixels(0, 0));
            LatLng farRight = new LatLng(mapView.getProjection().fromPixels(mapView.getWidth(), 0));

            return new VisibleRegion(nearLeft, nearRight, farLeft, farRight, new LatLngBounds(farRight, nearLeft));
        }
    }
}
