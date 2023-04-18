/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.manager

import android.location.Location
import android.os.SystemClock
import androidx.core.location.LocationCompat
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.Granularity.GRANULARITY_COARSE
import java.security.SecureRandom
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.round

class LocationPostProcessor {
    private var nextUpdateElapsedRealtime = 0L
    private val random = SecureRandom()
    private var latitudeOffsetMeters = nextOffsetMeters()
    private var longitudeOffsetMeters = nextOffsetMeters()

    // We cache the latest coarsed location
    private var coarseLocationBefore: Location? = null
    private var coarseLocationAfter: Location? = null

    private fun nextOffsetMeters(): Double = random.nextGaussian() * (COARSE_ACCURACY_METERS / 4.0)

    // We change the offset regularly to ensure there is no possibility to determine the offset and thus know exact locations when at a cliff.
    private fun updateOffsetMetersIfNeeded() {
        if (nextUpdateElapsedRealtime >= SystemClock.elapsedRealtime()) {
            latitudeOffsetMeters = latitudeOffsetMeters * 0.97 + nextOffsetMeters() * 0.03
            longitudeOffsetMeters = longitudeOffsetMeters * 0.97 + nextOffsetMeters() * 0.03
            nextUpdateElapsedRealtime = SystemClock.elapsedRealtime() + COARSE_UPDATE_TIME
        }
    }

    fun process(location: Location?, granularity: @Granularity Int, forGoogle: Boolean): Location? {
        if (location == null) return null
        val extrasAllowList = if (forGoogle) GOOGLE_EXTRAS_LIST else PUBLIC_EXTRAS_LIST
        return when {
            granularity == GRANULARITY_COARSE -> {
                if (location == coarseLocationBefore || location == coarseLocationAfter) {
                    coarseLocationAfter
                } else {
                    val newLocation = Location(location)
                    newLocation.removeBearing()
                    newLocation.removeSpeed()
                    newLocation.removeAltitude()
                    if (LocationCompat.hasBearingAccuracy(newLocation)) LocationCompat.setBearingAccuracyDegrees(newLocation, 0f)
                    if (LocationCompat.hasSpeedAccuracy(newLocation)) LocationCompat.setSpeedAccuracyMetersPerSecond(newLocation, 0f)
                    if (LocationCompat.hasVerticalAccuracy(newLocation)) LocationCompat.setVerticalAccuracyMeters(newLocation, 0f)
                    newLocation.extras = null
                    newLocation.accuracy = max(newLocation.accuracy, COARSE_ACCURACY_METERS)
                    updateOffsetMetersIfNeeded()
                    val latitudeAccuracy = metersToLongitudeAtEquator(COARSE_ACCURACY_METERS.toDouble())
                    val longitudeAccuracy = metersToLongitudeAtLatitude(COARSE_ACCURACY_METERS.toDouble(), location.latitude)
                    val offsetLatitude = coerceLatitude(location.latitude) + metersToLongitudeAtEquator(latitudeOffsetMeters)
                    newLocation.latitude = coerceLatitude(round(offsetLatitude / latitudeAccuracy) * latitudeAccuracy)
                    val offsetLongitude = coerceLongitude(location.longitude) + metersToLongitudeAtLatitude(longitudeOffsetMeters, newLocation.latitude)
                    newLocation.longitude = coerceLongitude(round(offsetLongitude / longitudeAccuracy) * longitudeAccuracy)
                    coarseLocationBefore = location
                    coarseLocationAfter = newLocation
                    newLocation
                }
            }

            location.hasAnyNonAllowedExtra(extrasAllowList) -> Location(location).stripExtras(extrasAllowList)
            else -> location
        }
    }

    companion object {
        private const val COARSE_ACCURACY_METERS = 2000f
        private const val COARSE_UPDATE_TIME = 3600_000
        private const val EQUATOR_METERS_PER_LONGITUDE = 111000.0

        val PUBLIC_EXTRAS_LIST = listOf(
            "noGPSLocation",
            LocationCompat.EXTRA_VERTICAL_ACCURACY,
            LocationCompat.EXTRA_BEARING_ACCURACY,
            LocationCompat.EXTRA_SPEED_ACCURACY,
            LocationCompat.EXTRA_MSL_ALTITUDE,
            LocationCompat.EXTRA_MSL_ALTITUDE_ACCURACY,
            LocationCompat.EXTRA_IS_MOCK,
        )

        val GOOGLE_EXTRAS_LIST = listOf(
            "noGPSLocation",
            LocationCompat.EXTRA_VERTICAL_ACCURACY,
            LocationCompat.EXTRA_BEARING_ACCURACY,
            LocationCompat.EXTRA_SPEED_ACCURACY,
            LocationCompat.EXTRA_MSL_ALTITUDE,
            LocationCompat.EXTRA_MSL_ALTITUDE_ACCURACY,
            LocationCompat.EXTRA_IS_MOCK,
            "locationType",
            "levelId",
            "levelNumberE3",
            "floorLabel",
            "indoorProbability",
            "wifiScan"
        )

        private fun Location.hasAnyNonAllowedExtra(allowList: List<String>): Boolean {
            for (key in extras?.keySet().orEmpty()) {
                if (key !in allowList) {
                    return true
                }
            }
            return false
        }

        private fun Location.stripExtras(allowList: List<String>): Location {
            val extras = extras
            for (key in extras?.keySet().orEmpty()) {
                if (key !in allowList) {
                    extras?.remove(key)
                }
            }
            this.extras = if (extras?.isEmpty == true) null else extras
            return this
        }

        private fun metersToLongitudeAtEquator(meters: Double): Double = meters / EQUATOR_METERS_PER_LONGITUDE

        private fun metersToLongitudeAtLatitude(meters: Double, latitude: Double): Double = metersToLongitudeAtEquator(meters) / cos(Math.toRadians(latitude))

        /**
         * Coerce latitude value to be between -89.99999° and 89.99999°.
         *
         * Sorry to those, who actually are at the geographical north/south pole, but at exactly 90°, our math wouldn't work out anymore.
         */
        private fun coerceLatitude(latitude: Double): Double = latitude.coerceIn(-89.99999, 89.99999)

        /**
         * Coerce longitude value to be between -180.00° and 180.00°.
         */
        private fun coerceLongitude(longitude: Double): Double = (longitude % 360.0).let {
            when {
                it >= 180.0 -> it - 360.0
                it < -180.0 -> it + 360.0
                else -> it
            }
        }
    }
}