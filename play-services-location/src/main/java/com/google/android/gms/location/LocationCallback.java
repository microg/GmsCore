/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import org.microg.gms.common.PublicApi;

/**
 * A callback for receiving notifications from the {@link FusedLocationProviderClient}.
 */
@PublicApi
public abstract class LocationCallback {
    /**
     * Called when there is a change in the availability of location data.
     * <p>
     * When {@link LocationAvailability#isLocationAvailable()} returns false it generally indicates that further
     * invocations of {@link #onLocationResult(LocationResult)} are unlikely until something changes with the device's
     * settings or environment. When {@link LocationAvailability#isLocationAvailable()} returns true it generally
     * indicates that further invocations of {@link #onLocationResult(LocationResult)} are likely, and fresh locations
     * can be expected.
     *
     * @param availability The latest location availability.
     */
    public void onLocationAvailability(LocationAvailability availability) {

    }

    /**
     * Called when a new {@link LocationResult} is available. The locations within the location result will generally
     * be as fresh as possible given the parameters of the associated {@link LocationRequest} and the state of the
     * device, but this does not imply that they will always represent the current location. Clients may wish to
     * reference the time associated with each location.
     *
     * @param result The latest location result.
     */
    public void onLocationResult(LocationResult result) {

    }
}
