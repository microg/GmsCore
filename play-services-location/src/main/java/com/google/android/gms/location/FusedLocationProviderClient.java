/*
 * SPDX-FileCopyrightText: 2020 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Looper;

import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.Task;

import org.microg.gms.common.PublicApi;

import java.util.concurrent.Executor;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**
 * The main entry point for interacting with the Fused Location Provider (FLP). In order to obtain an instance of this
 * class, see {@link LocationServices}.
 * <p>
 * In order to use most location APIs, clients are required to hold either the
 * {@link Manifest.permission#ACCESS_COARSE_LOCATION} permission or the {@link Manifest.permission#ACCESS_FINE_LOCATION}.
 * Clients holding only the coarse permission will receive locations that have been obfuscated to hide the device's
 * exact location, and only reveal the approximate area of the device. In addition, clients with only the coarse
 * permission will receive location updates at a throttled rate. Applications which do not require an exact location to
 * work (such as a weather app for instance) are encouraged to use only the coarse permission. From Android 12 onwards,
 * the user may force any app to use coarse location, so apps should test carefully their behavior with only the coarse
 * location permission to ensure everything works as expected.
 * <p>
 * If clients have only the coarse or fine location permission, they will not receive locations while they are in the
 * background. Whether an app is in the background or foreground is normally determined by whether it is currently
 * showing any UI to the user. Apps may also use a foreground location service to maintain their foreground status when
 * they would normally be in the background.
 * <p>
 * If clients also hold the {@link Manifest.permission#ACCESS_BACKGROUND_LOCATION} permission, they may receive
 * locations while in the background even if the above conditions are not met.
 * <p>
 * There are several types of use cases for location. One of the most common is simply obtaining a single location in
 * order to determine where the device is now, and continue from there. The
 * {@link #getCurrentLocation(CurrentLocationRequest, CancellationToken)} API is designed with exactly this use case in
 * mind. On the other hand, if repeated location updates are required, such as when tracking the user's location over
 * time, {@link #requestLocationUpdates(LocationRequest, Executor, LocationListener)} or one of its variants is better
 * suited. Clients are encourage to familiarize themselves with the full range of APIs available in this class to
 * understand which is best suited for their needs.
 */
@PublicApi
public abstract class FusedLocationProviderClient extends GoogleApi<Api.ApiOptions.NoOptions> {
    @PublicApi(exclude = true)
    protected FusedLocationProviderClient(Context context) {
        super(context, LocationServices.API);
    }

    /**
     * Key used for the Bundle extra in {@link Location} object indicating whether this is a mock location.
     *
     * @deprecated Use {@link Location#isMock()} on Android S and above, otherwise use {@link LocationCompat#isMock()}
     * from the compat libraries instead.
     */
    @Deprecated
    @NonNull
    public static String KEY_MOCK_LOCATION = "mockLocation";

    /**
     * Key used for the Bundle extra in {@link Location} object holding a float indicating the estimated vertical
     * accuracy of the location, in meters.
     *
     * @deprecated Use {@link Location#getVerticalAccuracyMeters()} on Android O and above, otherwise use
     * {@link LocationCompat#getVerticalAccuracyMeters()} from the compat libraries instead.
     */
    @Deprecated
    @NonNull
    public static String KEY_VERTICAL_ACCURACY = "verticalAccuracy";

    /**
     * Flushes any locations currently being batched and sends them to all registered {@link LocationListener}s,
     * {@link LocationCallback}s, and {@link PendingIntent}s. This call is only useful when batching is specified using
     * {@link LocationRequest#setMaxWaitTime(long)}, otherwise locations are already delivered immediately when available.
     * <p>
     * When the returned {@link Task} is complete, then you can assume that any pending batched locations have already been
     * delivered.
     */
    @NonNull
    public abstract Task<Void> flushLocations();

    /**
     * Returns a single location fix representing the best estimate of the current location of the device. This may return a cached location if a recent enough
     * location fix exists, or may compute a fresh location. If unable to retrieve a current location fix, null will be returned.
     * <p>
     * Clients may supply an optional {@link CancellationToken} which may be used to cancel the request.
     *
     * @param priority          {@link Priority} used to obtain location
     * @param cancellationToken optional {@link CancellationToken} to cancel the request
     */
    @NonNull
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public abstract Task<Location> getCurrentLocation(int priority, CancellationToken cancellationToken);

