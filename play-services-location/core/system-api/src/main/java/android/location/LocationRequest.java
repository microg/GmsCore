/*
 * SPDX-FileCopyrightText: 2012, The Android Open Source Project
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package android.location;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.os.WorkSource;

/**
 * A data object that contains quality of service parameters for requests
 * to the {@link LocationManager}.
 * <p/>
 * <p>LocationRequest objects are used to request a quality of service
 * for location updates from the Location Manager.
 * <p/>
 * <p>For example, if your application wants high accuracy location
 * it should create a location request with {@link #setQuality} set to
 * {@link #ACCURACY_FINE} or {@link #POWER_HIGH}, and it should set
 * {@link #setInterval} to less than one second. This would be
 * appropriate for mapping applications that are showing your location
 * in real-time.
 * <p/>
 * <p>At the other extreme, if you want negligible power
 * impact, but to still receive location updates when available, then use
 * {@link #setQuality} with {@link #POWER_NONE}. With this request your
 * application will not trigger (and therefore will not receive any
 * power blame) any location updates, but will receive locations
 * triggered by other applications. This would be appropriate for
 * applications that have no firm requirement for location, but can
 * take advantage when available.
 * <p/>
 * <p>In between these two extremes is a very common use-case, where
 * applications definitely want to receive
 * updates at a specified interval, and can receive them faster when
 * available, but still want a low power impact. These applications
 * should consider {@link #POWER_LOW} combined with a faster
 * {@link #setFastestInterval} (such as 1 minute) and a slower
 * {@link #setInterval} (such as 60 minutes). They will only be assigned
 * power blame for the interval set by {@link #setInterval}, but can
 * still receive locations triggered by other applications at a rate up
 * to {@link #setFastestInterval}. This style of request is appropriate for
 * many location aware applications, including background usage. Do be
 * careful to also throttle {@link #setFastestInterval} if you perform
 * heavy-weight work after receiving an update - such as using the network.
 * <p/>
 * <p>Activities should strongly consider removing all location
 * request when entering the background
 * (for example at {@link android.app.Activity#onPause}), or
 * at least swap the request to a larger interval and lower quality.
 * Future version of the location manager may automatically perform background
 * throttling on behalf of applications.
 * <p/>
 * <p>Applications cannot specify the exact location sources that are
 * used by Android's <em>Fusion Engine</em>. In fact, the system
 * may have multiple location sources (providers) running and may
 * fuse the results from several sources into a single Location object.
 * <p/>
 * <p>Location requests from applications with
 * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} and not
 * {@link android.Manifest.permission#ACCESS_FINE_LOCATION} will
 * be automatically throttled to a slower interval, and the location
 * object will be obfuscated to only show a coarse level of accuracy.
 * <p/>
 * <p>All location requests are considered hints, and you may receive
 * locations that are more accurate, less accurate, and slower
 * than requested.
 *
 * @hide
 */
@SuppressWarnings("WrongConstant")
public final class LocationRequest implements Parcelable {
    /**
     * Used with {@link #setQuality} to request the most accurate locations available.
     * <p/>
     * <p>This may be up to 1 meter accuracy, although this is implementation dependent.
     */
    public static final int ACCURACY_FINE = 100;

    /**
     * Used with {@link #setQuality} to request "block" level accuracy.
     * <p/>
     * <p>Block level accuracy is considered to be about 100 meter accuracy,
     * although this is implementation dependent. Using a coarse accuracy
     * such as this often consumes less power.
     */
    public static final int ACCURACY_BLOCK = 102;

    /**
     * Used with {@link #setQuality} to request "city" level accuracy.
     * <p/>
     * <p>City level accuracy is considered to be about 10km accuracy,
     * although this is implementation dependent. Using a coarse accuracy
     * such as this often consumes less power.
     */
    public static final int ACCURACY_CITY = 104;

    /**
     * Used with {@link #setQuality} to require no direct power impact (passive locations).
     * <p/>
     * <p>This location request will not trigger any active location requests,
     * but will receive locations triggered by other applications. Your application
     * will not receive any direct power blame for location work.
     */
    public static final int POWER_NONE = 200;

    /**
     * Used with {@link #setQuality} to request low power impact.
     * <p/>
     * <p>This location request will avoid high power location work where
     * possible.
     */
    public static final int POWER_LOW = 201;

    /**
     * Used with {@link #setQuality} to allow high power consumption for location.
     * <p/>
     * <p>This location request will allow high power location work.
     */
    public static final int POWER_HIGH = 203;

