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

package com.google.android.gms.maps.internal;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.maps.model.internal.*;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

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
	private MapView mapView;
	private int mapType = 1;

	public GoogleMapImpl(LayoutInflater inflater, GoogleMapOptions options) {
		this.view = new FrameLayout(inflater.getContext());
		try {
			mapView = (MapView) Class.forName("com.google.android.maps.MapView").getConstructor(Context.class, String.class).newInstance(inflater.getContext(), null);
			view.addView(mapView);
		} catch (Exception e) {
			Log.d(TAG, "Sorry, can't create legacy MapView");
		}
		this.options = options;
	}

	public void onCreate(Bundle savedInstanceState) {

	}

	MapView getMapView() {
		return mapView;
	}

	MapController getController() {
		return mapView != null ? mapView.getController() : null;
	}

	public View getView() {
		return view;
	}

	public IGoogleMapDelegate getDelegate() {
		return delegate;
	}

	private class Delegate extends IGoogleMapDelegate.Stub {
		@Override
		public CameraPosition getCameraPosition() throws RemoteException {
			if (mapView == null) return null;
			LatLng center = new LatLng(mapView.getMapCenter().getLatitudeE6() / 1E6F, mapView.getMapCenter().getLongitudeE6() / 1E6F);
			return new CameraPosition(center, mapView.getZoomLevel(), 0, 0);
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
		public void moveCamera(IObjectWrapper cameraUpdate) throws RemoteException {
			((CameraUpdate) ObjectWrapper.unwrap(cameraUpdate)).update(GoogleMapImpl.this);
		}

		@Override
		public void animateCamera(IObjectWrapper cameraUpdate) throws RemoteException {
			((CameraUpdate) ObjectWrapper.unwrap(cameraUpdate)).update(GoogleMapImpl.this);
		}

		@Override
		public void animateCameraWithCallback(IObjectWrapper cameraUpdate, ICancelableCallback callback) throws RemoteException {
			((CameraUpdate) ObjectWrapper.unwrap(cameraUpdate)).update(GoogleMapImpl.this);
			if (callback != null) callback.onFinish();
		}

		@Override
		public void animateCameraWithDurationAndCallback(IObjectWrapper cameraUpdate, int duration, ICancelableCallback callback) throws RemoteException {
			((CameraUpdate) ObjectWrapper.unwrap(cameraUpdate)).update(GoogleMapImpl.this);
			if (callback != null) callback.onFinish();
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
			return null;
		}

		@Override
		public IMarkerDelegate addMarker(MarkerOptions options) throws RemoteException {
			return null;
		}

		@Override
		public IGroundOverlayDelegate addGroundOverlay(GroundOverlayOptions options) throws RemoteException {
			return null;
		}

		@Override
		public ITileOverlayDelegate addTileOverlay(TileOverlayOptions options) throws RemoteException {
			return null;
		}

		@Override
		public void clear() throws RemoteException {

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
			return new UiSettings();
		}

		@Override
		public IProjectionDelegate getProjection() throws RemoteException {
			return null;
		}

		@Override
		public void setOnCameraChangeListener(IOnCameraChangeListener listener) throws RemoteException {

		}

		@Override
		public void setOnMapClickListener(final IOnMapClickListener listener) throws RemoteException {
			mapView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						Log.d(TAG, "onMapClick:");
						listener.onMapClick(new LatLng(0, 0));
					} catch (RemoteException ignored) {
					}
				}
			});
		}

		@Override
		public void setOnMapLongClickListener(final IOnMapLongClickListener listener) throws RemoteException {
			mapView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					try {
						Log.d(TAG, "onMapLongClick:");
						listener.onMapLongClick(new LatLng(0, 0));
					} catch (RemoteException e) {
						return false;
					}
					return true;
				}
			});
		}

		@Override
		public void setOnMarkerClickListener(IOnMarkerClickListener listener) throws RemoteException {

		}

		@Override
		public void setOnMarkerDragListener(IOnMarkerDragListener listener) throws RemoteException {

		}

		@Override
		public void setOnInfoWindowClickListener(IOnInfoWindowClickListener listener) throws RemoteException {

		}

		@Override
		public void setInfoWindowAdapter(IInfoWindowAdapter adapter) throws RemoteException {

		}

		@Override
		public IObjectWrapper getTestingHelper() throws RemoteException {
			return null;
		}

		@Override
		public ICircleDelegate addCircle(CircleOptions options) throws RemoteException {
			return null;
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
}
