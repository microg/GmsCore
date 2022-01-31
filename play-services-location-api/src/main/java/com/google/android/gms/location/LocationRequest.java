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

import android.os.SystemClock;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;

/**
 * A data object that contains quality of service parameters for requests to the
 * FusedLocationProviderApi.
 * <p/>
 * LocationRequest objects are used to request a quality of service for location updates from the
 * FusedLocationProviderApi.
 * <p/>
 * For example, if your application wants high accuracy location it should create a location
 * request with {@link #setPriority(int)} set to {@link #PRIORITY_HIGH_ACCURACY} and
 * {@link #setInterval(long)} to 5 seconds. This would be appropriate for mapping applications that
 * are showing your location in real-time.
 * <p/>
 * At the other extreme, if you want negligible power impact, but to still receive location updates
 * when available, then create a location request with {@link #setPriority(int)} set to
 * {@link #PRIORITY_NO_POWER}. With this request your application will not trigger (and therefore
 * will not receive any power blame) any location updates, but will receive locations triggered by
 * other applications. This would be appropriate for applications that have no firm requirement for
 * location, but can take advantage when available.
 * <p/>
 * In between these two extremes is a very common use-case, where applications definitely want to
 * receive updates at a specified interval, and can receive them faster when available, but still
 * want a low power impact. These applications should consider
 * {@link #PRIORITY_BALANCED_POWER_ACCURACY} combined with a faster
 * {@link #setFastestInterval(long)} (such as 1 minute) and a slower {@link #setInterval(long)}
 * (such as 60 minutes). They will only be assigned power blame for the interval set by
 * {@link #setInterval(long)}, but can still receive locations triggered by other applications at a
 * rate up to {@link #setFastestInterval(long)}. This style of request is appropriate for many
 * location aware applications, including background usage. Do be careful to also throttle
 * {@link #setFastestInterval(long)} if you perform heavy-weight work after receiving an update -
 * such as using the network.
 * <p/>
 * Activities should strongly consider removing all location request when entering the background
 * (for example at {@link android.app.Activity#onPause()}), or at least swap the request to a
 * larger interval and lower quality.
 * <p/>
 * Applications cannot specify the exact location sources, such as GPS, that are used by the
 * LocationClient. In fact, the system may have multiple location sources (providers) running and
 * may fuse the results from several sources into a single Location object.
 * <p/>
 * Location requests from applications with
 * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} and not
 * {@link android.Manifest.permission#ACCESS_FINE_LOCATION} will be automatically throttled to a
 * slower interval, and the location object will be obfuscated to only show a coarse level of
 * accuracy.
 * <p/>
 * All location requests are considered hints, and you may receive locations that are more/less
 * accurate, and faster/slower than requested.
 */
public class LocationRequest extends AutoSafeParcelable {

    /**
     * Used with {@link #setPriority(int)} to request "block" level accuracy.
     * <p/>
     * Block level accuracy is considered to be about 100 meter accuracy. Using a coarse accuracy
     * such as this often consumes less power.
     */
    public static final int PRIORITY_BALANCED_POWER_ACCURACY = 102;
    /**
     * Used with {@link #setPriority(int)} to request the most accurate locations available.
     * <p/>
     * This will return the finest location available.
     */
    public static final int PRIORITY_HIGH_ACCURACY = 100;
    /**
     * Used with {@link #setPriority(int)} to request "city" level accuracy.
     * <p/>
     * City level accuracy is considered to be about 10km accuracy. Using a coarse accuracy such as
     * this often consumes less power.
     */
    public static final int PRIORITY_LOW_POWER = 104;
    /**
     * Used with {@link #setPriority(int)} to request the best accuracy possible with zero
     * additional power consumption.
     * <p/>
     * No locations will be returned unless a different client has requested location updates in
     * which case this request will act as a passive listener to those locations.
     */
    public static final int PRIORITY_NO_POWER = 105;

    @Field(1000)
    private final int versionCode = 1;
    @Field(1)
    private int priority;
    @Field(2)
    private long interval;
    @Field(3)
    private long fastestInterval;
    @Field(4)
    private boolean explicitFastestInterval;
    @Field(5)
    private long expirationTime;
    @Field(6)
    private int numUpdates;
    @Field(7)
    private float smallestDisplacement;
    @Field(8)
    private long maxWaitTime;
    @Field(9)
    private boolean waitForAccurateLocation;

