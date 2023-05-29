/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.manager

import android.Manifest
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build.VERSION.SDK_INT
import android.os.IBinder
import android.util.Log
import androidx.core.content.getSystemService
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.android.gms.location.Granularity.GRANULARITY_COARSE
import com.google.android.gms.location.Granularity.GRANULARITY_FINE
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.internal.ClientIdentity
import org.microg.gms.location.GranularityUtil
import org.microg.gms.location.elapsedMillis
import org.microg.gms.location.network.NetworkLocationService
import org.microg.gms.utils.IntentCacheManager
import java.io.PrintWriter
import kotlin.math.max
import kotlin.math.min
import android.location.LocationManager as SystemLocationManager

class LocationManager(private val context: Context, private val lifecycle: Lifecycle) : LifecycleOwner {
    private var coarsePendingIntent: PendingIntent? = null
    private val postProcessor by lazy { LocationPostProcessor() }
    private val lastLocationCapsule by lazy { LastLocationCapsule(context) }
    val database by lazy { LocationAppsDatabase(context) }
    private val requestManager by lazy { LocationRequestManager(context, lifecycle, postProcessor, database) { onRequestManagerUpdated() } }
    private val gpsLocationListener by lazy { LocationListenerCompat { updateGpsLocation(it) } }

    val deviceOrientationManager = DeviceOrientationManager(context, lifecycle)

    override fun getLifecycle(): Lifecycle = lifecycle

    var started: Boolean = false
        private set

    suspend fun getLastLocation(clientIdentity: ClientIdentity, request: LastLocationRequest): Location? {
        if (request.maxUpdateAgeMillis < 0) throw IllegalArgumentException()
        GranularityUtil.checkValidGranularity(request.granularity)
        if (request.isBypass) {
            val permission = if (SDK_INT >= 33) "android.permission.LOCATION_BYPASS" else Manifest.permission.WRITE_SECURE_SETTINGS
            if (context.checkPermission(permission, clientIdentity.pid, clientIdentity.uid) != PackageManager.PERMISSION_GRANTED) {
                throw SecurityException("Caller must hold $permission for location bypass")
            }
        }
        if (request.impersonation != null) {
            Log.w(TAG, "${clientIdentity.packageName} wants to impersonate ${request.impersonation!!.packageName}. Ignoring.")
        }
        val permissionGranularity = context.granularityFromPermission(clientIdentity)
        var effectiveGranularity = getEffectiveGranularity(request.granularity, permissionGranularity)
        if (effectiveGranularity == GRANULARITY_FINE && database.getForceCoarse(clientIdentity.packageName)) effectiveGranularity = GRANULARITY_COARSE
        val returnedLocation = if (effectiveGranularity > permissionGranularity) {
            // No last location available at requested granularity due to lack of permission
            null
        } else {
            val preLocation = lastLocationCapsule.getLocation(effectiveGranularity, request.maxUpdateAgeMillis)
            val processedLocation = postProcessor.process(preLocation, effectiveGranularity, clientIdentity.isGoogle(context))
            if (!context.noteAppOpForEffectiveGranularity(clientIdentity, effectiveGranularity)) {
                // App Op denied
                null
            } else if (processedLocation != null && clientIdentity.isSelfProcess()) {
                // When the request is coming from us, we want to make sure to return a new object to not accidentally modify the internal state
                Location(processedLocation)
            } else {
                processedLocation
            }
        }
        database.noteAppLocation(clientIdentity.packageName, returnedLocation)
        return returnedLocation?.let { Location(it).apply { provider = "fused" } }
    }

    fun getLocationAvailability(clientIdentity: ClientIdentity, request: LocationAvailabilityRequest): LocationAvailability {
        if (request.bypass) {
            val permission = if (SDK_INT >= 33) "android.permission.LOCATION_BYPASS" else Manifest.permission.WRITE_SECURE_SETTINGS
            if (context.checkPermission(permission, clientIdentity.pid, clientIdentity.uid) != PackageManager.PERMISSION_GRANTED) {
                throw SecurityException("Caller must hold $permission for location bypass")
            }
        }
        if (request.impersonation != null) {
            Log.w(TAG, "${clientIdentity.packageName} wants to impersonate ${request.impersonation!!.packageName}. Ignoring.")
        }
        return lastLocationCapsule.locationAvailability
    }

    suspend fun addBinderRequest(clientIdentity: ClientIdentity, binder: IBinder, callback: ILocationCallback, request: LocationRequest) {
        request.verify(context, clientIdentity)
        requestManager.add(binder, clientIdentity, callback, request, lastLocationCapsule)
    }

    suspend fun updateBinderRequest(
        clientIdentity: ClientIdentity,
        oldBinder: IBinder,
        binder: IBinder,
        callback: ILocationCallback,
        request: LocationRequest
    ) {
        request.verify(context, clientIdentity)
        requestManager.update(oldBinder, binder, clientIdentity, callback, request, lastLocationCapsule)
    }

    suspend fun removeBinderRequest(binder: IBinder) {
        requestManager.remove(binder)
    }

    suspend fun addIntentRequest(clientIdentity: ClientIdentity, pendingIntent: PendingIntent, request: LocationRequest) {
        request.verify(context, clientIdentity)
        requestManager.add(pendingIntent, clientIdentity, request, lastLocationCapsule)
    }

    suspend fun removeIntentRequest(pendingIntent: PendingIntent) {
        requestManager.remove(pendingIntent)
    }

