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
import android.location.Location;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.microg.gms.common.GmsConnector;

@SuppressWarnings("deprecation")
public class FusedLocationProviderApiImpl implements FusedLocationProviderApi {
    private static final String TAG = "GmsFusedApiImpl";

    @Override
    public PendingResult<Status> flushLocations(GoogleApiClient client) {
        return null;
    }

    @Override
    public Location getLastLocation(GoogleApiClient client) {
        try {
            return LocationClientImpl.get(client).getLastLocation();
        } catch (RemoteException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    @Override
    public LocationAvailability getLocationAvailability(GoogleApiClient client) {
        try {
            return LocationClientImpl.get(client).getLocationAvailability();
        } catch (RemoteException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    @Override
    public PendingResult<Status> requestLocationUpdates(GoogleApiClient client,
                                                final LocationRequest request, final LocationListener listener) {
        return callVoid(client, new Runnable() {
            @Override
            public void run(LocationClientImpl client) throws RemoteException {
                client.requestLocationUpdates(request, listener);
            }
        });
    }

    @Override
    public PendingResult<Status> requestLocationUpdates(GoogleApiClient client, LocationRequest request, LocationCallback callback, Looper looper) {
        return callVoid(client, new Runnable() {
            @Override
            public void run(LocationClientImpl client) throws RemoteException {
                client.requestLocationUpdates(request, callback, looper);
            }
        });
    }

    @Override
    public PendingResult<Status> requestLocationUpdates(GoogleApiClient client,
                                                final LocationRequest request, final LocationListener listener,
                                                final Looper looper) {
        return callVoid(client, new Runnable() {
            @Override
            public void run(LocationClientImpl client) throws RemoteException {
                client.requestLocationUpdates(request, listener, looper);
            }
        });
    }

    @Override
    public PendingResult<Status> requestLocationUpdates(GoogleApiClient client,
                                                final LocationRequest request, final PendingIntent callbackIntent) {
        return callVoid(client, new Runnable() {
            @Override
            public void run(LocationClientImpl client) throws RemoteException {
                client.requestLocationUpdates(request, callbackIntent);
            }
        });
    }

    @Override
    public PendingResult<Status> removeLocationUpdates(GoogleApiClient client,
                                               final LocationListener listener) {
        return callVoid(client, new Runnable() {
            @Override
            public void run(LocationClientImpl client) throws RemoteException {
                client.removeLocationUpdates(listener);
            }
        });
    }

    @Override
    public PendingResult<Status> removeLocationUpdates(GoogleApiClient client, LocationCallback callback) {
        return callVoid(client, new Runnable() {
            @Override
            public void run(LocationClientImpl client) throws RemoteException {
                client.removeLocationUpdates(callback);
            }
        });
    }

    @Override
    public PendingResult<Status> removeLocationUpdates(GoogleApiClient client,
                                               final PendingIntent callbackIntent) {
        return callVoid(client, new Runnable() {
            @Override
            public void run(LocationClientImpl client) throws RemoteException {
                client.removeLocationUpdates(callbackIntent);
            }
        });
    }

    @Override
    public PendingResult<Status> setMockMode(GoogleApiClient client, final boolean isMockMode) {
        return callVoid(client, new Runnable() {
            @Override
            public void run(LocationClientImpl client) throws RemoteException {
                client.setMockMode(isMockMode);
            }
        });
    }

    @Override
    public PendingResult<Status> setMockLocation(GoogleApiClient client, final Location mockLocation) {
        return callVoid(client, new Runnable() {
            @Override
            public void run(LocationClientImpl client) throws RemoteException {
                client.setMockLocation(mockLocation);
            }
        });
    }

    private PendingResult<Status> callVoid(GoogleApiClient client, final Runnable runnable) {
        return GmsConnector.call(client, LocationServices.API, new GmsConnector.Callback<LocationClientImpl, Status>() {
            @Override
            public void onClientAvailable(LocationClientImpl client, ResultProvider<Status> resultProvider) throws RemoteException {
                runnable.run(client);
                resultProvider.onResultAvailable(Status.SUCCESS);
            }
        });
    }

    private interface Runnable {
        void run(LocationClientImpl client) throws RemoteException;
    }
}
