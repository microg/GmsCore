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

package com.google.android.gms.location;

import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.os.Looper;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;

import org.microg.gms.common.ForwardConnectionCallbacks;
import org.microg.gms.common.ForwardConnectionFailedListener;
import org.microg.gms.common.api.AbstractPlayServicesClient;

/**
 * This class is deprecated as of play services 6.5, do not use it in production systems,
 * it's just a forwarder for the {@link FusedLocationProviderApi}.
 */
@Deprecated
public class LocationClient extends AbstractPlayServicesClient {
    public static final String KEY_LOCATION_CHANGED = "com.google.android.location.LOCATION";

    public LocationClient(Context context, ConnectionCallbacks callbacks,
            OnConnectionFailedListener connectionFailedListener) {
        super(new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new ForwardConnectionCallbacks(callbacks))
                .addOnConnectionFailedListener(new ForwardConnectionFailedListener
                        (connectionFailedListener))
                .build());
    }

    public Location getLastLocation() {
        assertConnected();
        return LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
    }

    public void requestLocationUpdates(LocationRequest request,
            LocationListener listener) {
        assertConnected();
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request,
                listener).await();
    }

    public void requestLocationUpdates(LocationRequest request,
            LocationListener listener, Looper looper) {
        assertConnected();
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request,
                listener, looper).await();
    }

    public void requestLocationUpdates(LocationRequest request,
            PendingIntent callbackIntent) {
        assertConnected();
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request,
                callbackIntent).await();
    }

    public void removeLocationUpdates(LocationListener listener) {
        assertConnected();
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, listener).await();
    }

    public void removeLocationUpdates(PendingIntent callbackIntent) {
        assertConnected();
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,
                callbackIntent).await();
    }

    public void setMockMode(boolean isMockMode) {
        assertConnected();
        LocationServices.FusedLocationApi.setMockMode(googleApiClient, isMockMode).await();
    }

    public void setMockLocation(Location mockLocation) {
        assertConnected();
        LocationServices.FusedLocationApi.setMockLocation(googleApiClient, mockLocation).await();
    }
}
