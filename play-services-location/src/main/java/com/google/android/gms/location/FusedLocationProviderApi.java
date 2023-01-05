/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

/**
 * The main entry point for interacting with the fused location provider.
 * <p>
 * The methods must be used in conjunction with a {@link GoogleApiClient}.
 *
 * @deprecated Use the GoogleApi-based API {@link FusedLocationProviderClient} instead.
 */
@Deprecated
public interface FusedLocationProviderApi {
    /**
     * Key used for a Bundle extra holding a {@link Location} value when a location change is broadcast using a PendingIntent.
     *
     * @deprecated Use {@link LocationResult#hasResult(Intent)} and {@link LocationResult#extractResult(Intent)}.
     * You may also receive {@link LocationAvailability} in the Intent which you can access using
     * {@link LocationAvailability#hasLocationAvailability(Intent)} and {@link LocationAvailability#extractLocationAvailability(Intent)}.
     */
    @Deprecated
    String KEY_LOCATION_CHANGED = "com.google.android.location.LOCATION";
    /**
     * Key used for the Bundle extra in {@link Location} object holding a boolean indicating whether the location was
     * set using {@link #setMockLocation(GoogleApiClient, Location)}. If the value is false this extra is not set.
     *
     * @deprecated Prefer to use {@link LocationCompat#isMock()} from the compat libraries.
     */
    String KEY_MOCK_LOCATION = "mockLocation";

    /**
     * Flushes any locations currently being batched and sends them to all registered {@link LocationListener}s,
     * {@link LocationCallback}s, and {@link PendingIntent}s. This call is only useful when batching is specified using
     * {@link LocationRequest#setMaxWaitTime(long)}, otherwise locations are already delivered immediately when available.
     * <p>
     * When the returned {@link PendingResult} is complete, then you can assume that any pending batched locations have already been delivered.
     */
    PendingResult<Status> flushLocations(GoogleApiClient client);

    /**
     * Returns the best most recent location currently available.
     * <p>
     * If a location is not available, which should happen very rarely, null will be returned. The best accuracy
     * available while respecting the location permissions will be returned.
     * <p>
     * This method provides a simplified way to get location. It is particularly well suited for applications that do
     * not require an accurate location and that do not want to maintain extra logic for location updates.
     *
     * @param client An existing GoogleApiClient. If not connected null will be returned.
     */
    Location getLastLocation(GoogleApiClient client);

    /**
     * Returns the availability of location data. When {@link LocationAvailability#isLocationAvailable()} returns true,
     * then the location returned by {@link #getLastLocation(GoogleApiClient)} will be reasonably up to date within the
     * hints specified by the active LocationRequests.
     * <p>
     * If the client isn't connected to Google Play services and the request times out, null is returned.
     * <p>
     * Note it's always possible for {@link #getLastLocation(GoogleApiClient)} to return null even when this method
     * returns true (e.g. location settings were disabled between calls).
     *
     * @param client An existing GoogleApiClient. If not connected null will be returned.
     */
    LocationAvailability getLocationAvailability(GoogleApiClient client);

    /**
     * Removes all location updates for the given pending intent.
     * <p>
     * It is possible for this call to cancel the PendingIntent under some circumstances.
     *
     * @param client         An existing GoogleApiClient. It must be connected at the time of this call, which is normally
     *                       achieved by calling {@link GoogleApiClient#connect()} and waiting for {@link GoogleApiClient.ConnectionCallbacks#onConnected(Bundle)} to be called.
     * @param callbackIntent The PendingIntent that was used in {@link #requestLocationUpdates(GoogleApiClient, LocationRequest, PendingIntent)}
     *                       or is equal as defined by {@link PendingIntent#equals(Object)}.
     * @return a PendingResult for the call, check {@link Status#isSuccess()} to determine if it was successful.
     */
    PendingResult<Status> removeLocationUpdates(GoogleApiClient client, PendingIntent callbackIntent);

    PendingResult<Status> removeLocationUpdates(GoogleApiClient client, LocationListener listener);

    PendingResult<Status> removeLocationUpdates(GoogleApiClient client, LocationCallback callback);

    PendingResult<Status> requestLocationUpdates(GoogleApiClient client, LocationRequest request, LocationListener listener);

    PendingResult<Status> requestLocationUpdates(GoogleApiClient client, LocationRequest request, LocationCallback callback, Looper looper);

    PendingResult<Status> requestLocationUpdates(GoogleApiClient client, LocationRequest request, LocationListener listener, Looper looper);

    PendingResult<Status> requestLocationUpdates(GoogleApiClient client, LocationRequest request, PendingIntent callbackIntent);

    PendingResult<Status> setMockLocation(GoogleApiClient client, Location mockLocation);

    PendingResult<Status> setMockMode(GoogleApiClient client, boolean isMockMode);
}
