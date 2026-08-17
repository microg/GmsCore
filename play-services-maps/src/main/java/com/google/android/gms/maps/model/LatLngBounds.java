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

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.Arrays;

/**
 * An immutable class representing a latitude/longitude aligned rectangle.
 */
@PublicApi
public final class LatLngBounds extends AutoSafeParcelable {
    @SafeParceled(1)
    private int versionCode = 1;
    /**
     * Southwest corner of the bound.
     */
    @SafeParceled(2)
    public final LatLng southwest;
    /**
     * Northeast corner of the bound.
     */
    @SafeParceled(3)
    public final LatLng northeast;

    /**
     * This constructor is dirty setting the final fields to make the compiler happy.
     * In fact, those are replaced by their real values later using SafeParcelUtil.
     */
    private LatLngBounds() {
        southwest = northeast = null;
    }

    /**
     * Creates a new bounds based on a southwest and a northeast corner.
     * <p/>
     * The bounds conceptually includes all points where:
     * <ul>
     * <li>the latitude is in the range [northeast.latitude, southwest.latitude];</li>
     * <li>the longitude is in the range [southwest.longtitude, northeast.longitude]
     * if southwest.longtitude ≤ northeast.longitude; and</li>
     * <li>the longitude is in the range [southwest.longitude, 180) ∪ [-180, northeast.longitude]
     * if southwest.longtitude > northeast.longitude.</li>
     * </ul>
     *
     * @param southwest southwest corner
     * @param northeast northeast corner
     * @throws IllegalArgumentException if the latitude of the northeast corner is below the
     *                                  latitude of the southwest corner.
     */
    public LatLngBounds(LatLng southwest, LatLng northeast) throws IllegalArgumentException {
        if (northeast.latitude < southwest.latitude)
            throw new IllegalArgumentException("latitude of northeast corner must not be" +
                    " lower than latitude of southwest corner");
        this.southwest = southwest;
        this.northeast = northeast;
    }

    /**
     * Creates a new builder.
     */
    public Builder builder() {
        return new Builder();
    }

    /**
     * Returns whether this contains the given {@link LatLng}.
     *
     * @param point the {@link LatLng} to test
     * @return {@code true} if this contains the given point; {@code false} if not.
     */
    public boolean contains(LatLng point) {
        return containsLatitude(point.latitude) && containsLongitude(point.longitude);
    }

    private boolean containsLatitude(double latitude) {
        return southwest.latitude <= latitude && latitude <= northeast.latitude;
    }

    private boolean containsLongitude(double longitude) {
        return southwest.longitude <= northeast.longitude ? (
                southwest.longitude <= longitude && longitude <= northeast.longitude
        ) : (
                southwest.longitude >= longitude && longitude < 180 ||
                        longitude >= -180 && longitude <= northeast.longitude
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        LatLngBounds that = (LatLngBounds) o;

        if (!northeast.equals(that.northeast))
            return false;
        if (!southwest.equals(that.southwest))
            return false;

        return true;
    }

    /**
     * Returns the center of this {@link LatLngBounds}. The center is simply the average of the
     * coordinates (taking into account if it crosses the antimeridian). This is approximately the
     * geographical center (it would be exact if the Earth were a perfect sphere). It will not
     * necessarily be the center of the rectangle as drawn on the map due to the Mercator
     * projection.
     *
     * @return A {@link LatLng} that is the center of the {@link LatLngBounds}.
     */
    public LatLng getCenter() {
        double lat = (southwest.latitude + northeast.latitude) / 2.0;
        double lon = (southwest.longitude + northeast.longitude) / 2.0 +
                southwest.longitude <= northeast.latitude ? 0 : 180.0;
        return new LatLng(lat, lon);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] { southwest, northeast });
    }

    /**
     * Returns a new {@link LatLngBounds} that extends this {@link LatLngBounds} to include the
     * given {@link LatLng}. This will return the smallest LatLngBounds that contains both this
     * and the extra point.
     * <p/>
     * In particular, it will consider extending the bounds both in the eastward and westward
     * directions (one of which may cross the antimeridian) and choose the smaller of the two. In
     * the case that both directions result in a LatLngBounds of the same size, this will extend
     * it in the eastward direction.
     *
     * @param point a {@link LatLng} to be included in the new bounds
     * @return A new {@link LatLngBounds} that contains this and the extra point.
     */
    public LatLngBounds including(LatLng point) {
        double latMin = Math.min(southwest.latitude, point.latitude);
        double latMax = Math.max(northeast.latitude, point.latitude);
        double lonMin = southwest.longitude;
        double lonMax = northeast.longitude;
        if (!containsLongitude(point.longitude)) {
            if ((southwest.longitude - point.longitude + 360.0) % 360.0 <
                    (point.longitude - northeast.longitude + 360.0D) % 360.0D) {
                lonMin = point.longitude;
            } else {
                lonMax = point.longitude;
            }
        }
        return new LatLngBounds(new LatLng(latMin, lonMin), new LatLng(latMax, lonMax));
    }

    @Override
    public String toString() {
        return "LatLngBounds{" +
                "southwest=" + southwest +
                ", northeast=" + northeast +
                '}';
    }

    public static Creator<LatLngBounds> CREATOR = new AutoCreator<LatLngBounds>(LatLngBounds.class);

    /**
     * This is a builder that is able to create a minimum bound based on a set of LatLng points.
     */
    public static final class Builder {
        private LatLngBounds bounds;

        public Builder() {

        }

        /**
         * Creates the LatLng bounds.
         *
         * @throws IllegalStateException if no points have been included.
         */
        public LatLngBounds build() throws IllegalStateException {
            if (bounds == null)
                throw new IllegalStateException(
                        "You must not call build() before adding points to the Builder");
            return bounds;
        }

        /**
         * Includes this point for building of the bounds. The bounds will be extended in a
         * minimum way to include this point.
         * <p/>
         * More precisely, it will consider extending the bounds both in the eastward and westward
         * directions (one of which may cross the antimeridian) and choose the smaller of the two.
         * In the case that both directions result in a LatLngBounds of the same size, this will
         * extend it in the eastward direction. For example, adding points (0, -179) and (1, 179)
         * will create a bound crossing the 180 longitude.
         *
         * @param point A {@link LatLng} to be included in the bounds.
         * @return This builder object with a new point added.
         */
        public Builder include(LatLng point) {
            if (bounds == null) {
                bounds = new LatLngBounds(point, point);
            } else {
                bounds = bounds.including(point);
            }
            return this;
        }
    }
}
