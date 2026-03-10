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
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

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
    @NonNull
    String KEY_LOCATION_CHANGED = "com.google.android.location.LOCATION";
    /**
     * Key used for the Bundle extra in {@link Location} object holding a boolean indicating whether the location was
     * set using {@link #setMockLocation(GoogleApiClient, Location)}. If the value is false this extra is not set.
     *
     * @deprecated Prefer to use {@link LocationCompat#isMock()} from the compat libraries.
     */
    @NonNull
    String KEY_MOCK_LOCATION = "mockLocation";

    /**
     * Flushes any locations currently being batched and sends them to all registered {@link LocationListener}s,
     * {@link LocationCallback}s, and {@link PendingIntent}s. This call is only useful when batching is specified using
     * {@link LocationRequest#setMaxWaitTime(long)}, otherwise locations are already delivered immediately when available.
     * <p>
     * When the returned {@link PendingResult} is complete, then you can assume that any pending batched locations have already been delivered.
     */
    @NonNull
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
    @Nullable
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
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
    @Nullable
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
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
    @NonNull
    PendingResult<Status> removeLocationUpdates(GoogleApiClient client, PendingIntent callbackIntent);

    /**
     * Removes all location updates for the given location listener.
     *
     * @param client   An existing GoogleApiClient. It must be connected at the time of this call, which is normally achieved by calling
     *                 {@link GoogleApiClient#connect()} and waiting for {@link GoogleApiClient.ConnectionCallbacks#onConnected} to be called.
     * @param listener The listener to remove.
     * @return a PendingResult for the call, check {@link Status#isSuccess()} to determine if it was successful.
     */
    @NonNull
    PendingResult<Status> removeLocationUpdates(GoogleApiClient client, LocationListener listener);

    /**
     * Removes all location updates for the given location result listener.
     *
     * @param client   An existing GoogleApiClient. It must be connected at the time of this call, which is normally achieved by calling
     *                 {@link GoogleApiClient#connect()} and waiting for {@link GoogleApiClient.ConnectionCallbacks#onConnected} to be called.
     * @param callback The callback to remove.
     * @return a PendingResult for the call, check {@link Status#isSuccess()} to determine if it was successful.
     */
    @NonNull
    PendingResult<Status> removeLocationUpdates(GoogleApiClient client, LocationCallback callback);

    /**
     * Requests location updates.
     * <p>
     * This method is suited for the foreground use cases, more specifically for requesting locations while being connected to {@link GoogleApiClient}. For
     * background use cases, the {@link PendingIntent} version of the method is recommended, see
     * {@link #requestLocationUpdates(GoogleApiClient, LocationRequest, PendingIntent)}.
     * <p>
     * Any previous LocationRequests registered on this LocationListener will be replaced.
     * <p>
     * Callbacks for LocationListener will be made on the calling thread, which must already be a prepared looper thread, such as the main thread of the
     * calling Activity. The variant of this method with a {@link Looper} is recommended for cases where the callback needs to happen on a specific thread. See
     * {@link #requestLocationUpdates(GoogleApiClient, LocationRequest, LocationListener, Looper)}.
     *
     * @param client   An existing GoogleApiClient. It must be connected at the time of this call, which is normally achieved by calling
     *                 {@link GoogleApiClient#connect()} and waiting for {@link GoogleApiClient.ConnectionCallbacks#onConnected} to be called.
     * @param request  The location request for the updates.
     * @param listener The listener for the location updates.
     * @return a PendingResult for the call, check {@link Status#isSuccess()} to determine if it was successful.
     * @throws IllegalStateException If this method is executed in a thread that has not called Looper.prepare().
     */
    @NonNull
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    PendingResult<Status> requestLocationUpdates(GoogleApiClient client, LocationRequest request, LocationListener listener);

    /**
     * Requests location updates with a callback on the specified Looper thread.
     * <p>
     * This method is suited for the foreground use cases,more specifically for requesting locations while being connected to {@link GoogleApiClient}. For
     * background use cases, the {@link PendingIntent} version of the method is recommended, see
     * {@link #requestLocationUpdates(GoogleApiClient, LocationRequest, PendingIntent)}.
     * <p>
     * Any previous LocationRequests registered on this LocationListener will be replaced.
     * <p>
     * Callbacks for {@link LocationCallback} will be made on the specified thread, which must already be a prepared looper thread.
     *
     * @param client   An existing GoogleApiClient. It must be connected at the time of this call, which is normally achieved by calling
     *                 {@link GoogleApiClient#connect()} and waiting for {@link GoogleApiClient.ConnectionCallbacks#onConnected} to be called.
     * @param request  The location request for the updates.
     * @param callback The callback for the location updates.
     * @param looper   The Looper object whose message queue will be used to implement the callback mechanism, or null to make callbacks on the calling thread.
     * @return a PendingResult for the call, check {@link Status#isSuccess()} to determine if it was successful.
     * @throws IllegalStateException If looper is null and this method is executed in a thread that has not called Looper.prepare().
     */
    @NonNull
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    PendingResult<Status> requestLocationUpdates(GoogleApiClient client, LocationRequest request, LocationCallback callback, Looper looper);

    /**
     * Requests location updates with a callback on the specified Looper thread.
     * <p>
     * This method is suited for the foreground use cases,more specifically for requesting locations while being connected to {@link GoogleApiClient}. For
     * background use cases, the {@link PendingIntent} version of the method is recommended, see
     * {@link #requestLocationUpdates(GoogleApiClient, LocationRequest, PendingIntent)}.
     * <p>
     * Any previous LocationRequests registered on this LocationListener will be replaced.
     * <p>
     * Callbacks for LocationListener will be made on the specified thread, which must already be a prepared looper thread. For cases where the callback can
     * happen on the calling thread, the variant of this method without a {@link Looper} can be used.
     *
     * @param client   An existing GoogleApiClient. It must be connected at the time of this call, which is normally achieved by calling
     *                 {@link GoogleApiClient#connect()} and waiting for {@link GoogleApiClient.ConnectionCallbacks#onConnected} to be called.
     * @param request  The location request for the updates.
     * @param listener The listener for the location updates.
     * @param looper   The Looper object whose message queue will be used to implement the callback mechanism, or null to make callbacks on the calling thread.
     * @return a PendingResult for the call, check {@link Status#isSuccess()} to determine if it was successful.
     * @throws IllegalStateException If looper is null and this method is executed in a thread that has not called Looper.prepare().
     */
    @NonNull
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    PendingResult<Status> requestLocationUpdates(GoogleApiClient client, LocationRequest request, LocationListener listener, Looper looper);

    /**
     * Requests location updates with a callback on the specified PendingIntent.
     * <p>
     * This method is suited for the background use cases, more specifically for receiving location updates, even when the app has been killed by the system.
     * In order to do so, use a {@link PendingIntent} for a started service. For foreground use cases, the {@link LocationListener} version of the method is
     * recommended, see {@link #requestLocationUpdates(GoogleApiClient, LocationRequest, LocationListener)}.
     * <p>
     * Any previously registered requests that have the same PendingIntent (as defined by {@link PendingIntent#equals(Object)}) will be replaced by this
     * request.
     * <p>
     * Both {@link LocationResult} and {@link LocationAvailability} are sent to the given PendingIntent. You can extract data from an Intent using
     * {@link LocationResult#hasResult(Intent)}, {@link LocationResult#extractResult(Intent)}, {@link LocationAvailability#hasLocationAvailability(Intent)},
     * and {@link LocationAvailability#extractLocationAvailability(Intent)}.
     *
     * @param client         An existing GoogleApiClient. It must be connected at the time of this call, which is normally achieved by calling
     *                       {@link GoogleApiClient#connect()} and waiting for {@link GoogleApiClient.ConnectionCallbacks#onConnected} to be called.
     * @param request        The location request for the updates.
     * @param callbackIntent A pending intent to be sent for each location update.
     * @return a PendingResult for the call, check {@link Status#isSuccess()} to determine if it was successful.
     */
    @NonNull
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    PendingResult<Status> requestLocationUpdates(GoogleApiClient client, LocationRequest request, PendingIntent callbackIntent);

    /**
     * Sets the mock location to be used for the location provider. This location will be used in place of any actual locations from the underlying providers
     * (network or gps).
     * <p>
     * {@link #setMockMode(GoogleApiClient, boolean)} must be called and set to true prior to calling this method.
     * <p>
     * Care should be taken in specifying the timestamps as many applications require them to be monotonically increasing.
     *
     * @param client       An existing GoogleApiClient. It must be connected at the time of this call, which is normally achieved by calling
     *                     {@link GoogleApiClient#connect()} and waiting for {@link GoogleApiClient.ConnectionCallbacks#onConnected} to be called.
     * @param mockLocation The mock location. Must have a minimum number of fields set to be considered a valid location, as per documentation in the
     *                     {@link Location} class.
     * @return a PendingResult for the call, check {@link Status#isSuccess()} to determine if it was successful.
     * @throws SecurityException if the ACCESS_MOCK_LOCATION permission is not present or the {@link Settings.Secure#ALLOW_MOCK_LOCATION} system setting is
     *                           not enabled.
     */
    @NonNull
    PendingResult<Status> setMockLocation(GoogleApiClient client, Location mockLocation);

    /**
     * Sets whether or not the location provider is in mock mode.
     * <p>
     * The underlying providers (network and gps) will be stopped (except by direct {@link LocationManager} access), and only locations specified in
     * {@link #setMockLocation(GoogleApiClient, Location)} will be reported. This will affect all location clients connected using the
     * {@link FusedLocationProviderApi}, including geofencer clients (i.e. geofences can be triggered based on mock locations).
     * <p>
     * The client must remain connected in order for mock mode to remain active. If the client dies the system will return to its normal state.
     * <p>
     * Calls are not nested, and mock mode will be set directly regardless of previous calls.
     *
     * @param client     An existing GoogleApiClient. It must be connected at the time of this call, which is normally achieved by calling
     *                   {@link GoogleApiClient#connect()} and waiting for {@link GoogleApiClient.ConnectionCallbacks#onConnected} to be called.
     * @param isMockMode If true the location provider will be set to mock mode. If false it will be returned to its normal state.
     * @return a PendingResult for the call, check {@link Status#isSuccess()} to determine if it was successful.
     * @throws SecurityException if the ACCESS_MOCK_LOCATION permission is not present or the {@link Settings.Secure#ALLOW_MOCK_LOCATION} system setting is
     *                           not enabled.
     */
    @NonNull
    PendingResult<Status> setMockMode(GoogleApiClient client, boolean isMockMode);
}
