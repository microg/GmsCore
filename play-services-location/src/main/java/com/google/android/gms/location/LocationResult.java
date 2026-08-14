/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location;

import android.content.Intent;
import android.location.Location;

import androidx.annotation.NonNull;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.ArrayList;
import java.util.List;

@PublicApi
public class LocationResult extends AutoSafeParcelable {
    @PublicApi(exclude = true)
    public static final String EXTRA_LOCATION_RESULT = "com.google.android.gms.location.EXTRA_LOCATION_RESULT";

    @Field(1000)
    private int versionCode = 2;

    @Field(value = 1, subClass = Location.class)
    public final List<Location> locations;

    private LocationResult() {
        this.locations = new ArrayList<>();
    }

    private LocationResult(List<Location> locations) {
        this.locations = locations;
    }

    /**
     * Creates a {@link LocationResult} for the given locations.
     */
    public static LocationResult create(List<Location> locations) {
        return new LocationResult(locations);
    }

    /**
     * Extracts the {@link LocationResult} from an Intent.
     * <p>
     * This is a utility function which extracts the {@link LocationResult} from the extras of an Intent that was sent
     * from the fused location provider.
     *
     * @return a {@link LocationResult}, or {@code null} if the Intent doesn't contain a result.
     */
    public static LocationResult extractResult(Intent intent) {
        if (!hasResult(intent)) return null;
        return intent.getExtras().getParcelable(EXTRA_LOCATION_RESULT);
    }

    /**
     * Returns true if an Intent contains a {@link LocationResult}.
     * <p>
     * This is a utility function that can be called from inside an intent receiver to make sure the received intent is
     * from the fused location provider.
     *
     * @return true if the intent contains a {@link LocationResult}, false otherwise.
     */
    public static boolean hasResult(Intent intent) {
        if (intent == null) return false;
        return intent.hasExtra(EXTRA_LOCATION_RESULT);
    }

    /**
     * Returns the most recent location available in this result, or null if no locations are available.
     */
    public Location getLastLocation() {
        if (locations.isEmpty()) return null;
        return locations.get(locations.size() - 1);
    }

    /**
     * Returns locations computed, ordered from oldest to newest.
     * <p>
     * No duplicate locations will be returned to any given listener (i.e. locations will not overlap in time between
     * subsequent calls to a listener).
     */
    @NonNull
    public List<Location> getLocations() {
        return locations;
    }

    @Override
    public String toString() {
        return "LocationResult[locations: " + locations + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LocationResult)) return false;

        LocationResult that = (LocationResult) obj;
        if (that.locations.size() != locations.size()) return false;
        for (int i = 0; i < that.locations.size(); i++) {
            if (that.locations.get(i).getTime() != locations.get(i).getTime()) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 17;
        for (Location location : locations) {
            long time = location.getTime();
            result = (result * 31) + ((int) (time ^ (time >>> 32)));
        }
        return result;
    }

    public static final Creator<LocationResult> CREATOR = new AutoCreator<LocationResult>(LocationResult.class);
}