    /**
     * Returns a single location fix representing the best estimate of the current location of the device. This may return a historical location if a recent
     * enough location fix exists, or may compute a fresh location. If unable to retrieve a current location fix, null will be returned.
     * <p>
     * Clients may supply an optional {@link CancellationToken} which may be used to cancel the request.
     *
     * @param request           {@link CurrentLocationRequest} with parameters detailing how to obtain the current location
     * @param cancellationToken optional {@link CancellationToken} to cancel the request
     */
    @NonNull
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public abstract Task<Location> getCurrentLocation(CurrentLocationRequest request, CancellationToken cancellationToken);

    /**
     * Returns the most recent historical location currently available according to the given request. Will return null if no matching historical location is
     * available.
     */
    @NonNull
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public abstract Task<Location> getLastLocation(LastLocationRequest request);

    /**
     * Returns the most recent historical location currently available. Will return null if no historical location is available. The historical location may
     * be of an arbitrary age, so clients should check how old the location is to see if it suits their purposes.
     */
    @NonNull
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public abstract Task<Location> getLastLocation();

    /**
     * Returns the estimated availability of location data. If {@link LocationAvailability#isLocationAvailable()} returns true then it is likely (but not
     * guaranteed) that Fused Location Provider APIs will be able to derive and return fresh location updates. If
     * {@link LocationAvailability#isLocationAvailable()} returns false, then it is likely (but not guaranteed) that Fused Location Provider APIs will be
     * unable to derive and return fresh location updates, though there may be historical locations available.
     */
    @NonNull
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public abstract Task<LocationAvailability> getLocationAvailability();

    /**
     * Removes all location updates for the given listener.
     */
    @NonNull
    public abstract Task<Void> removeLocationUpdates(LocationListener listener);

    /**
     * Removes all location updates for the given callback.
     */
    @NonNull
    public abstract Task<Void> removeLocationUpdates(LocationCallback callback);

    /**
     * Removes all location updates for the given pending intent.
     */
    @NonNull
    public abstract Task<Void> removeLocationUpdates(PendingIntent pendingIntent);

    /**
     * Requests location updates with the given request and results delivered to the given listener on the specified {@link Looper}. A
     * previous request for location updates for the same listener will be replaced by this request. If the location request has a
     * priority higher than {@link Priority#PRIORITY_PASSIVE}, a wakelock may be held on the client's behalf while delivering locations.
     * <p>
     * Use {@link #removeLocationUpdates(LocationListener)} to stop location updates once no longer needed.
     * <p>
     * Depending on the arguments passed in through the {@link LocationRequest}, locations from the past may be delivered when
     * the listener is first registered. Clients should ensure they are checking location timestamps appropriately if necessary.
     * <p>
     * If the given Looper is null, the Looper associated with the calling thread will be used instead.
     *
     * @throws IllegalStateException if {@code looper} is null and the calling thread has not called {@link Looper#prepare()}
     */
    @NonNull
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public abstract Task<Void> requestLocationUpdates(LocationRequest request, LocationListener listener, Looper looper);

    /**
     * Requests location updates with the given request and results delivered to the given callback on the specified {@link Executor}.
     * A previous request for location updates for the same callback will be replaced by this request. If the location request has
     * a priority higher than {@link Priority#PRIORITY_PASSIVE}, a wakelock may be held on the client's behalf while delivering
     * locations. A wakelock will not be held while delivering availability updates.
     * <p>
     * Use {@link #removeLocationUpdates(LocationCallback)} to stop location updates once no longer needed.
     * <p>
     * Depending on the arguments passed in through the {@link LocationRequest}, locations from the past may be delivered when
     * the callback is first registered. Clients should ensure they are checking location timestamps appropriately if necessary.
     */
    @NonNull
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public abstract Task<Void> requestLocationUpdates(LocationRequest request, Executor executor, LocationCallback callback);

    /**
     * Requests location updates with the given request and results delivered to the given listener on the specified {@link Executor}.
     *
     * @see #requestLocationUpdates(LocationRequest, LocationListener, Looper)
     */
    @NonNull
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public abstract Task<Void> requestLocationUpdates(LocationRequest request, Executor executor, LocationListener listener);

