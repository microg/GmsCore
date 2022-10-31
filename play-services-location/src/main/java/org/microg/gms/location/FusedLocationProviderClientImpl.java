/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location;

import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.os.RemoteException;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import org.microg.gms.common.api.InstantGoogleApiCall;
import org.microg.gms.common.api.PendingGoogleApiCall;

public class FusedLocationProviderClientImpl extends FusedLocationProviderClient {
    public FusedLocationProviderClientImpl(Context context) {
        super(context);
    }

    public Task<Void> flushLocations() {
        return scheduleTask(new PendingGoogleApiCall<Void, LocationClientImpl>() {
            @Override
            public void execute(LocationClientImpl client, TaskCompletionSource<Void> completionSource) {
                completionSource.setResult(null);
            }
        });
    }

    public Task<Location> getLastLocation() {
        return scheduleTask((InstantGoogleApiCall<Location, LocationClientImpl>) LocationClientImpl::getLastLocation);
    }

    @Override
    public Task<Void> requestLocationUpdates(LocationRequest request, LocationCallback callback, Looper looper) {
        return scheduleTask(new PendingGoogleApiCall<Void, LocationClientImpl>() {
            @Override
            public void execute(LocationClientImpl client, TaskCompletionSource<Void> completionSource) {
                try {
                    client.requestLocationUpdates(request, callback, looper);
                    completionSource.setResult(null);
                } catch (RemoteException e) {
                    completionSource.setException(e);
                }
            }
        });
    }
}
