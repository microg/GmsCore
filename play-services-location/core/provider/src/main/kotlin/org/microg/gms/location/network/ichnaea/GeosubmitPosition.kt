/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.ichnaea

data class GeosubmitPosition(
    /**
     * The latitude of the observation (WSG 84).
     */
    val latitude: Double? = null,
    /**
     * The longitude of the observation (WSG 84).
     */
    val longitude: Double? = null,
    /**
     * The accuracy of the observed position in meters.
     */
    val accuracy: Double? = null,
    /**
     * The altitude at which the data was observed in meters above sea-level.
     */
    val altitude: Double? = null,
    /**
     * The accuracy of the altitude estimate in meters.
     */
    val altitudeAccuracy: Double? = null,
    /**
     * The age of the position data (in milliseconds).
     */
    val age: Long? = null,
    /**
     * The heading field denotes the direction of travel of the device and is specified in degrees, where 0° ≤ heading < 360°, counting clockwise relative to the true north.
     */
    val heading: Double? = null,
    /**
     * The air pressure in hPa (millibar).
     */
    val pressure: Double? = null,
    /**
     * The speed field denotes the magnitude of the horizontal component of the device’s current velocity and is specified in meters per second.
     */
    val speed: Double? = null,
    /**
     * The source of the position information. If the field is omitted, “gps” is assumed. The term gps is used to cover all types of satellite based positioning systems including Galileo and Glonass. Other possible values are manual for a position entered manually into the system and fused for a position obtained from a combination of other sensors or outside service queries.
     */
    val source: GeosubmitSource? = null,
)