    public LocationRequest() {
        this.priority = PRIORITY_BALANCED_POWER_ACCURACY;
        this.interval = 3600000;
        this.fastestInterval = 600000;
        this.explicitFastestInterval = false;
        this.expirationTime = Long.MAX_VALUE;
        this.numUpdates = Integer.MAX_VALUE;
        this.smallestDisplacement = 0;
        this.maxWaitTime = 0;
    }

    /**
     * Create a location request with default parameters.
     * <p/>
     * Default parameters are for a block accuracy, slowly updated location. It can then be
     * adjusted as required by the applications before passing to the FusedLocationProviderApi.
     *
     * @return a new location request
     */
    public static LocationRequest create() {
        return new LocationRequest();
    }

    /**
     * Get the request expiration time, in milliseconds since boot.
     * <p/>
     * This value can be compared to {@link SystemClock#elapsedRealtime()} to determine
     * the time until expiration.
     *
     * @return expiration time of request, in milliseconds since boot including suspend
     */
    public long getExpirationTime() {
        return expirationTime;
    }

    /**
     * Get the fastest interval of this request, in milliseconds.
     * <p/>
     * The system will never provide location updates faster than the minimum of
     * {@link #getFastestInterval()} and {@link #getInterval()}.
     *
     * @return fastest interval in milliseconds, exact
     */
    public long getFastestInterval() {
        return fastestInterval;
    }

    /**
     * Get the desired interval of this request, in milliseconds.
     *
     * @return desired interval in milliseconds, inexact
     */
    public long getInterval() {
        return interval;
    }

    /**
     * Gets the maximum wait time in milliseconds for location updates. If the wait time is smaller than the interval
     * requested with {@link #setInterval(long)}, then the interval will be used instead.
     *
     * @return maximum wait time in milliseconds, inexact
     * @see #setMaxWaitTime(long)
     */
    public long getMaxWaitTime() {
        return maxWaitTime;
    }

    /**
     * Get the number of updates requested.
     * <p/>
     * By default this is {@link java.lang.Integer#MAX_VALUE}, which indicates that locations are
     * updated until the request is explicitly removed.
     *
     * @return number of updates
     */
    public int getNumUpdates() {
        return numUpdates;
    }

    /**
     * Get the quality of the request.
     *
     * @return an accuracy constant
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Get the minimum displacement between location updates in meters
     * <p/>
     * By default this is 0.
     *
     * @return minimum displacement between location updates in meters
     */
    public float getSmallestDisplacement() {
        return smallestDisplacement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        LocationRequest that = (LocationRequest) o;

        if (expirationTime != that.expirationTime)
            return false;
        if (explicitFastestInterval != that.explicitFastestInterval)
            return false;
        if (fastestInterval != that.fastestInterval)
            return false;
        if (interval != that.interval)
            return false;
        if (maxWaitTime != that.maxWaitTime)
            return false;
        if (numUpdates != that.numUpdates)
            return false;
        if (priority != that.priority)
            return false;
        return Float.compare(that.smallestDisplacement, smallestDisplacement) == 0;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(
                new Object[]{priority, interval, fastestInterval, explicitFastestInterval,
                        explicitFastestInterval, numUpdates, smallestDisplacement, maxWaitTime
                });
    }

    /**
     * Returns whether or not the fastest interval was explicitly specified for the location request.
     *
     * @return True if the fastest interval was explicitly set for the location request; false otherwise
     */
    public boolean isFastestIntervalExplicitlySet() {
        return explicitFastestInterval;
    }

    /**
     * Returns whether the location services will wait a few seconds initially for accurate locations, if accurate
     * locations cannot be computed on the device for {@link #PRIORITY_HIGH_ACCURACY} requests.
     */
    public boolean isWaitForAccurateLocation() {
        return waitForAccurateLocation;
    }

    /**
     * Set the duration of this request, in milliseconds.
     * <p/>
     * The duration begins immediately (and not when the request is passed to the location client),
     * so call this method again if the request is re-used at a later time.
     * <p/>
     * The location client will automatically stop updates after the request expires.
     * <p/>
     * The duration includes suspend time. Values less than 0 are allowed, but indicate that the
     * request has already expired.
     *
     * @param millis duration of request in milliseconds
     * @return the same object, so that setters can be chained
     */
    public LocationRequest setExpirationDuration(long millis) {
        expirationTime = SystemClock.elapsedRealtime() + millis;
        return this;
    }

