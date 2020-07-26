/*
 * Copyright (C) 2013-2017 microG Project Team
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

package org.microg.gms.location;

import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.ILocationListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.internal.IGeofencerCallbacks;
import com.google.android.gms.location.internal.ParcelableGeofence;

import org.microg.gms.common.api.ConnectionCallbacks;
import org.microg.gms.common.api.GoogleApiClientImpl;
import org.microg.gms.common.api.OnConnectionFailedListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationClientImpl extends GoogleLocationManagerClient {
    private static final String TAG = "GmsLocationClientImpl";
    private NativeLocationClientImpl nativeLocation = null;
    private Map<LocationListener, ILocationListener> listenerMap = new HashMap<LocationListener, ILocationListener>();


    public LocationClientImpl(Context context, ConnectionCallbacks callbacks,
                              OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener);
        Log.d(TAG, "<init>");
    }

    public static LocationClientImpl get(GoogleApiClient apiClient) {
        if (apiClient instanceof GoogleApiClientImpl) {
            return (LocationClientImpl) ((GoogleApiClientImpl) apiClient)
                    .getApiConnection(LocationServices.API);
        }
        return null;
    }

    public void addGeofences(GeofencingRequest request, PendingIntent pendingIntent, IGeofencerCallbacks callbacks) throws RemoteException {
        if (nativeLocation != null) {
            nativeLocation.addGeofences(request, pendingIntent, callbacks);
        } else {
            getServiceInterface().addGeofences(request, pendingIntent, callbacks);
        }
    }

    public void addGeofences(List<ParcelableGeofence> request, PendingIntent pendingIntent, IGeofencerCallbacks callbacks) throws RemoteException {
        if (nativeLocation != null) {
            nativeLocation.addGeofences(request, pendingIntent, callbacks);
        } else {
            getServiceInterface().addGeofencesList(request, pendingIntent, callbacks, getContext().getPackageName());
        }
    }

    public void removeGeofences(List<String> geofenceRequestIds, IGeofencerCallbacks callbacks) throws RemoteException {
        if (nativeLocation != null) {
            nativeLocation.removeGeofences(geofenceRequestIds, callbacks);
        } else {
            getServiceInterface().removeGeofencesById(geofenceRequestIds.toArray(new String[geofenceRequestIds.size()]), callbacks, getContext().getPackageName());
        }
    }

    public void removeGeofences(PendingIntent pendingIntent, IGeofencerCallbacks callbacks) throws RemoteException {
        if (nativeLocation != null) {
            nativeLocation.removeGeofences(pendingIntent, callbacks);
        } else {
            getServiceInterface().removeGeofencesByIntent(pendingIntent, callbacks, getContext().getPackageName());
        }
    }

    public Location getLastLocation() throws RemoteException {
        Log.d(TAG, "getLastLocation()");
        if (nativeLocation != null) {
            return nativeLocation.getLastLocation();
        } else {
            return getServiceInterface().getLastLocation();
        }
    }

    public void requestLocationUpdates(LocationRequest request, final LocationListener listener)
            throws RemoteException {
        if (nativeLocation != null) {
            nativeLocation.requestLocationUpdates(request, listener);
        } else {
            if (!listenerMap.containsKey(listener)) {
                listenerMap.put(listener, new ILocationListener.Stub() {
                    @Override
                    public void onLocationChanged(Location location) throws RemoteException {
                        listener.onLocationChanged(location);
                    }
                });
            }
            getServiceInterface().requestLocationUpdatesWithPackage(request,
                    listenerMap.get(listener), getContext().getPackageName());
        }
    }

    public void requestLocationUpdates(LocationRequest request, PendingIntent pendingIntent)
            throws RemoteException {
        if (nativeLocation != null) {
            nativeLocation.requestLocationUpdates(request, pendingIntent);
        } else {
            getServiceInterface().requestLocationUpdatesWithIntent(request, pendingIntent);
        }
    }

    public void requestLocationUpdates(LocationRequest request, LocationListener listener,
                                       Looper looper) throws RemoteException {
        if (nativeLocation != null) {
            nativeLocation.requestLocationUpdates(request, listener, looper);
        }
        requestLocationUpdates(request, listener); // TODO
    }

    public void removeLocationUpdates(LocationListener listener) throws RemoteException {
        if (nativeLocation != null) {
            nativeLocation.removeLocationUpdates(listener);
        } else {
            getServiceInterface().removeLocationUpdatesWithListener(listenerMap.get(listener));
        }
    }

    public void removeLocationUpdates(PendingIntent pendingIntent) throws RemoteException {
        if (nativeLocation != null) {
            nativeLocation.removeLocationUpdates(pendingIntent);
        } else {
            getServiceInterface().removeLocationUpdatesWithIntent(pendingIntent);
        }
    }

    public void setMockMode(boolean isMockMode) throws RemoteException {
        if (nativeLocation != null) {
            nativeLocation.setMockMode(isMockMode);
        } else {
            getServiceInterface().setMockMode(isMockMode);
        }
    }

    public void setMockLocation(Location mockLocation) throws RemoteException {
        if (nativeLocation != null) {
            nativeLocation.setMockLocation(mockLocation);
        } else {
            getServiceInterface().setMockLocation(mockLocation);
        }
    }

    @Override
    public void handleConnectionFailed() {
        // DO NOT call super here, because fails are not really problems :)
        nativeLocation = new NativeLocationClientImpl(this);
        state = ConnectionState.PSEUDO_CONNECTED;
        Bundle bundle = new Bundle();
        bundle.putBoolean("fallback_to_native_active", true);
        callbacks.onConnected(bundle);
    }
}