    fun start() {
        synchronized(this) {
            if (started) return
            started = true
        }
        val intent = Intent(context, LocationManagerService::class.java)
        intent.action = NetworkLocationService.ACTION_REPORT_LOCATION
        coarsePendingIntent = PendingIntent.getService(context, 0, intent, (if (SDK_INT >= 31) FLAG_MUTABLE else 0) or FLAG_UPDATE_CURRENT)
        lastLocationCapsule.start()
        requestManager.start()
    }

    fun stop() {
        synchronized(this) {
            if (!started) return
            started = false
        }
        requestManager.stop()
        lastLocationCapsule.stop()
        deviceOrientationManager.stop()

        val intent = Intent(context, NetworkLocationService::class.java)
        intent.putExtra(NetworkLocationService.EXTRA_PENDING_INTENT, coarsePendingIntent)
        intent.putExtra(NetworkLocationService.EXTRA_ENABLE, false)
        context.startService(intent)

        val locationManager = context.getSystemService<SystemLocationManager>() ?: return
        try {
            LocationManagerCompat.removeUpdates(locationManager, gpsLocationListener)
        } catch (e: SecurityException) {
            // Ignore
        }
    }

    private fun onRequestManagerUpdated() {
        val networkInterval = when (requestManager.granularity) {
            GRANULARITY_COARSE -> max(requestManager.intervalMillis, MAX_COARSE_UPDATE_INTERVAL)
            GRANULARITY_FINE -> max(requestManager.intervalMillis, MAX_FINE_UPDATE_INTERVAL)
            else -> Long.MAX_VALUE
        }
        val gpsInterval = when (requestManager.priority to requestManager.granularity) {
            PRIORITY_HIGH_ACCURACY to GRANULARITY_FINE -> requestManager.intervalMillis
            else -> Long.MAX_VALUE
        }

        val intent = Intent(context, NetworkLocationService::class.java)
        intent.putExtra(NetworkLocationService.EXTRA_PENDING_INTENT, coarsePendingIntent)
        intent.putExtra(NetworkLocationService.EXTRA_ENABLE, true)
        intent.putExtra(NetworkLocationService.EXTRA_INTERVAL_MILLIS, networkInterval)
        intent.putExtra(NetworkLocationService.EXTRA_LOW_POWER, requestManager.granularity <= GRANULARITY_COARSE)
        intent.putExtra(NetworkLocationService.EXTRA_WORK_SOURCE, requestManager.workSource)
        context.startService(intent)

        val locationManager = context.getSystemService<SystemLocationManager>() ?: return
        if (gpsInterval != Long.MAX_VALUE) {
            try {
                LocationManagerCompat.requestLocationUpdates(
                    locationManager,
                    SystemLocationManager.GPS_PROVIDER,
                    LocationRequestCompat.Builder(gpsInterval).build(),
                    gpsLocationListener,
                    context.mainLooper
                )
            } catch (e: SecurityException) {
                // Ignore
            }
        } else {
            try {
                LocationManagerCompat.removeUpdates(locationManager, gpsLocationListener)
            } catch (e: SecurityException) {
                // Ignore
            }
        }
    }

    fun updateNetworkLocation(location: Location) {
        val lastLocation = lastLocationCapsule.getLocation(GRANULARITY_FINE, Long.MAX_VALUE)

        // Ignore outdated location
        if (lastLocation != null && location.elapsedMillis + UPDATE_CLIFF_MS < lastLocation.elapsedMillis) return

        if (lastLocation == null ||
            lastLocation.accuracy > location.accuracy ||
            lastLocation.elapsedMillis + min(requestManager.intervalMillis * 2, UPDATE_CLIFF_MS) < location.elapsedMillis ||
            lastLocation.accuracy + ((location.elapsedMillis - lastLocation.elapsedMillis) / 1000.0) > location.accuracy
        ) {
            lastLocationCapsule.updateCoarseLocation(location)
            sendNewLocation()
        }
    }

    fun updateGpsLocation(location: Location) {
        lastLocationCapsule.updateFineLocation(location)
        sendNewLocation()
    }

    fun sendNewLocation() {
        lifecycleScope.launchWhenStarted {
            requestManager.processNewLocation(lastLocationCapsule)
        }
        lastLocationCapsule.getLocation(GRANULARITY_FINE, Long.MAX_VALUE)?.let { deviceOrientationManager.onLocationChanged(it) }
    }

    fun dump(writer: PrintWriter) {
        writer.println("Location availability: ${lastLocationCapsule.locationAvailability}")
        writer.println(
            "Last coarse location: ${
                postProcessor.process(
                    lastLocationCapsule.getLocation(GRANULARITY_COARSE, Long.MAX_VALUE),
                    GRANULARITY_COARSE,
                    true
                )
            }"
        )
        writer.println(
            "Last fine location: ${
                postProcessor.process(
                    lastLocationCapsule.getLocation(GRANULARITY_FINE, Long.MAX_VALUE),
                    GRANULARITY_FINE,
                    true
                )
            }"
        )
        requestManager.dump(writer)
        deviceOrientationManager.dump(writer)
    }

    fun handleCacheIntent(intent: Intent) {
        when (IntentCacheManager.getType(intent)) {
            LocationRequestManager.CACHE_TYPE -> {
                requestManager.handleCacheIntent(intent)
            }

            else -> {
                Log.w(TAG, "Unknown cache intent: $intent")
            }
        }
    }

    companion object {
        const val MAX_COARSE_UPDATE_INTERVAL = 20_000L
        const val MAX_FINE_UPDATE_INTERVAL = 10_000L
        const val EXTENSION_CLIFF_MS = 10_000L
        const val UPDATE_CLIFF_MS = 30_000L
    }
}