    /**
     * Set the request expiration time, in millisecond since boot.
     * <p/>
     * This expiration time uses the same time base as {@link SystemClock#elapsedRealtime()}.
     * <p/>
     * The location client will automatically stop updates after the request expires.
     * <p/>
     * The duration includes suspend time. Values before {@link SystemClock#elapsedRealtime()} are
     * allowed, but indicate that the request has already expired.
     *
     * @param millis expiration time of request, in milliseconds since boot including suspend
     * @return the same object, so that setters can be chained
     */
    public LocationRequest setExpirationTime(long millis) {
        expirationTime = millis;
        return this;
    }

    /**
     * Explicitly set the fastest interval for location updates, in milliseconds.
     * <p/>
     * This controls the fastest rate at which your application will receive location updates,
     * which might be faster than {@link #setInterval(long)} in some situations (for example, if
     * other applications are triggering location updates).
     * <p/>
     * This allows your application to passively acquire locations at a rate faster than it
     * actively acquires locations, saving power.
     * <p/>
     * Unlike {@link #setInterval(long)}, this parameter is exact. Your application will never
     * receive updates faster than this value.
     * <p/>
     * If you don't call this method, a fastest interval will be selected for you. It will be a
     * value faster than your active interval ({@link #setInterval(long)}).
     * <p/>
     * An interval of 0 is allowed, but not recommended, since location updates may be extremely
     * fast on future implementations.
     * <p/>
     * If {@link #setFastestInterval(long)} is set slower than {@link #setInterval(long)}, then
     * your effective fastest interval is {@link #setInterval(long)}.
     *
     * @param millis fastest interval for updates in milliseconds, exact
     * @return the same object, so that setters can be chained
     * @throws IllegalArgumentException if the interval is less than zero
     */
    public LocationRequest setFastestInterval(long millis) throws IllegalArgumentException {
        if (millis < 0)
            throw new IllegalArgumentException("interval must not be negative");
        fastestInterval = millis;
        explicitFastestInterval = true;
        return this;
    }

    /**
     * Set the desired interval for active location updates, in milliseconds.
     * <p/>
     * The location client will actively try to obtain location updates for your application at
     * this interval, so it has a direct influence on the amount of power used by your application.
     * Choose your interval wisely.
     * <p/>
     * This interval is inexact. You may not receive updates at all (if no location sources are
     * available), or you may receive them slower than requested. You may also receive them faster
     * than requested (if other applications are requesting location at a faster interval). The
     * fastest rate that that you will receive updates can be controlled with
     * {@link #setFastestInterval(long)}. By default this fastest rate is 6x the interval frequency.
     * <p/>
     * Applications with only the coarse location permission may have their interval silently
     * throttled.
     * <p/>
     * An interval of 0 is allowed, but not recommended, since location updates may be extremely
     * fast on future implementations.
     * <p/>
     * {@link #setPriority(int)} and {@link #setInterval(long)} are the most important parameters
     * on a location request.
     *
     * @param millis desired interval in millisecond, inexact
     * @return the same object, so that setters can be chained
     * @throws IllegalArgumentException if the interval is less than zero
     */
    public LocationRequest setInterval(long millis) throws IllegalArgumentException {
        if (millis < 0)
            throw new IllegalArgumentException("interval must not be negative");
        interval = millis;
        return this;
    }

    /**
     * Sets the maximum wait time in milliseconds for location updates.
     * <p>
     * If you pass a value at least 2x larger than the interval specified with {@link #setInterval(long)}, then
     * location delivery may be delayed and multiple locations can be delivered at once. Locations are determined at
     * the {@link #setInterval(long)} rate, but can be delivered in batch after the interval you set in this method.
     * This can consume less battery and give more accurate locations, depending on the device's hardware capabilities.
     * You should set this value to be as large as possible for your needs if you don't need immediate location
     * delivery.
     *
     * @param millis desired maximum wait time in millisecond, inexact
     * @return the same object, so that setters can be chained
     * @throws IllegalArgumentException if the interval is less than zero
     */
    public LocationRequest setMaxWaitTime(long millis) throws IllegalArgumentException {
        if (millis < 0)
            throw new IllegalArgumentException("interval must not be negative");
        maxWaitTime = millis;
        return this;
    }

    /**
     * Set the number of location updates.
     * <p/>
     * By default locations are continuously updated until the request is explicitly removed,
     * however you can optionally request a set number of updates. For example, if your application
     * only needs a single fresh location, then call this method with a value of 1 before passing
     * the request to the location client.
     * <p/>
     * When using this option care must be taken to either explicitly remove the request when no
     * longer needed or to set an expiration with ({@link #setExpirationDuration(long)} or
     * {@link #setExpirationTime(long)}. Otherwise in some cases if a location can't be computed,
     * this request could stay active indefinitely consuming power.
     *
     * @param numUpdates the number of location updates requested
     * @return the same object, so that setters can be chained
     * @throws IllegalArgumentException if numUpdates is 0 or less
     */
    public LocationRequest setNumUpdates(int numUpdates) throws IllegalArgumentException {
        if (numUpdates <= 0)
            throw new IllegalArgumentException("numUpdates must not be 0 or negative");
        this.numUpdates = numUpdates;
        return this;
    }

