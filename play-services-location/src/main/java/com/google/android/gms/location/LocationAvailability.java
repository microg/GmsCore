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

import android.content.Intent;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.Arrays;

/**
 * Status on the availability of location data.
 * <p/>
 * Delivered from LocationCallback registered via FusedLocationProviderApi#requestLocationUpdates(GoogleApiClient, LocationRequest, LocationCallback, Looper)
 * or from a PendingIntent registered via FusedLocationProviderApi#requestLocationUpdates(GoogleApiClient, LocationRequest, PendingIntent).
 * It is also available on demand via FusedLocationProviderApi#getLocationAvailability(GoogleApiClient).
 */
@PublicApi
public class LocationAvailability extends AutoSafeParcelable {
    private static final String EXTRA_KEY = "com.google.android.gms.location.EXTRA_LOCATION_AVAILABILITY";

    @PublicApi(exclude = true)
    public static final int STATUS_SUCCESSFUL = 0;
    @PublicApi(exclude = true)
    public static final int STATUS_UNKNOWN = 1;
    @PublicApi(exclude = true)
    public static final int STATUS_TIMED_OUT_ON_SCAN = 2;
    @PublicApi(exclude = true)
    public static final int STATUS_NO_INFO_IN_DATABASE = 3;
    @PublicApi(exclude = true)
    public static final int STATUS_INVALID_SCAN = 4;
    @PublicApi(exclude = true)
    public static final int STATUS_UNABLE_TO_QUERY_DATABASE = 5;
    @PublicApi(exclude = true)
    public static final int STATUS_SCANS_DISABLED_IN_SETTINGS = 6;
    @PublicApi(exclude = true)
    public static final int STATUS_LOCATION_DISABLED_IN_SETTINGS = 7;
    @PublicApi(exclude = true)
    public static final int STATUS_IN_PROGRESS = 8;

    @SafeParceled(1000)
    private int versionCode = 2;

    @SafeParceled(1)
    @PublicApi(exclude = true)
    public int cellStatus;

    @SafeParceled(2)
    @PublicApi(exclude = true)
    public int wifiStatus;

    @SafeParceled(3)
    @PublicApi(exclude = true)
    public long elapsedRealtimeNs;

    @SafeParceled(4)
    @PublicApi(exclude = true)
    public int locationStatus;

    @PublicApi(exclude = true)
    public LocationAvailability() {
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
     * <p/>
     * This is a utility function which extracts the {@link LocationAvailability} from the extras
     * of an Intent that was sent in response to a location request.
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
     * <p/>
     * This is a utility function that can be called from inside an intent receiver to make sure the
     * received intent contains location availability data.
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
     * Returns true if the device location is known and reasonably up to date within the hints
     * requested by the active {@link LocationRequest}s. Failure to determine location may result
     * from a number of causes including disabled location settings or an inability to retrieve
     * sensor data in the device's environment.
     */
    public boolean isLocationAvailable() {
        return locationStatus < 1000;
    }

    @Override
    public String toString() {
        return "LocationAvailability[isLocationAvailable: " + isLocationAvailable() + "]";
    }

    public static final Creator<LocationAvailability> CREATOR = new AutoCreator<LocationAvailability>(LocationAvailability.class);
}