    /**
     * Create a location request with default parameters.
     * <p/>
     * <p>Default parameters are for a low power, slowly updated location.
     * It can then be adjusted as required by the applications before passing
     * to the {@link LocationManager}
     *
     * @return a new location request
     */
    public static LocationRequest create() {
        return null;
    }

    /**
     * @hide
     */
    public static LocationRequest createFromDeprecatedProvider(String provider, long minTime,
            float minDistance, boolean singleShot) {
        return null;
    }

    /**
     * @hide
     */
    public static LocationRequest createFromDeprecatedCriteria(Criteria criteria, long minTime,
            float minDistance, boolean singleShot) {
        return null;
    }

    /**
     * @hide
     */
    public LocationRequest() {
    }

    /**
     * @hide
     */
    public LocationRequest(LocationRequest src) {
    }

    /**
     * Set the quality of the request.
     * <p/>
     * <p>Use with a accuracy constant such as {@link #ACCURACY_FINE}, or a power
     * constant such as {@link #POWER_LOW}. You cannot request both and accuracy and
     * power, only one or the other can be specified. The system will then
     * maximize accuracy or minimize power as appropriate.
     * <p/>
     * <p>The quality of the request is a strong hint to the system for which
     * location sources to use. For example, {@link #ACCURACY_FINE} is more likely
     * to use GPS, and {@link #POWER_LOW} is more likely to use WIFI & Cell tower
     * positioning, but it also depends on many other factors (such as which sources
     * are available) and is implementation dependent.
     * <p/>
     * <p>{@link #setQuality} and {@link #setInterval} are the most important parameters
     * on a location request.
     *
     * @param quality an accuracy or power constant
     * @return the same object, so that setters can be chained
     * @throws InvalidArgumentException if the quality constant is not valid
     */
    public LocationRequest setQuality(int quality) {
        return null;
    }

    /**
     * Get the quality of the request.
     *
     * @return an accuracy or power constant
     */
    public int getQuality() {
        return 0;
    }

    /**
     * Set the desired interval for active location updates, in milliseconds.
     * <p/>
     * <p>The location manager will actively try to obtain location updates
     * for your application at this interval, so it has a
     * direct influence on the amount of power used by your application.
     * Choose your interval wisely.
     * <p/>
     * <p>This interval is inexact. You may not receive updates at all (if
     * no location sources are available), or you may receive them
     * slower than requested. You may also receive them faster than
     * requested (if other applications are requesting location at a
     * faster interval). The fastest rate that that you will receive
     * updates can be controlled with {@link #setFastestInterval}.
     * <p/>
     * <p>Applications with only the coarse location permission may have their
     * interval silently throttled.
     * <p/>
     * <p>An interval of 0 is allowed, but not recommended, since
     * location updates may be extremely fast on future implementations.
     * <p/>
     * <p>{@link #setQuality} and {@link #setInterval} are the most important parameters
     * on a location request.
     *
     * @param millis desired interval in millisecond, inexact
     * @return the same object, so that setters can be chained
     * @throws InvalidArgumentException if the interval is less than zero
     */
    public LocationRequest setInterval(long millis) {
        return null;
    }

    /**
     * Get the desired interval of this request, in milliseconds.
     *
     * @return desired interval in milliseconds, inexact
     */
    public long getInterval() {
        return 0;
    }

    /**
     * Explicitly set the fastest interval for location updates, in
     * milliseconds.
     * <p/>
     * <p>This controls the fastest rate at which your application will
     * receive location updates, which might be faster than
     * {@link #setInterval} in some situations (for example, if other
     * applications are triggering location updates).
     * <p/>
     * <p>This allows your application to passively acquire locations
     * at a rate faster than it actively acquires locations, saving power.
     * <p/>
     * <p>Unlike {@link #setInterval}, this parameter is exact. Your
     * application will never receive updates faster than this value.
     * <p/>
     * <p>If you don't call this method, a fastest interval
     * will be selected for you. It will be a value faster than your
     * active interval ({@link #setInterval}).
     * <p/>
     * <p>An interval of 0 is allowed, but not recommended, since
     * location updates may be extremely fast on future implementations.
     * <p/>
     * <p>If {@link #setFastestInterval} is set slower than {@link #setInterval},
     * then your effective fastest interval is {@link #setInterval}.
     *
     * @param millis fastest interval for updates in milliseconds, exact
     * @return the same object, so that setters can be chained
     * @throws InvalidArgumentException if the interval is less than zero
     */
    public LocationRequest setFastestInterval(long millis) {
        return null;
    }

    /**
     * Get the fastest interval of this request, in milliseconds.
     * <p/>
     * <p>The system will never provide location updates faster
     * than the minimum of {@link #getFastestInterval} and
     * {@link #getInterval}.
     *
     * @return fastest interval in milliseconds, exact
     */
    public long getFastestInterval() {
        return 0;
    }