    /**
     * Requests location updates with the given request and results delivered to the given callback on the specified
     * {@link Looper}. A previous request for location updates for the same callback will be replaced by this request.
     * If the location request has a priority higher than {@link Priority#PRIORITY_PASSIVE}, a wakelock may be held on
     * the client's behalf while delivering locations. A wakelock will not be held while delivering availability
     * updates.
     * <p>
     * Use {@link #removeLocationUpdates(LocationCallback)} to stop location updates once no longer needed.
     * <p>
     * Depending on the arguments passed in through the {@link LocationRequest}, locations from the past may be
     * delivered when the callback is first registered. Clients should ensure they are checking location timestamps
     * appropriately if necessary.
     * <p>
     * If the given {@link Looper} is null, the Looper associated with the calling thread will be used instead.
     *
     * @throws IllegalStateException if looper is null and the calling thread has not called {@link Looper#prepare()}
     */
    @NonNull
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public abstract Task<Void> requestLocationUpdates(LocationRequest request, LocationCallback callback, Looper looper);

    /**
     * Requests location updates with the given request and results delivered via the specified {@link PendingIntent}. A previous
     * request for location updates for the same pending intent will be replaced by this request. If the location request has a
     * priority higher than {@link Priority#PRIORITY_PASSIVE}, a wakelock may be held on the client's behalf while delivering
     * locations. A wakelock will not be held while delivering availability updates.
     * <p>
     * Location updates should be extracted from the received {@link Intent} via {@link LocationResult#hasResult(Intent)} and
     * {@link LocationResult#extractResult(Intent)}. Availability updates should be extracted from the {@link Intent} via
     * {@link LocationAvailability#hasLocationAvailability(Intent)} and
     * {@link LocationAvailability#extractLocationAvailability(Intent)}.
     * <p>
     * This method is suited for receiving location updates in the background, even when the receiving app may have been
     * killed by the system. Using a {@link PendingIntent} allows the target component to be started and receive location updates.
     * For foreground use cases prefer to listen for location updates via a listener or callback instead of a pending intent.
     * <p>
     * {@link PendingIntent} location requests are automatically removed when the client application is reset (for example, when the
     * client application is upgraded, restarted, removed, or force-quit), or if the pending intent is canceled.
     * <p>
     * Use {@link #removeLocationUpdates(PendingIntent)} to stop location updates once no longer needed.
     * <p>
     * Depending on the arguments passed in through the {@link LocationRequest}, locations from the past may be delivered when
     * the callback is first registered. Clients should ensure they are checking location timestamps appropriately if necessary.
     */
    @NonNull
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public abstract Task<Void> requestLocationUpdates(LocationRequest request, PendingIntent pendingIntent);

    /**
     * Sets the mock location of the Fused Location Provider.
     * <p>
     * Delivers the given location to the FLP as if it was coming from an underlying location source. Normal FLP logic around receiving and delivering location
     * will generally apply. For this reason the timestamps of the location should be set appropriately, as the FLP may expect monotonically increasing
     * timestamps. When this location is reported to FLP clients it will be marked as a mock location (see {@link Location#isMock()} or
     * {@link LocationCompat#isMock()} from the compat libraries).
     * <p>
     * This API can only be successfully used while the FLP is in mock mode. Clients must fulfill the same security requirements as for
     * {@link #setMockMode(boolean)} as well.
     *
     * @param location valid location to set as the next FLP location
     * @throws SecurityException if security requirements are not met
     */
    public abstract Task<Void> setMockLocation(Location location);

    /**
     * Sets whether or not the Fused Location Provider is in mock mode.
     * <p>
     * Entering mock mode clears the FLP's cached locations, and ensures that the FLP will only report locations set through {@link #setMockLocation(Location)}.
     * Exiting mock mode will clear any mock locations set from the FLP's cache as well. Mock mode affects all location clients using the FLP, including
     * location clients in other processes and derivative APIs such as geofencing and so forth. Because this affects all FLP usage, clients should always
     * ensure they properly set the mock mode to false when finished.
     * <p>
     * Successfully using this API on devices running Android M+ requires the client to request the {@code android.permission.ACCESS_MOCK_LOCATION} permission
     * and to be selected as the mock location app within the device developer settings. Using this API on pre-M devices requires the
     * {@link Settings.Secure#ALLOW_MOCK_LOCATION} setting to be enabled.
     *
     * @param mockMode the mock mode state to set for the Fused Location Provider APIs
     * @throws SecurityException if security requirements are not met
     */
    public abstract Task<Void> setMockMode(boolean mockMode);
}
