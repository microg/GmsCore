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
package com.google.android.gms.location

import android.os.SystemClock
import com.google.android.gms.location.internal.ParcelableGeofence

/**
 * Represents a geographical region, also known as a geofence. Geofences can be monitored by
 * geofencer service. And when the user crosses the boundary of a geofence, an alert will be
 * generated.
 */
interface Geofence {
    /**
     * Returns the request ID of this geofence. The request ID is a string to identify this geofence
     * inside your application. When two geofences with the same requestId are monitored, the new
     * one will replace the old one regardless the geographical region these two geofences
     * represent.
     */
    val requestId: String?

    /**
     * A builder that builds [Geofence].
     */
    class Builder {
        private var regionType = -1
        private var latitude = 0.0
        private var longitude = 0.0
        private var radius = 0f
        private var expirationTime = Long.MIN_VALUE
        private var loiteringDelay = -1
        private var notificationResponsiveness = 0
        private var requestId: String? = null
        private var transitionTypes = 0

        /**
         * Creates a geofence object.
         *
         * @throws IllegalArgumentException if any parameters are not set or out of range
         */
        @Throws(IllegalArgumentException::class)
        fun build(): Geofence = when {
            requestId == null -> {
                throw IllegalArgumentException("Request ID not set.")
            }
            transitionTypes == 0 -> {
                throw IllegalArgumentException("Transition types not set.")
            }
            transitionTypes and GEOFENCE_TRANSITION_DWELL > 0 && loiteringDelay < 0 -> {
                throw IllegalArgumentException("Non-negative loitering delay needs to be set when transition types include GEOFENCE_TRANSITION_DWELLING.")
            }
            expirationTime == Long.MIN_VALUE -> {
                throw IllegalArgumentException("Expiration not set.")
            }
            regionType == -1 -> {
                throw IllegalArgumentException("Geofence region not set.")
            }
            notificationResponsiveness < 0 -> {
                throw IllegalArgumentException("Notification responsiveness should be nonnegative.")
            }
            else -> {
                ParcelableGeofence(requestId, expirationTime, regionType, latitude, longitude, radius, transitionTypes, notificationResponsiveness, loiteringDelay)
            }
        }

        /**
         * Sets the region of this geofence. The geofence represents a circular area on a flat, horizontal plane.
         *
         * @param latitude  latitude in degrees, between -90 and +90 inclusive
         * @param longitude longitude in degrees, between -180 and +180 inclusive
         * @param radius    radius in meters
         */
        fun setCircularRegion(latitude: Double, longitude: Double, radius: Float): Builder {
            regionType = 1
            this.latitude = latitude
            this.longitude = longitude
            this.radius = radius
            return this
        }

        /**
         * Sets the expiration duration of geofence. This geofence will be removed automatically
         * after this period of time.
         *
         * @param durationMillis time for this proximity alert, in milliseconds, or [.NEVER_EXPIRE]
         * to indicate no expiration. When positive, this geofence will be
         * removed automatically after this amount of time.
         */
        fun setExpirationDuration(durationMillis: Long): Builder {
            expirationTime = if (durationMillis < 0) {
                -1
            } else {
                SystemClock.elapsedRealtime() + durationMillis
            }
            return this
        }

        fun setLoiteringDelay(loiteringDelayMs: Int): Builder {
            loiteringDelay = loiteringDelayMs
            return this
        }

        /**
         * Sets the best-effort notification responsiveness of the geofence. Defaults to 0. Setting
         * a big responsiveness value, for example 5 minutes, can save power significantly. However,
         * setting a very small responsiveness value, for example 5 seconds, doesn't necessarily
         * mean you will get notified right after the user enters or exits a geofence: internally,
         * the geofence might adjust the responsiveness value to save power when needed.
         *
         * @param notificationResponsivenessMs notificationResponsivenessMs	(milliseconds) defines
         * the best-effort description of how soon should the
         * callback be called when the transition associated
         * with the Geofence is triggered. For instance, if set
         * to 300000 milliseconds the callback will be called 5
         * minutes within entering or exiting the geofence.
         */
        fun setNotificationResponsiveness(notificationResponsivenessMs: Int): Builder {
            notificationResponsiveness = notificationResponsivenessMs
            return this
        }

        /**
         * Sets the request ID of the geofence. Request ID is a string to identify this geofence
         * inside your application. When two geofences with the same requestId are monitored, the
         * new one will replace the old one regardless the geographical region these two geofences
         * represent.
         *
         * @param requestId the request ID. The length of the string can be up to 100 characters.
         */
        fun setRequestId(requestId: String?): Builder {
            this.requestId = requestId
            return this
        }

        /**
         * Sets the transition types of interest. Alerts are only generated for the given transition
         * types.
         *
         * @param transitionTypes geofence transition types of interest, as a bitwise-OR of
         * GEOFENCE_TRANSITION_ flags.
         */
        fun setTransitionTypes(transitionTypes: Int): Builder {
            this.transitionTypes = transitionTypes
            return this
        }
    }

    companion object {
        const val GEOFENCE_TRANSITION_DWELL = 4

        /**
         * The transition type indicating that the user enters the geofence(s).
         */
        const val GEOFENCE_TRANSITION_ENTER = 1

        /**
         * The transition type indicating that the user exits the geofence(s).
         */
        const val GEOFENCE_TRANSITION_EXIT = 2

        /**
         * Expiration value that indicates the geofence should never expire.
         */
        const val NEVER_EXPIRE = -1L
    }
}