    /**
     * Set the duration of this request, in milliseconds.
     * <p/>
     * <p>The duration begins immediately (and not when the request
     * is passed to the location manager), so call this method again
     * if the request is re-used at a later time.
     * <p/>
     * <p>The location manager will automatically stop updates after
     * the request expires.
     * <p/>
     * <p>The duration includes suspend time. Values less than 0
     * are allowed, but indicate that the request has already expired.
     *
     * @param millis duration of request in milliseconds
     * @return the same object, so that setters can be chained
     */
    public LocationRequest setExpireIn(long millis) {
        return null;
    }

    /**
     * Set the request expiration time, in millisecond since boot.
     * <p/>
     * <p>This expiration time uses the same time base as {@link SystemClock#elapsedRealtime}.
     * <p/>
     * <p>The location manager will automatically stop updates after
     * the request expires.
     * <p/>
     * <p>The duration includes suspend time. Values before {@link SystemClock#elapsedRealtime}
     * are allowed,  but indicate that the request has already expired.
     *
     * @param millis expiration time of request, in milliseconds since boot including suspend
     * @return the same object, so that setters can be chained
     */
    public LocationRequest setExpireAt(long millis) {
        return null;
    }

    /**
     * Get the request expiration time, in milliseconds since boot.
     * <p/>
     * <p>This value can be compared to {@link SystemClock#elapsedRealtime} to determine
     * the time until expiration.
     *
     * @return expiration time of request, in milliseconds since boot including suspend
     */
    public long getExpireAt() {
        return 0;
    }

    /**
     * Set the number of location updates.
     * <p/>
     * <p>By default locations are continuously updated until the request is explicitly
     * removed, however you can optionally request a set number of updates.
     * For example, if your application only needs a single fresh location,
     * then call this method with a value of 1 before passing the request
     * to the location manager.
     *
     * @param numUpdates the number of location updates requested
     * @return the same object, so that setters can be chained
     * @throws InvalidArgumentException if numUpdates is 0 or less
     */
    public LocationRequest setNumUpdates(int numUpdates) {
        return null;
    }

    /**
     * Get the number of updates requested.
     * <p/>
     * <p>By default this is {@link Integer#MAX_VALUE}, which indicates that
     * locations are updated until the request is explicitly removed.
     *
     * @return number of updates
     */
    public int getNumUpdates() {
        return 0;
    }

    /**
     * @hide
     */
    public void decrementNumUpdates() {
    }

    /**
     * @hide
     */
    public LocationRequest setProvider(String provider) {
        return null;
    }

    /**
     * @hide
     */
    public String getProvider() {
        return null;
    }

    /**
     * @hide
     */
    public LocationRequest setSmallestDisplacement(float meters) {
        return null;
    }

    /**
     * @hide
     */
    public float getSmallestDisplacement() {
        return 0;
    }

    /**
     * Sets the WorkSource to use for power blaming of this location request.
     * <p/>
     * <p>No permissions are required to make this call, however the LocationManager
     * will throw a SecurityException when requesting location updates if the caller
     * doesn't have the {@link android.Manifest.permission#UPDATE_DEVICE_STATS} permission.
     *
     * @param workSource WorkSource defining power blame for this location request.
     * @hide
     */
    public void setWorkSource(WorkSource workSource) {
    }

    /**
     * @hide
     */
    public WorkSource getWorkSource() {
        return null;
    }

    /**
     * Sets whether or not this location request should be hidden from AppOps.
     * <p/>
     * <p>Hiding a location request from AppOps will remove user visibility in the UI as to this
     * request's existence.  It does not affect power blaming in the Battery page.
     * <p/>
     * <p>No permissions are required to make this call, however the LocationManager
     * will throw a SecurityException when requesting location updates if the caller
     * doesn't have the {@link android.Manifest.permission#UPDATE_APP_OPS_STATS} permission.
     *
     * @param hideFromAppOps If true AppOps won't keep track of this location request.
     * @hide
     * @see android.app.AppOpsManager
     */
    public void setHideFromAppOps(boolean hideFromAppOps) {
    }

    /**
     * @hide
     */
    public boolean getHideFromAppOps() {
        return false;
    }

    public static final Parcelable.Creator<LocationRequest> CREATOR =
            new Parcelable.Creator<LocationRequest>() {
                @Override
                public LocationRequest createFromParcel(Parcel in) {
                    return null;
                }

                @Override
                public LocationRequest[] newArray(int size) {
                    return null;
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
    }

    /**
     * @hide
     */
    public static String qualityToString(int quality) {
        return null;
    }
}
