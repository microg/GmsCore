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

package org.microg.gms.maps.camera;

import android.os.RemoteException;
import android.util.Log;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.maps.internal.ICameraUpdateFactoryDelegate;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.maps.GeoPoint;
import org.microg.gms.maps.GoogleMapImpl;

public class CameraUpdateFactoryImpl extends ICameraUpdateFactoryDelegate.Stub {
	private static final String TAG = CameraUpdateFactoryImpl.class.getName();

	@Override
	public IObjectWrapper zoomIn() throws RemoteException {
		Log.d(TAG, "zoomIn");
		return new ObjectWrapper<CameraUpdate>(new CameraUpdate() {
			@Override
			public void update(GoogleMapImpl map) {
				map.getController().zoomIn();
			}
		});
	}

	@Override
	public IObjectWrapper zoomOut() throws RemoteException {
		Log.d(TAG, "zoomOut");
		return new ObjectWrapper<CameraUpdate>(new CameraUpdate() {
			@Override
			public void update(GoogleMapImpl map) {
				map.getController().zoomOut();
			}
		});
	}

	@Override
	public IObjectWrapper scrollBy(final float x, final float y) throws RemoteException {
		Log.d(TAG, "scrollBy");
		return new ObjectWrapper<CameraUpdate>(new CameraUpdate() {
			@Override
			public void update(GoogleMapImpl map) {
				map.getController().scrollBy((int) x, (int) y);
			}
		});
	}

	@Override
	public IObjectWrapper zoomTo(final float zoom) throws RemoteException {
		Log.d(TAG, "zoomTo");
		return new ObjectWrapper<CameraUpdate>(new CameraUpdate() {
			@Override
			public void update(GoogleMapImpl map) {
				map.getController().setZoom((int) zoom);
			}
		});
	}

	@Override
	public IObjectWrapper zoomBy(final float zoomDelta) throws RemoteException {
		Log.d(TAG, "zoomBy");
		return new ObjectWrapper<CameraUpdate>(new CameraUpdate() {
			@Override
			public void update(GoogleMapImpl map) {
				map.getController().setZoom((int) (map.getMapView().getZoomLevel() + zoomDelta));
			}
		});
	}

	@Override
	public IObjectWrapper zoomByWithFocus(final float zoomDelta, int x, int y) throws RemoteException {
		Log.d(TAG, "zoomByWithFocus");
		return new ObjectWrapper<CameraUpdate>(new CameraUpdate() {
			@Override
			public void update(GoogleMapImpl map) {
				// TODO focus
				map.getController().setZoom((int) (map.getMapView().getZoomLevel() + zoomDelta));
			}
		});
	}

	@Override
	public IObjectWrapper newCameraPosition(final CameraPosition cameraPosition) throws RemoteException {
		Log.d(TAG, "newCameraPosition");
        if (cameraPosition == null) {
            return new ObjectWrapper<CameraUpdate>(new CameraUpdate() {
                @Override
                public void update(GoogleMapImpl map) {
                    // Nothing
                }
            });
        }
		return newLatLngZoom(cameraPosition.target, cameraPosition.zoom);
	}

	@Override
	public IObjectWrapper newLatLng(final LatLng latLng) throws RemoteException {
		Log.d(TAG, "newLatLng");
		return new ObjectWrapper<CameraUpdate>(new CameraUpdate() {
			@Override
			public void update(GoogleMapImpl map) {
				map.getController().setCenter(new GeoPoint((int) (latLng.latitude * 1E6), (int) (latLng.longitude * 1E6)));
			}
		});
	}

	@Override
	public IObjectWrapper newLatLngZoom(final LatLng latLng, final float zoom) throws RemoteException {
		Log.d(TAG, "newLatLngZoom");
		return new ObjectWrapper<CameraUpdate>(new CameraUpdate() {
			@Override
			public void update(GoogleMapImpl map) {
				map.getController().setZoom((int) zoom);
				map.getController().setCenter(new GeoPoint((int) (latLng.latitude * 1E6), (int) (latLng.longitude * 1E6)));
			}
		});
	}

	@Override
	public IObjectWrapper newLatLngBounds(final LatLngBounds bounds, int i) throws RemoteException {
		Log.d(TAG, "newLatLngBounds");
		return new ObjectWrapper<CameraUpdate>(new CameraUpdate() {
			@Override
			public void update(GoogleMapImpl map) {
                double latSpan = bounds.northEast.latitude - bounds.southWest.latitude,
                        lonSpan = bounds.northEast.longitude - bounds.southWest.longitude;
                map.getController().zoomToSpan((int) (latSpan * 1E6), (int) (lonSpan * 1E6));
                map.getController().setCenter(new GeoPoint((int) ((bounds.southWest.latitude + latSpan/2) * 1E6),
                        (int) ((bounds.southWest.longitude + lonSpan/2) * 1E6)));
            }
        });
    }

	@Override
	public IObjectWrapper newLatLngBoundsWithSize(LatLngBounds bounds, int i1, int i2, int i3) throws RemoteException {
		Log.d(TAG, "newLatLngBoundsWithSize");
		return new ObjectWrapper<CameraUpdate>(new CameraUpdate() {
			@Override
			public void update(GoogleMapImpl map) {
				// TODO
			}
		});
	}
}
