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

import android.content.Context;
import android.util.AttributeSet;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.Arrays;

/**
 * An immutable class that aggregates all camera position parameters.
 */
@PublicApi
public final class CameraPosition extends AutoSafeParcelable {
    @SafeParceled(1)
    private int versionCode = 1;
    /**
     * The location that the camera is pointing at.
     */
    @SafeParceled(2)
    public final LatLng target;
    /**
     * Zoom level near the center of the screen.
     * See {@link Builder#zoom(float)} for the definition of the camera's zoom level.
     */
    @SafeParceled(3)
    public final float zoom;
    /**
     * The angle, in degrees, of the camera angle from the nadir (directly facing the Earth).
     * See {@link Builder#tilt(float)} for details of restrictions on the range of values.
     */
    @SafeParceled(4)
    public final float tilt;
    /**
     * Direction that the camera is pointing in, in degrees clockwise from north.
     */
    @SafeParceled(5)
    public final float bearing;

    /**
     * This constructor is dirty setting the final fields to make the compiler happy.
     * In fact, those are replaced by their real values later using SafeParcelUtil.
     */
    private CameraPosition() {
        target = null;
        zoom = tilt = bearing = 0;
    }

    /**
     * Constructs a CameraPosition.
     *
     * @param target  The target location to align with the center of the screen.
     * @param zoom    Zoom level at target. See {@link #zoom} for details of restrictions.
     * @param tilt    The camera angle, in degrees, from the nadir (directly down). See
     *                {@link #tilt} for details of restrictions.
     * @param bearing Direction that the camera is pointing in, in degrees clockwise from north.
     *                This value will be normalized to be within 0 degrees inclusive and 360
     *                degrees exclusive.
     * @throws NullPointerException     if {@code target} is {@code null}
     * @throws IllegalArgumentException if {@code tilt} is outside range of {@code 0} to {@code 90}
     *                                  degrees inclusive
     */
    public CameraPosition(LatLng target, float zoom, float tilt, float bearing)
            throws NullPointerException, IllegalArgumentException {
        if (target == null) {
            throw new NullPointerException("null camera target");
        }
        this.target = target;
        this.zoom = zoom;
        if (tilt < 0 || 90 < tilt) {
            throw new IllegalArgumentException("Tilt needs to be between 0 and 90 inclusive");
        }
        this.tilt = tilt;
        if (bearing <= 0) {
            bearing += 360;
        }
        this.bearing = bearing % 360;
    }

    /**
     * Creates a builder for a camera position.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder for a camera position, initialized to a given position.
     */
    public static Builder builder(CameraPosition camera) {
        return new Builder(camera);
    }

    /**
     * Creates a CameraPostion from the attribute set
     *
     * @throws UnsupportedOperationException
     */
    public static CameraPosition createFromAttributes(Context context, AttributeSet attrs) {
        return null; // TODO
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        CameraPosition that = (CameraPosition) o;

        if (Float.compare(that.bearing, bearing) != 0)
            return false;
        if (Float.compare(that.tilt, tilt) != 0)
            return false;
        if (Float.compare(that.zoom, zoom) != 0)
            return false;
        if (!target.equals(that.target))
            return false;

        return true;
    }

    /**
     * Constructs a CameraPosition pointed for a particular target and zoom level. The resultant
     * bearing is North, and the viewing angle is perpendicular to the Earth's surface. i.e.,
     * directly facing the Earth's surface, with the top of the screen pointing North.
     *
     * @param target The target location to align with the center of the screen.
     * @param zoom   Zoom level at target. See {@link Builder#zoom(float)} for details on the range
     *               the value will be clamped to. The larger the value the more zoomed in the
     *               camera is.
     */
    public static final CameraPosition fromLatLngZoom(LatLng target, float zoom) {
        return builder().target(target).zoom(zoom).build();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] { target, zoom, tilt, bearing });
    }

    @Override
    public String toString() {
        return "CameraPosition{" +
                "target=" + target +
                ", zoom=" + zoom +
                ", tilt=" + tilt +
                ", bearing=" + bearing +
                '}';
    }

    public static Creator<CameraPosition> CREATOR = new AutoCreator<CameraPosition>(CameraPosition.class);

    /**
     * Builds camera position.
     */
    public static final class Builder {
        private LatLng target;
        private float zoom;
        private float tilt;
        private float bearing;

        public Builder() {
        }

        public Builder(CameraPosition previous) {
            target = previous.target;
            zoom = previous.zoom;
            tilt = previous.tilt;
            bearing = previous.bearing;
        }

        /**
         * Sets the direction that the camera is pointing in, in degrees clockwise from north.
         */
        public Builder bearing(float bearing) {
            this.bearing = bearing;
            return this;
        }

        /**
         * Builds a {@link CameraPosition}.
         */
        public CameraPosition build() {
            return new CameraPosition(target, zoom, tilt, bearing);
        }

        /**
         * Sets the location that the camera is pointing at.
         */
        public Builder target(LatLng target) {
            this.target = target;
            return this;
        }

        /**
         * Sets the angle, in degrees, of the camera from the nadir (directly facing the Earth).
         * When changing the camera position for a map, this value is restricted depending on the
         * zoom level of the camera. The restrictions are as follows:
         * <ul>
         * <li>For zoom levels less than 10 the maximum is 30.</li>
         * <li>For zoom levels from 10 to 14 the maximum increases linearly from 30 to 45 (e.g. at
         * zoom level 12, the maximum is 37.5).</li>
         * <li>For zoom levels from 14 to 15.5 the maximum increases linearly from 45 to 67.5.</li>
         * <li>For zoom levels greater than 15.5 the maximum is 67.5.</li>
         * </ul>
         * The minimum is always 0 (directly down). If you specify a value outside this range and try to move the camera to this camera position it will be clamped to these bounds.
         */
        public Builder tilt(float tilt) {
            this.tilt = tilt;
            return this;
        }

        /**
         * Sets the zoom level of the camera. Zoom level is defined such that at zoom level 0, the
         * whole world is approximately 256dp wide (assuming that the camera is not tilted).
         * Increasing the zoom level by 1 doubles the width of the world on the screen. Hence at
         * zoom level N, the width of the world is approximately 256 * 2 N dp, i.e., at zoom level
         * 2, the whole world is approximately 1024dp wide.
         * <p/>
         * When changing the camera position for a map, the zoom level of the camera is restricted
         * to a certain range depending on various factors including location, map type and map
         * size. Note that the camera zoom need not be an integer value.
         */
        public Builder zoom(float zoom) {
            this.zoom = zoom;
            return this;
        }
    }
}
