/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location;

import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.os.Looper;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    @NonNull
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
    public Task<Location> getCurrentLocation(@NonNull CurrentLocationRequest request, CancellationToken cancellationToken) {
        return null;
    }

    @NonNull
    @Override
    public Task<Location> getLastLocation(@NonNull LastLocationRequest request) {
        return null;
    }

    @NonNull
    public Task<Location> getLastLocation() {
        return scheduleTask((ReturningGoogleApiCall<Location, LocationClientImpl>) LocationClientImpl::getLastLocation);
    }

    @NonNull
    @Override
    public Task<LocationAvailability> getLocationAvailability() {
        return scheduleTask((ReturningGoogleApiCall<LocationAvailability, LocationClientImpl>) LocationClientImpl::getLocationAvailability);
    }

    @NonNull
    @Override
    public Task<Void> removeLocationUpdates(@NonNull LocationListener listener) {
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.removeLocationUpdates(listener));
    }

    @NonNull
    @Override
    public Task<Void> removeLocationUpdates(@NonNull PendingIntent pendingIntent) {
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.removeLocationUpdates(pendingIntent));
    }

    @NonNull
    @Override
    public Task<Void> requestLocationUpdates(@NonNull LocationRequest request, @NonNull LocationListener listener, @Nullable Looper looper) {
        Looper currentLooper = looper == null ? Looper.myLooper() : looper;
        if (currentLooper == null) throw new IllegalStateException("looper is null and the calling thread has not called Looper.prepare()");
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.requestLocationUpdates(request, listener, currentLooper));
    }

    @NonNull
    @Override
    public Task<Void> requestLocationUpdates(@NonNull LocationRequest request, @NonNull Executor executor, @NonNull LocationCallback callback) {
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.requestLocationUpdates(request, executor, callback));
    }

    @NonNull
    @Override
    public Task<Void> requestLocationUpdates(@NonNull LocationRequest request, @NonNull Executor executor, @NonNull LocationListener listener) {
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.requestLocationUpdates(request, executor, listener));
    }

    @NonNull
    @Override
    public Task<Void> requestLocationUpdates(@NonNull LocationRequest request, @NonNull LocationCallback callback, Looper looper) {
        Looper currentLooper = looper == null ? Looper.myLooper() : looper;
        if (currentLooper == null) throw new IllegalStateException("looper is null and the calling thread has not called Looper.prepare()");
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.requestLocationUpdates(request, callback, currentLooper));
    }

    @NonNull
    @Override
    public Task<Void> requestLocationUpdates(@NonNull LocationRequest request, @NonNull PendingIntent pendingIntent) {
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.requestLocationUpdates(request, pendingIntent));
    }

    @NonNull
    @Override
    public Task<Void> setMockLocation(@NonNull Location location) {
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.setMockLocation(location));
    }

    @NonNull
    @Override
    public Task<Void> setMockMode(boolean mockMode) {
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.setMockMode(mockMode));
    }

    @NonNull
    @Override
    public Task<Void> removeLocationUpdates(@NonNull LocationCallback callback) {
        return scheduleTask((VoidReturningGoogleApiCall<LocationClientImpl>) (client) -> client.removeLocationUpdates(callback));
    }
}
