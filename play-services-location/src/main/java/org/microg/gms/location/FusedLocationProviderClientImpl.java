/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location;

import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import com.google.android.gms.location.*;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.Task;

import org.microg.gms.common.api.ReturningGoogleApiCall;
import org.microg.gms.common.api.PendingGoogleApiCall;
import org.microg.gms.common.api.VoidReturningGoogleApiCall;

import java.util.concurrent.Executor;

public class FusedLocationProviderClientImpl extends FusedLocationProviderClient {
    public FusedLocationProviderClientImpl(Context context) {
        super(context);
    }

    public Task<Void> flushLocations() {
        return scheduleTask((ReturningGoogleApiCall<Void, LocationClientImpl>) (client) -> null);
    }

    @NonNull
    @Override
    public Task<Location> getCurrentLocation(int priority, CancellationToken cancellationToken) {
        return null;
    }

    @NonNull
    @Override
    public Task<Location> getCurrentLocation(CurrentLocationRequest request, CancellationToken cancellationToken) {
        return null;
    }

    @NonNull
    @Override
    public Task<Location> getLastLocation(LastLocationRequest request) {
        return null;
    }

    public Task<Location> getLastLocation() {
        return scheduleTask((ReturningGoogleApiCall<Location, LocationClientImpl>) LocationClientImpl::getLastLocation);
    }

    @NonNull
    @Override
    public Task<LocationAvailability> getLocationAvailability() {
        return null;
    }

    @Override
    public Task<Void> removeLocationUpdates(LocationListener listener) {
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.removeLocationUpdates(listener));
    }

    @Override
    public Task<Void> removeLocationUpdates(PendingIntent pendingIntent) {
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.removeLocationUpdates(pendingIntent));
    }

    @Override
    public Task<Void> requestLocationUpdates(LocationRequest request, LocationListener listener, Looper looper) {
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.requestLocationUpdates(request, listener, looper));
    }

    @Override
    public Task<Void> requestLocationUpdates(LocationRequest request, Executor executor, LocationCallback callback) {
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.requestLocationUpdates(request, executor, callback));
    }

    @Override
    public Task<Void> requestLocationUpdates(LocationRequest request, Executor executor, LocationListener listener) {
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.requestLocationUpdates(request, executor, listener));
    }

    @Override
    public Task<Void> requestLocationUpdates(LocationRequest request, LocationCallback callback, Looper looper) {
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.requestLocationUpdates(request, callback, looper));
    }

    @Override
    public Task<Void> requestLocationUpdates(LocationRequest request, PendingIntent pendingIntent) {
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.requestLocationUpdates(request, pendingIntent));
    }

    @Override
    public Task<Void> setMockLocation(Location location) {
        return null;
    }

    @Override
    public Task<Void> setMockMode(boolean mockMode) {
        return null;
    }

    @Override
    public Task<Void> removeLocationUpdates(LocationCallback callback) {
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.removeLocationUpdates(callback));
    }
}
