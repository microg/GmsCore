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
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingApi;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.internal.IGeofencerCallbacks;
import com.google.android.gms.location.internal.ParcelableGeofence;

import org.microg.gms.common.GmsConnector;

import java.util.ArrayList;
import java.util.List;

public class GeofencingApiImpl implements GeofencingApi {
    @Override
    public PendingResult<Status> addGeofences(GoogleApiClient client, final GeofencingRequest geofencingRequest, final PendingIntent pendingIntent) {
        return callGeofencer(client, new Runnable() {
            @Override
            public void run(LocationClientImpl client, IGeofencerCallbacks callbacks) throws RemoteException {
                client.addGeofences(geofencingRequest, pendingIntent, callbacks);
            }
        });
    }

    @Override
    public PendingResult<Status> addGeofences(GoogleApiClient client, final List<Geofence> geofences, final PendingIntent pendingIntent) {
        final List<ParcelableGeofence> geofenceList = new ArrayList<ParcelableGeofence>();
        for (Geofence geofence : geofences) {
            if (geofence instanceof ParcelableGeofence) geofenceList.add((ParcelableGeofence) geofence);
        }
        return callGeofencer(client, new Runnable() {
            @Override
            public void run(LocationClientImpl client, IGeofencerCallbacks callbacks) throws RemoteException {
                client.addGeofences(geofenceList, pendingIntent, callbacks);
            }
        });
    }

    @Override
    public PendingResult<Status> removeGeofences(GoogleApiClient client, final List<String> geofenceRequestIds) {
        return callGeofencer(client, new Runnable() {
            @Override
            public void run(LocationClientImpl client, IGeofencerCallbacks callbacks) throws RemoteException {
                client.removeGeofences(geofenceRequestIds, callbacks);
            }
        });
    }

    @Override
    public PendingResult<Status> removeGeofences(GoogleApiClient client, final PendingIntent pendingIntent) {
        return callGeofencer(client, new Runnable() {
            @Override
            public void run(LocationClientImpl client, IGeofencerCallbacks callbacks) throws RemoteException {
                client.removeGeofences(pendingIntent, callbacks);
            }
        });
    }

    @NonNull
    private IGeofencerCallbacks.Stub createGeofencerCallbacks(final GmsConnector.Callback.ResultProvider<Status> resultProvider) {
        return new IGeofencerCallbacks.Stub(){
            @Override
            public void onAddGeofenceResult(int statusCode, String[] requestIds) throws RemoteException {
                resultProvider.onResultAvailable(new Status(statusCode));
            }

            @Override
            public void onRemoveGeofencesByRequestIdsResult(int statusCode, String[] requestIds) throws RemoteException {
                resultProvider.onResultAvailable(new Status(statusCode));
            }

            @Override
            public void onRemoveGeofencesByPendingIntentResult(int statusCode, PendingIntent pendingIntent) throws RemoteException {
                resultProvider.onResultAvailable(new Status(statusCode));
            }
        };
    }

    private PendingResult<Status> callGeofencer(GoogleApiClient client, final Runnable runnable) {
        return GmsConnector.call(client, LocationServices.API, new GmsConnector.Callback<LocationClientImpl, Status>() {
            @Override
            public void onClientAvailable(LocationClientImpl client, ResultProvider<Status> resultProvider) throws RemoteException {
                runnable.run(client, createGeofencerCallbacks(resultProvider));
            }
        });
    }

    private interface Runnable {
        void run(LocationClientImpl client, IGeofencerCallbacks callbacks) throws RemoteException;
    }
}
