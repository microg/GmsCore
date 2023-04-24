/*
 * SPDX-FileCopyrightText: 2017 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.os.SystemClock;

import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import com.google.android.gms.location.internal.ParcelableGeofence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a geographical region, also known as a geofence. Geofences can be monitored by
 * geofencer service. And when the user crosses the boundary of a geofence, an alert will be
 * generated.
 */
public interface Geofence {
    int GEOFENCE_TRANSITION_DWELL = 4;

    /**
     * The transition type indicating that the user enters the geofence(s).
     */
    int GEOFENCE_TRANSITION_ENTER = 1;

    /**
     * The transition type indicating that the user exits the geofence(s).
     */
    int GEOFENCE_TRANSITION_EXIT = 2;

    /**
     * Expiration value that indicates the geofence should never expire.
     */
    long NEVER_EXPIRE = -1L;

    /**
     * Returns the expiration elapsed realtime of geofence in milliseconds, or {@link #NEVER_EXPIRE} if there's no expiration. When positive, this geofence will
     * be removed automatically after that time.
     */
    long getExpirationTime();

    /**
     * Returns latitude in degrees, between -90 and +90 inclusive.
     */
    double getLatitude();

    /**
     * Returns the delay between {@link #GEOFENCE_TRANSITION_ENTER} and {@link #GEOFENCE_TRANSITION_DWELL} in milliseconds.
     */
    int getLoiteringDelay();

    /**
     * Returns longitude in degrees, between -180 and +180 inclusive.
     */
    double getLongitude();

    /**
     * Returns the best-effort description of how soon should the callback be called when the transition associated with the geofence is triggered,
     * in milliseconds.
     */
    int getNotificationResponsiveness();

    /**
     * Returns radius in meters.
     */
    float getRadius();

    /**
     * Returns the request ID of this geofence. The request ID is a string to identify this geofence
     * inside your application. When two geofences with the same requestId are monitored, the new
     * one will replace the old one regardless the geographical region these two geofences
     * represent.
     */
    String getRequestId();

    /**
     * Returns the transition types of interest as a bitwise-OR of {@code GEOFENCE_TRANSITION_} flags.
     */
    @TransitionTypes int getTransitionTypes();

    /**
     * A builder that builds {@link Geofence}.
     */
    class Builder {
        private int regionType = -1;
        private double latitude;
        private double longitude;
        private float radius;
        private long expirationTime = Long.MIN_VALUE;
        private int loiteringDelay = -1;
        private int notificationResponsiveness;
        private String requestId;
        private @TransitionTypes int transitionTypes;

        /**
         * Creates a geofence object.
         *
         * @throws IllegalArgumentException if any parameters are not set or out of range
         */
        public Geofence build() throws IllegalArgumentException {
            if (requestId == null) {
                throw new IllegalArgumentException("Request ID not set.");
            } else if (transitionTypes == 0) {
                throw new IllegalArgumentException("Transition types not set.");
            } else if ((transitionTypes & GEOFENCE_TRANSITION_DWELL) > 0 && loiteringDelay < 0) {
                throw new IllegalArgumentException("Non-negative loitering delay needs to be set when transition types include GEOFENCE_TRANSITION_DWELLING.");
            } else if (expirationTime == Long.MIN_VALUE) {
                throw new IllegalArgumentException("Expiration not set.");
            } else if (regionType == -1) {
                throw new IllegalArgumentException("Geofence region not set.");
            } else if (notificationResponsiveness < 0) {
                throw new IllegalArgumentException("Notification responsiveness should be nonnegative.");
            } else {
                return new ParcelableGeofence(requestId, expirationTime, regionType, latitude, longitude, radius, transitionTypes, notificationResponsiveness, loiteringDelay);
            }
        }

        /**
         * Sets the region of this geofence. The geofence represents a circular area on a flat, horizontal plane.
         *
         * @param latitude  latitude in degrees, between -90 and +90 inclusive
         * @param longitude longitude in degrees, between -180 and +180 inclusive
         * @param radius    radius in meters
         */
        public Builder setCircularRegion(@FloatRange(from = -90.0d, to = 90.0d) double latitude, @FloatRange(from = -180.0d, to = 180.0d) double longitude, @FloatRange(from = 0.0d, fromInclusive = false) float radius) {
            this.regionType = 1;
            this.latitude = latitude;
            this.longitude = longitude;
            this.radius = radius;
            return this;
        }

        /**
         * Sets the expiration duration of geofence. This geofence will be removed automatically
         * after this period of time.
         *
         * @param durationMillis time for this proximity alert, in milliseconds, or {@link #NEVER_EXPIRE}
         *                       to indicate no expiration. When positive, this geofence will be
         *                       removed automatically after this amount of time.
         */
        public Builder setExpirationDuration(long durationMillis) {
            if (durationMillis < 0) {
                expirationTime = -1;
            } else {
                expirationTime = SystemClock.elapsedRealtime() + durationMillis;
            }
            return this;
        }

        public Builder setLoiteringDelay(int loiteringDelayMs) {
            this.loiteringDelay = loiteringDelayMs;
            return this;
        }

        /**
         * Sets the best-effort notification responsiveness of the geofence. Defaults to 0. Setting
         * a big responsiveness value, for example 5 minutes, can save power significantly. However,
         * setting a very small responsiveness value, for example 5 seconds, doesn't necessarily
         * mean you will get notified right after the user enters or exits a geofence: internally,
         * the geofence might adjust the responsiveness value to save power when needed.
         *
         * @param notificationResponsivenessMs notificationResponsivenessMs	(milliseconds) defines
         *                                     the best-effort description of how soon should the
         *                                     callback be called when the transition associated
         *                                     with the Geofence is triggered. For instance, if set
         *                                     to 300000 milliseconds the callback will be called 5
         *                                     minutes within entering or exiting the geofence.
         */
        public Builder setNotificationResponsiveness(@IntRange(from = 0) int notificationResponsivenessMs) {
            this.notificationResponsiveness = notificationResponsivenessMs;
            return this;
        }

        /**
         * Sets the request ID of the geofence. Request ID is a string to identify this geofence
         * inside your application. When two geofences with the same requestId are monitored, the
         * new one will replace the old one regardless the geographical region these two geofences
         * represent.
         *
         * @param requestId the request ID. The length of the string can be up to 100 characters.
         */
        public Builder setRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        /**
         * Sets the transition types of interest. Alerts are only generated for the given transition
         * types.
         *
         * @param transitionTypes geofence transition types of interest, as a bitwise-OR of
         *                        GEOFENCE_TRANSITION_ flags.
         */
        public Builder setTransitionTypes(@TransitionTypes int transitionTypes) {
            this.transitionTypes = transitionTypes;
            return this;
        }
    }

    /**
     * Geofence transition event.
     */
    @Target({ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({GEOFENCE_TRANSITION_ENTER, GEOFENCE_TRANSITION_EXIT, GEOFENCE_TRANSITION_DWELL})
    @interface GeofenceTransition {

    }

    /**
     * Geofence transition types of interest, as either 0 or a bitwise-OR of {@code GEOFENCE_TRANSITION_} flags.
     */
    @Target({ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {GEOFENCE_TRANSITION_ENTER, GEOFENCE_TRANSITION_EXIT, GEOFENCE_TRANSITION_DWELL}, flag = true)
    @interface TransitionTypes {

    }
}
