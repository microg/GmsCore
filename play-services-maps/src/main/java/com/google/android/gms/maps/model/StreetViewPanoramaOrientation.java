/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps.model;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.utils.ToStringHelper;

import java.util.Objects;

/**
 * An immutable class that aggregates all user point of view parameters.
 */
@SafeParcelable.Class
public class StreetViewPanoramaOrientation extends AbstractSafeParcelable {

    /**
     * The angle, in degrees, of the orientation. See {@link StreetViewPanoramaOrientation.Builder#tilt} for details of restrictions on the range of
     * values.
     */
    @Field(2)
    public final float tilt;
    /**
     * Direction of the orientation, in degrees clockwise from north.
     */
    @Field(3)
    public final float bearing;

    /**
     * Constructs a StreetViewPanoramaOrientation.
     *
     * @param tilt    The angle, in degrees, of the orientation. See {@link StreetViewPanoramaOrientation.Builder#tilt} for details of restrictions.
     * @param bearing Direction of the orientation, in degrees clockwise from north. This value will be normalized to be within 0 degrees inclusive and 360 degrees
     *                exclusive.
     * @throws IllegalArgumentException if {@code tilt} is outside the range of -90 to 90 degrees inclusive.
     */
    @Constructor
    public StreetViewPanoramaOrientation(@Param(2) float tilt, @Param(3) float bearing) throws IllegalArgumentException {
        if (tilt < -90.0f || tilt > 90.0f) throw new IllegalArgumentException();
        this.tilt = tilt;
        this.bearing = (bearing < 0.0f ? (bearing % 360.0f) + 360.0f : bearing) % 360.0f;
    }

    /**
     * Creates a builder for a Street View panorama orientation.
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder for a Street View panorama orientation
     *
     * @param orientation must not be {@code null}.
     */
    @NonNull
    public static Builder builder(@NonNull StreetViewPanoramaOrientation orientation) {
        return new Builder(orientation);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StreetViewPanoramaOrientation)) return false;

        StreetViewPanoramaOrientation that = (StreetViewPanoramaOrientation) o;
        return tilt == that.tilt && bearing == that.bearing;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[]{this.tilt, this.bearing});
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("StreetViewPanoramaOrientation")
                .field("tilt", tilt)
                .field("bearing", bearing)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<StreetViewPanoramaOrientation> CREATOR = findCreator(StreetViewPanoramaOrientation.class);

    /**
     * Builds Street View panorama orientations.
     */
    public static final class Builder {
        public float bearing;
        public float tilt;

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Creates a builder with an existing {@link StreetViewPanoramaOrientation}.
         *
         * @param previous The existing orientation to initialize the builder with. Must not be {@code null}.
         */
        public Builder(@NonNull StreetViewPanoramaOrientation previous) {
            this.bearing = previous.bearing;
            this.tilt = previous.tilt;
        }

        /**
         * Sets the direction of the orientation, in degrees clockwise from north.
         */
        @NonNull
        public Builder bearing(float bearing) {
            this.bearing = bearing;
            return this;
        }

        /**
         * Sets the angle, in degrees, of the orientation This value is restricted to being between -90 (directly down) and 90 (directly up).
         */
        @NonNull
        public Builder tilt(float tilt) {
            this.tilt = tilt;
            return this;
        }

        /**
         * Builds a {@link StreetViewPanoramaOrientation}.
         */
        @NonNull
        public StreetViewPanoramaOrientation build() {
            return new StreetViewPanoramaOrientation(this.tilt, this.bearing);
        }
    }
}
