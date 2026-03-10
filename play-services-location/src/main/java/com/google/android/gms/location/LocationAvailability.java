/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.content.Intent;

import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.Arrays;

/**
 * Information on the availability of location data.
 */
@PublicApi
public class LocationAvailability extends AutoSafeParcelable {
    private static final String EXTRA_KEY = "com.google.android.gms.location.EXTRA_LOCATION_AVAILABILITY";

    @Hide
    public static LocationAvailability AVAILABLE = new LocationAvailability(0, 1, 1, 0, null);
    @Hide
    public static LocationAvailability UNAVAILABLE = new LocationAvailability(1000, 1, 1, 0, null);

    @Hide
    public static final int STATUS_SUCCESSFUL = 0;
    @Hide
    public static final int STATUS_UNKNOWN = 1;
    @Hide
    public static final int STATUS_TIMED_OUT_ON_SCAN = 2;
    @Hide
    public static final int STATUS_NO_INFO_IN_DATABASE = 3;
    @Hide
    public static final int STATUS_INVALID_SCAN = 4;
    @Hide
    public static final int STATUS_UNABLE_TO_QUERY_DATABASE = 5;
    @Hide
    public static final int STATUS_SCANS_DISABLED_IN_SETTINGS = 6;
    @Hide
    public static final int STATUS_LOCATION_DISABLED_IN_SETTINGS = 7;
    @Hide
    public static final int STATUS_IN_PROGRESS = 8;

    @Field(1000)
    private int versionCode = 2;

    @Field(1)
    @Hide
    public int cellStatus;

    @Field(2)
    @Hide
    public int wifiStatus;

    @Field(3)
    @Hide
    public long elapsedRealtimeNs;

    @Field(4)
    @Hide
    public int locationStatus;

    @Field(5)
    @Hide
    public NetworkLocationStatus[] batchedStatus;

    private LocationAvailability() {
    }

    @Hide
    public LocationAvailability(int locationStatus, int cellStatus, int wifiStatus, long elapsedRealtimeNs, NetworkLocationStatus[] batchedStatus) {
        this.locationStatus = locationStatus;
        this.cellStatus = cellStatus;
        this.wifiStatus = wifiStatus;
        this.elapsedRealtimeNs = elapsedRealtimeNs;
        this.batchedStatus = batchedStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LocationAvailability) {
            LocationAvailability other = (LocationAvailability) o;
            return other.cellStatus == cellStatus && other.wifiStatus == wifiStatus && other.elapsedRealtimeNs == elapsedRealtimeNs && other.locationStatus == locationStatus;
        }
        return false;
    }

    /**
     * Extracts the {@link LocationAvailability} from an Intent.
     *
     * @return a {@link LocationAvailability}, or null if the Intent doesn't contain this data.
     */
    public static LocationAvailability extractLocationAvailability(Intent intent) {
        if (!hasLocationAvailability(intent)) {
            return null;
        }
        return intent.getParcelableExtra(EXTRA_KEY);
    }

    /**
     * Returns true if an Intent contains a {@link LocationAvailability}.
     *
     * @return true if the intent contains a {@link LocationAvailability}, false otherwise.
     */
    public static boolean hasLocationAvailability(Intent intent) {
        return intent != null && intent.hasExtra(EXTRA_KEY);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{locationStatus, cellStatus, wifiStatus, elapsedRealtimeNs});
    }

    /**
     * Returns true if the device location is generally available.
     */
    public boolean isLocationAvailable() {
        return locationStatus < 1000;
    }

    @Override
    public String toString() {
        return "LocationAvailability[" + isLocationAvailable() + "]";
    }

    public static final Creator<LocationAvailability> CREATOR = new AutoCreator<LocationAvailability>(LocationAvailability.class);
}
