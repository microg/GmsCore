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

package com.google.android.gms.maps.model;

import android.os.Parcel;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.PublicApi;

/**
 * An immutable class representing a pair of latitude and longitude coordinates, stored as degrees.
 */
@PublicApi
@SafeParcelable.Class
public final class LatLng extends AbstractSafeParcelable {
    @Field(1)
    int versionCode = 1;
    /**
     * Latitude, in degrees. This value is in the range [-90, 90].
     */
    @Field(2)
    public final double latitude;
    /**
     * Longitude, in degrees. This value is in the range [-180, 180).
     */
    @Field(3)
    public final double longitude;

    /**
     * This constructor is dirty setting the final fields to make the compiler happy.
     * In fact, those are replaced by their real values later using SafeParcelUtil.
     */
    private LatLng() {
        latitude = longitude = 0;
    }

    @Constructor
    LatLng(@Param(1) int versionCode, @Param(2) double latitude, @Param(3) double longitude) {
        this.versionCode = versionCode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Constructs a LatLng with the given latitude and longitude, measured in degrees.
     *
     * @param latitude  The point's latitude. This will be clamped to between -90 degrees and
     *                  +90 degrees inclusive.
     * @param longitude The point's longitude. This will be normalized to be within -180 degrees
     *                  inclusive and +180 degrees exclusive.
     */
    public LatLng(double latitude, double longitude) {
        this.latitude = Math.max(-90, Math.min(90, latitude));
        if ((-180 <= longitude) && (longitude < 180)) {
            this.longitude = longitude;
        } else {
            this.longitude = ((360 + (longitude - 180) % 360) % 360 - 180);
        }
    }

    /**
     * Tests if this LatLng is equal to another.
     * <p/>
     * Two points are considered equal if and only if their latitudes are bitwise equal and their
     * longitudes are bitwise equal. This means that two {@link LatLng}s that are very near, in
     * terms of geometric distance, might not be considered {@code .equal()}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        LatLng latLng = (LatLng) o;

        if (Double.compare(latLng.latitude, latitude) != 0)
            return false;
        if (Double.compare(latLng.longitude, longitude) != 0)
            return false;

        return true;
    }

    @Override
    public final int hashCode() {
        long tmp1 = Double.doubleToLongBits(latitude);
        int tmp2 = 31 + (int) (tmp1 ^ tmp1 >>> 32);
        tmp1 = Double.doubleToLongBits(longitude);
        return tmp2 * 31 + (int) (tmp1 ^ tmp1 >>> 32);
    }

    @Override
    public String toString() {
        return "lat/lng: (" + latitude + "," + longitude + ")";
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static SafeParcelableCreatorAndWriter<LatLng> CREATOR = findCreator(LatLng.class);
}
