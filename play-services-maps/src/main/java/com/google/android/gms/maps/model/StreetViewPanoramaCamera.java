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
 * An immutable class that aggregates all camera position parameters.
 */
@SafeParcelable.Class
public class StreetViewPanoramaCamera extends AbstractSafeParcelable {

    /**
     * Zoom level near the centre of the screen. See {@link StreetViewPanoramaCamera.Builder#zoom} for the definition of the camera's zoom level.
     */
    @Field(2)
    public final float zoom;

    /**
     * The angle, in degrees, of the camera from the horizon of the panorama. See {@link StreetViewPanoramaCamera.Builder#tilt} for details of
     * restrictions on the range of values.
     */
    @Field(3)
    public final float tilt;

    /**
     * Direction that the camera is pointing in, in degrees clockwise from north.
     */
    @Field(4)
    public final float bearing;

    /**
     * Constructs a StreetViewPanoramaCamera.
     *
     * @param zoom    Zoom level of the camera to the panorama. See {@link StreetViewPanoramaCamera.Builder#zoom} for details of restrictions.
     * @param tilt    The camera angle, in degrees, from the horizon of the panorama. See {@link StreetViewPanoramaCamera.Builder#tilt} for details of restrictions.
     * @param bearing Direction that the camera is pointing in, in degrees clockwise from north. This value will be normalized to be within 0 degrees inclusive and 360
     *                degrees exclusive.
     * @throws IllegalArgumentException if {@code tilt} is outside the range of -90 to 90 degrees inclusive.
     */
    @Constructor
    public StreetViewPanoramaCamera(@Param(2) float zoom, @Param(3) float tilt, @Param(4) float bearing) throws IllegalArgumentException {
        if (tilt < -90.0f || tilt > 90.0f) throw new IllegalArgumentException();
        this.zoom = zoom;
        this.tilt = tilt;
        this.bearing = (bearing < 0.0f ? (bearing % 360.0f) + 360.0f : bearing) % 360.0f;
    }

    /**
     * Creates a builder for a Street View panorama camera.
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder for a Street View panorama camera.
     *
     * @param camera The camera that will be set to the builder. Must not be {@code null}.
     */
    @NonNull
    public static Builder builder(@NonNull StreetViewPanoramaCamera camera) {
        return new Builder(camera);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StreetViewPanoramaCamera)) return false;

        StreetViewPanoramaCamera that = (StreetViewPanoramaCamera) o;
        return zoom == that.zoom && tilt == that.tilt && bearing == that.bearing;
    }

    /**
     * Returns the particular camera's tilt and bearing as an orientation
     *
     * @return orientation Tilt and bearing of the camera
     */
    @NonNull
    public StreetViewPanoramaOrientation getOrientation() {
        return new StreetViewPanoramaOrientation(tilt, bearing);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[]{this.zoom, this.tilt, this.bearing});
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("StreetViewPanoramaCamera")
                .field("zoom", zoom)
                .field("tilt", tilt)
                .field("bearing", bearing)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<StreetViewPanoramaCamera> CREATOR = findCreator(StreetViewPanoramaCamera.class);

    /**
     * Builds panorama cameras.
     */
    public static class Builder {
        public float zoom;
        public float tilt;
        public float bearing;

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Creates a builder with an existing {@link StreetViewPanoramaCamera}.
         *
         * @param camera The existing camera to initialize the builder. Must not be {@code null}.
         */
        public Builder(StreetViewPanoramaCamera camera) {
            this.zoom = camera.zoom;
            this.tilt = camera.tilt;
            this.bearing = camera.bearing;
        }

        /**
         * Sets the direction that the camera is pointing in, in degrees clockwise from north.
         */
        @NonNull
        public Builder bearing(float bearing) {
            this.bearing = bearing;
            return this;
        }

        /**
         * Sets the camera tilt and bearing based upon the given orientation's tilt and bearing.
         *
         * @param orientation The orientation to be set for the builder. Must not be {@code null}.
         */
        @NonNull
        public Builder orientation(@NonNull StreetViewPanoramaOrientation orientation) {
            this.tilt = orientation.tilt;
            this.bearing = orientation.bearing;
            return this;
        }

        /**
         * Sets the angle, in degrees, of the camera from the horizon of the panorama. This value is restricted to being between -90 (directly down) and
         * 90 (directly up).
         */
        @NonNull
        public Builder tilt(float tilt) {
            this.tilt = tilt;
            return this;
        }

        /**
         * Sets the zoom level of the camera. The original zoom level is set at 0. A zoom of 1 would double the magnification. The zoom is clamped
         * between 0 and the maximum zoom level. The maximum zoom level can vary based upon the panorama. Clamped means that any value
         * falling outside this range will be set to the closest extreme that falls within the range. For example, a value of -1 will be set to 0. Another
         * example: If the maximum zoom for the panorama is 19, and the value is given as 20, it will be set to 19. Note that the camera zoom need not
         * be an integer value.
         */
        @NonNull
        public Builder zoom(float zoom) {
            this.zoom = zoom;
            return this;
        }

        /**
         * Builds a {@link StreetViewPanoramaCamera}.
         */
        public StreetViewPanoramaCamera build() {
            return new StreetViewPanoramaCamera(zoom, tilt, bearing);
        }
    }
}
