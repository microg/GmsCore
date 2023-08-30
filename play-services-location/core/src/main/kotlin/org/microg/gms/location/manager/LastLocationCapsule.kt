/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.manager

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build.VERSION.SDK_INT
import android.os.SystemClock
import android.util.Log
import androidx.core.content.getSystemService
import androidx.core.location.LocationCompat
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Field
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.Granularity.GRANULARITY_COARSE
import com.google.android.gms.location.Granularity.GRANULARITY_FINE
import com.google.android.gms.location.LocationAvailability
import org.microg.gms.location.elapsedMillis
import org.microg.safeparcel.AutoSafeParcelable
import java.io.File
import java.lang.Long.max
import java.util.concurrent.TimeUnit

class LastLocationCapsule(private val context: Context) {
    private var lastFineLocation: Location? = null
    private var lastCoarseLocation: Location? = null

    private var lastFineLocationTimeCoarsed: Location? = null
    private var lastCoarseLocationTimeCoarsed: Location? = null

    var locationAvailability: LocationAvailability = LocationAvailability.AVAILABLE

    private val file: File
        get() = context.getFileStreamPath(FILE_NAME)

    fun getLocation(effectiveGranularity: @Granularity Int, maxUpdateAgeMillis: Long): Location? {
        val location = when (effectiveGranularity) {
            GRANULARITY_COARSE -> lastCoarseLocationTimeCoarsed
            GRANULARITY_FINE -> lastCoarseLocation
            else -> return null
        } ?: return null
        val cliff = if (effectiveGranularity == GRANULARITY_COARSE) max(maxUpdateAgeMillis, TIME_COARSE_CLIFF) else maxUpdateAgeMillis
        val elapsedRealtimeDiff = SystemClock.elapsedRealtime() - LocationCompat.getElapsedRealtimeMillis(location)
        if (elapsedRealtimeDiff > cliff) return null
        if (elapsedRealtimeDiff <= maxUpdateAgeMillis) return location
        // Location is too old according to maxUpdateAgeMillis, but still in scope due to time coarsing. Adjust time
        val locationUpdated = Location(location)
        val timeAdjustment = elapsedRealtimeDiff - maxUpdateAgeMillis
        if (SDK_INT >= 17) {
            locationUpdated.elapsedRealtimeNanos = location.elapsedRealtimeNanos + TimeUnit.MILLISECONDS.toNanos(timeAdjustment)
        }
        locationUpdated.time = location.time + timeAdjustment
        return locationUpdated
    }

    fun reset() {
        lastFineLocation = null
        lastFineLocationTimeCoarsed = null
        lastCoarseLocation = null
        lastCoarseLocationTimeCoarsed = null
        locationAvailability = LocationAvailability.AVAILABLE
    }

    fun updateCoarseLocation(location: Location) {
        if (lastCoarseLocation != null && lastCoarseLocation!!.elapsedMillis + EXTENSION_CLIFF > location.elapsedMillis) {
            if (!location.hasSpeed()) {
                location.speed = lastCoarseLocation!!.distanceTo(location) / ((location.elapsedMillis - lastCoarseLocation!!.elapsedMillis) / 1000)
                LocationCompat.setSpeedAccuracyMetersPerSecond(location, location.speed)
            }
            if (!location.hasBearing() && location.speed > 0.5f) {
                location.bearing = lastCoarseLocation!!.bearingTo(location)
                LocationCompat.setBearingAccuracyDegrees(location, 180.0f)
            }
        }
        lastCoarseLocation = newest(lastCoarseLocation, location)
        lastCoarseLocationTimeCoarsed = newest(lastCoarseLocationTimeCoarsed, location, TIME_COARSE_CLIFF)
    }

    fun updateFineLocation(location: Location) {
        lastFineLocation = newest(lastFineLocation, location)
        lastFineLocationTimeCoarsed = newest(lastFineLocationTimeCoarsed, location, TIME_COARSE_CLIFF)
        updateCoarseLocation(location)
    }

    private fun newest(oldLocation: Location?, newLocation: Location, cliff: Long = 0): Location {
        if (oldLocation == null) return newLocation
        if (LocationCompat.isMock(oldLocation) && !LocationCompat.isMock(newLocation)) return newLocation
        if (LocationCompat.getElapsedRealtimeNanos(newLocation) >= LocationCompat.getElapsedRealtimeNanos(oldLocation) + TimeUnit.MILLISECONDS.toNanos(cliff)) return newLocation
        return oldLocation
    }

    fun start() {
        fun Location.adjustRealtime() = apply {
            if (SDK_INT >= 17) {
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos() - TimeUnit.MILLISECONDS.toNanos((System.currentTimeMillis() - time))
            }
        }
        try {
            if (file.exists()) {
                val capsule = SafeParcelableSerializer.deserializeFromBytes(file.readBytes(), LastLocationCapsuleParcelable.CREATOR)
                lastFineLocation = capsule.lastFineLocation?.adjustRealtime()
                lastCoarseLocation = capsule.lastCoarseLocation?.adjustRealtime()
                lastFineLocationTimeCoarsed = capsule.lastFineLocationTimeCoarsed?.adjustRealtime()
                lastCoarseLocationTimeCoarsed = capsule.lastCoarseLocationTimeCoarsed?.adjustRealtime()
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
            // Ignore
        }
        val locationManager = context.getSystemService<LocationManager>() ?: return
        try {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let { updateCoarseLocation(it) }
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { updateFineLocation(it) }
        } catch (e: SecurityException) {
            // Ignore
        }
    }

    fun stop() {
        try {
            if (file.exists()) file.delete()
            file.writeBytes(SafeParcelableSerializer.serializeToBytes(LastLocationCapsuleParcelable(lastFineLocation, lastCoarseLocation, lastFineLocationTimeCoarsed, lastCoarseLocationTimeCoarsed)))
        } catch (e: Exception) {
            Log.w(TAG, e)
            // Ignore
        }
    }

    companion object {
        private const val FILE_NAME = "last_location_capsule"
        private const val TIME_COARSE_CLIFF = 60_000L
        private const val EXTENSION_CLIFF = 30_000L

        private class LastLocationCapsuleParcelable(
            @Field(1) @JvmField val lastFineLocation: Location?,
            @Field(2) @JvmField val lastCoarseLocation: Location?,
            @Field(3) @JvmField val lastFineLocationTimeCoarsed: Location?,
            @Field(4) @JvmField val lastCoarseLocationTimeCoarsed: Location?
        ) : AutoSafeParcelable() {
            constructor() : this(null, null, null, null)

            companion object {
                @JvmField
                val CREATOR = AutoCreator(LastLocationCapsuleParcelable::class.java)
            }
        }
    }
}