/*
 * SPDX-FileCopyrightText: 2012, The Android Open Source Project
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.location.provider;

import android.location.LocationRequest;

/**
 * This class is an interface to LocationRequests for unbundled applications.
 * <p/>
 * <p>IMPORTANT: This class is effectively a public API for unbundled
 * applications, and must remain API stable. See README.txt in the root
 * of this package for more information.
 */
public final class LocationRequestUnbundled {
    /**
     * Returned by {@link #getQuality} when requesting the most accurate locations available.
     * <p/>
     * <p>This may be up to 1 meter accuracy, although this is implementation dependent.
     */
    public static final int ACCURACY_FINE = LocationRequest.ACCURACY_FINE;

    /**
     * Returned by {@link #getQuality} when requesting "block" level accuracy.
     * <p/>
     * <p>Block level accuracy is considered to be about 100 meter accuracy,
     * although this is implementation dependent. Using a coarse accuracy
     * such as this often consumes less power.
     */
    public static final int ACCURACY_BLOCK = LocationRequest.ACCURACY_BLOCK;

    /**
     * Returned by {@link #getQuality} when requesting "city" level accuracy.
     * <p/>
     * <p>City level accuracy is considered to be about 10km accuracy,
     * although this is implementation dependent. Using a coarse accuracy
     * such as this often consumes less power.
     */
    public static final int ACCURACY_CITY = LocationRequest.ACCURACY_CITY;

    /**
     * Returned by {@link #getQuality} when requiring no direct power impact (passive locations).
     * <p/>
     * <p>This location request will not trigger any active location requests,
     * but will receive locations triggered by other applications. Your application
     * will not receive any direct power blame for location work.
     */
    public static final int POWER_NONE = LocationRequest.POWER_NONE;

    /**
     * Returned by {@link #getQuality} when requesting low power impact.
     * <p/>
     * <p>This location request will avoid high power location work where
     * possible.
     */
    public static final int POWER_LOW = LocationRequest.POWER_LOW;

    /**
     * Returned by {@link #getQuality} when allowing high power consumption for location.
     * <p/>
     * <p>This location request will allow high power location work.
     */
    public static final int POWER_HIGH = LocationRequest.POWER_HIGH;

    /**
     * Get the desired interval of this request, in milliseconds.
     *
     * @return desired interval in milliseconds, inexact
     */
    public long getInterval() {
        return 0;
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
     * Get the quality of the request.
     *
     * @return an accuracy or power constant
     */
    public int getQuality() {
        return 0;
    }

    /**
     * Get the minimum distance between location updates, in meters.
     *
     * @return minimum distance between location updates in meters
     */
    public float getSmallestDisplacement() {
        return 0;
    }
}