    /**
     * Set the priority of the request.
     * <p/>
     * Use with a priority constant such as {@link #PRIORITY_HIGH_ACCURACY}. No other values are
     * accepted.
     * <p/>
     * The priority of the request is a strong hint to the LocationClient for which location
     * sources to use. For example, {@link #PRIORITY_HIGH_ACCURACY} is more likely to use GPS, and
     * {@link #PRIORITY_BALANCED_POWER_ACCURACY} is more likely to use WIFI & Cell tower
     * positioning, but it also depends on many other factors (such as which sources are available)
     * and is implementation dependent.
     * <p/>
     * {@link #setPriority(int)} and {@link #setInterval(long)} are the most important parameters
     * on a location request.
     *
     * @param priority an accuracy or power constant
     * @return the same object, so that setters can be chained
     * @throws IllegalArgumentException if the quality constant is not valid
     */
    public LocationRequest setPriority(int priority) {
        switch (priority) {
            default:
                throw new IllegalArgumentException("priority is not a known constant");
            case PRIORITY_BALANCED_POWER_ACCURACY:
            case PRIORITY_HIGH_ACCURACY:
            case PRIORITY_LOW_POWER:
            case PRIORITY_NO_POWER:
                this.priority = priority;
        }
        return this;
    }

    /**
     * Set the minimum displacement between location updates in meters
     * <p/>
     * By default this is 0.
     *
     * @param smallestDisplacementMeters the smallest displacement in meters the user must move
     *                                   between location updates.
     * @return the same object, so that setters can be chained
     * @throws IllegalArgumentException if smallestDisplacementMeters is negative
     */
    public LocationRequest setSmallestDisplacement(float smallestDisplacementMeters) {
        if (smallestDisplacementMeters < 0)
            throw new IllegalArgumentException("smallestDisplacementMeters must not be negative");
        this.smallestDisplacement = smallestDisplacementMeters;
        return this;
    }

    /**
     * Sets whether the client wants the locations services to wait a few seconds for accurate locations initially,
     * when accurate locations could not be computed on the device immediately after {@link #PRIORITY_HIGH_ACCURACY}
     * request is made. By default the location services will wait for accurate locations.
     * <p>
     * Note that this only applies to clients with {@link #PRIORITY_HIGH_ACCURACY} requests.
     * <p>
     * Also note this only applies to the initial locations computed right after the location request is added. The
     * following inaccurate locations may still be delivered to the clients without delay.
     */
    public LocationRequest setWaitForAccurateLocation(boolean waitForAccurateLocation) {
        this.waitForAccurateLocation = waitForAccurateLocation;
        return this;
    }

    private static String priorityToString(int priority) {
        switch (priority) {
            case PRIORITY_HIGH_ACCURACY:
                return "PRIORITY_HIGH_ACCURACY";
            case PRIORITY_BALANCED_POWER_ACCURACY:
                return "PRIORITY_BALANCED_POWER_ACCURACY";
            case PRIORITY_LOW_POWER:
                return "PRIORITY_LOW_POWER";
            case PRIORITY_NO_POWER:
                return "PRIORITY_NO_POWER";
            default:
                return "???";
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Request[");
        sb.append(priorityToString(priority));
        if (priority != PRIORITY_NO_POWER)
            sb.append(" requested=").append(interval).append("ms");
        sb.append(" fastest=").append(fastestInterval).append("ms");
        if (maxWaitTime > interval)
            sb.append(" maxWait=").append(maxWaitTime).append("ms");
        if (smallestDisplacement > 0)
            sb.append(" smallestDisplacement=").append(smallestDisplacement).append("m");
        if (expirationTime != Long.MAX_VALUE)
            sb.append(" expireIn=").append(SystemClock.elapsedRealtime() - expirationTime).append("ms");
        if (numUpdates != Integer.MAX_VALUE)
            sb.append(" num=").append(numUpdates);
        sb.append("]");
        return sb.toString();
    }

    public static final Creator<LocationRequest> CREATOR = new AutoCreator<LocationRequest>(LocationRequest.class);
}
