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
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.ILocationListener;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.internal.IGeofencerCallbacks;
import com.google.android.gms.location.internal.ParcelableGeofence;

import org.microg.gms.common.api.ConnectionCallbacks;
import org.microg.gms.common.api.GoogleApiClientImpl;
import org.microg.gms.common.api.OnConnectionFailedListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class LocationClientImpl extends GoogleLocationManagerClient {
    private static final String TAG = "GmsLocationClientImpl";
    private Map<LocationListener, ILocationListener> listenerMap = new HashMap<LocationListener, ILocationListener>();
    private Map<LocationCallback, ILocationListener> callbackMap = new HashMap<LocationCallback, ILocationListener>();


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
        getServiceInterface().addGeofences(request, pendingIntent, callbacks);
    }

    public void addGeofences(List<ParcelableGeofence> request, PendingIntent pendingIntent, IGeofencerCallbacks callbacks) throws RemoteException {
        getServiceInterface().addGeofencesList(request, pendingIntent, callbacks, getContext().getPackageName());
    }

    public void removeGeofences(List<String> geofenceRequestIds, IGeofencerCallbacks callbacks) throws RemoteException {
        getServiceInterface().removeGeofencesById(geofenceRequestIds.toArray(new String[geofenceRequestIds.size()]), callbacks, getContext().getPackageName());
    }

    public void removeGeofences(PendingIntent pendingIntent, IGeofencerCallbacks callbacks) throws RemoteException {
        getServiceInterface().removeGeofencesByIntent(pendingIntent, callbacks, getContext().getPackageName());
    }

    public Location getLastLocation() throws RemoteException {
        return getServiceInterface().getLastLocationWithPackage(getContext().getPackageName());
    }

    public LocationAvailability getLocationAvailability() throws RemoteException {
        return getServiceInterface().getLocationAvailabilityWithPackage(getContext().getPackageName());
    }

    public void requestLocationUpdates(LocationRequest request, final LocationListener listener)
            throws RemoteException {
        if (!listenerMap.containsKey(listener)) {
            listenerMap.put(listener, new ILocationListener.Stub() {
                @Override
                public void onLocationChanged(Location location) throws RemoteException {
                    listener.onLocationChanged(location);
                }

                @Override
                public void cancel() throws RemoteException {

                }
            });
        }
        getServiceInterface().requestLocationUpdatesWithPackage(request, listenerMap.get(listener), getContext().getPackageName());
    }

    public void requestLocationUpdates(LocationRequest request, PendingIntent pendingIntent)
            throws RemoteException {
        getServiceInterface().requestLocationUpdatesWithIntent(request, pendingIntent);
    }

    public void requestLocationUpdates(LocationRequest request, LocationListener listener, @NonNull Looper looper) throws RemoteException {
        final Handler handler = new Handler(looper);
        requestLocationUpdates(request, handler::post, listener);
    }

    public void requestLocationUpdates(LocationRequest request, Executor executor, LocationListener listener) throws RemoteException {
        if (!listenerMap.containsKey(listener)) {
            listenerMap.put(listener, new ILocationListener.Stub() {
                @Override
                public void onLocationChanged(Location location) throws RemoteException {
                    executor.execute(() -> listener.onLocationChanged(location));
                }

                @Override
                public void cancel() throws RemoteException {

                }
            });
        }
        getServiceInterface().requestLocationUpdatesWithPackage(request, listenerMap.get(listener), getContext().getPackageName());
    }

    public void requestLocationUpdates(LocationRequest request, LocationCallback callback, @NonNull Looper looper) throws RemoteException {
        final Handler handler = new Handler(looper);
        requestLocationUpdates(request, handler::post, callback);
    }

    public void requestLocationUpdates(LocationRequest request, Executor executor, LocationCallback callback) throws RemoteException {
        if (!callbackMap.containsKey(callback)) {
            callbackMap.put(callback, new ILocationListener.Stub() {
                @Override
                public void onLocationChanged(Location location) throws RemoteException {
                    executor.execute(() -> callback.onLocationResult(LocationResult.create(Collections.singletonList(location))));
                }

                @Override
                public void cancel() throws RemoteException {

                }
            });
        }
        getServiceInterface().requestLocationUpdatesWithPackage(request, callbackMap.get(callback), getContext().getPackageName());
    }

    public void removeLocationUpdates(LocationListener listener) throws RemoteException {
        getServiceInterface().removeLocationUpdatesWithListener(listenerMap.get(listener));
    }

    public void removeLocationUpdates(LocationCallback callback) throws RemoteException {
        getServiceInterface().removeLocationUpdatesWithListener(callbackMap.get(callback));
    }

    public void removeLocationUpdates(PendingIntent pendingIntent) throws RemoteException {
        getServiceInterface().removeLocationUpdatesWithIntent(pendingIntent);
    }

    public void setMockMode(boolean isMockMode) throws RemoteException {
        getServiceInterface().setMockMode(isMockMode);
    }

    public void setMockLocation(Location mockLocation) throws RemoteException {
        getServiceInterface().setMockLocation(mockLocation);
    }
}
