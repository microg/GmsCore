/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.manager

import android.Manifest
import android.app.AppOpsManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.*
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.android.gms.location.Granularity.*
import com.google.android.gms.location.Priority.*
import com.google.android.gms.location.internal.ClientIdentity
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.microg.gms.location.GranularityUtil
import org.microg.gms.location.PriorityUtil
import org.microg.gms.location.elapsedMillis
import org.microg.gms.location.formatDuration
import org.microg.gms.utils.IntentCacheManager
import org.microg.gms.utils.WorkSourceUtil
import java.io.PrintWriter
import kotlin.math.max

class LocationRequestManager(private val context: Context, override val lifecycle: Lifecycle, private val postProcessor: LocationPostProcessor, private val database: LocationAppsDatabase = LocationAppsDatabase(context), private val requestDetailsUpdatedCallback: () -> Unit) :
    IBinder.DeathRecipient, LifecycleOwner {
    private val lock = Mutex()
    private val binderRequests = mutableMapOf<IBinder, LocationRequestHolder>()
    private val pendingIntentRequests = mutableMapOf<PendingIntent, LocationRequestHolder>()
    private val cacheManager by lazy { IntentCacheManager.create<LocationManagerService, LocationRequestHolderParcelable>(context, CACHE_TYPE) }
    var priority: @Priority Int = PRIORITY_PASSIVE
    var granularity: @Granularity Int = GRANULARITY_PERMISSION_LEVEL
        private set
    var intervalMillis: Long = Long.MAX_VALUE
        private set
    var workSource = WorkSource()
        private set
    private var grantedPermissions: List<Int> = locationPermissions.map { ContextCompat.checkSelfPermission(context, it) }
    private var permissionChanged: Boolean = false
    private var requestDetailsUpdated = false
    private var checkingWhileHighAccuracy = false

    private val appOpsLock = Any()
    @GuardedBy("appOpsLock")
    private var currentAppOps = emptyMap<ClientIdentity, Boolean>()

    override fun binderDied() {
        lifecycleScope.launchWhenStarted {
            lock.withLock {
                val toRemove = binderRequests.keys.filter { !it.isBinderAlive }.toList()
                for (binder in toRemove) {
                    binderRequests.remove(binder)
                }
                recalculateRequests()
            }
            notifyRequestDetailsUpdated()
        }
    }

    suspend fun add(binder: IBinder, clientIdentity: ClientIdentity, callback: ILocationCallback, request: LocationRequest, lastLocationCapsule: LastLocationCapsule) {
        update(null, binder, clientIdentity, callback, request, lastLocationCapsule)
    }

    suspend fun update(oldBinder: IBinder?, binder: IBinder, clientIdentity: ClientIdentity, callback: ILocationCallback, request: LocationRequest, lastLocationCapsule: LastLocationCapsule): Boolean {
        var new = false
        lock.withLock {
            try {
                oldBinder?.unlinkToDeath(this, 0)
            } catch (e: Exception) {
                Log.w(TAG, "update: ", e)
            }
            val holder = binderRequests.remove(oldBinder)
            new = holder == null
            try {
                val startedHolder = holder?.update(callback, request) ?: LocationRequestHolder(context, clientIdentity, request, callback, null).start().also {
                    var effectiveGranularity = it.effectiveGranularity
                    if (effectiveGranularity == GRANULARITY_FINE && database.getForceCoarse(it.clientIdentity.packageName)) effectiveGranularity = GRANULARITY_COARSE
                    val lastLocation = lastLocationCapsule.getLocation(effectiveGranularity, request.maxUpdateAgeMillis)
                    if (lastLocation != null) it.processNewLocation(lastLocation)
                }
                binderRequests[binder] = startedHolder
                binder.linkToDeath(this, 0)
            } catch (e: Exception) {
                holder?.cancel()
            }
            recalculateRequests()
        }
        notifyRequestDetailsUpdated()
        return new
    }

    suspend fun remove(oldBinder: IBinder) {
        lock.withLock {
            oldBinder.unlinkToDeath(this, 0)
            val holder = binderRequests.remove(oldBinder)
            if (holder != null) {
                holder.cancel()
                recalculateRequests()
            }
        }
        notifyRequestDetailsUpdated()
    }

    suspend fun add(pendingIntent: PendingIntent, clientIdentity: ClientIdentity, request: LocationRequest, lastLocationCapsule: LastLocationCapsule) {
        lock.withLock {
            try {
                pendingIntentRequests[pendingIntent] = LocationRequestHolder(context, clientIdentity, request, null, pendingIntent).start().also {
                    cacheManager.add(it.asParcelable()) { it.pendingIntent == pendingIntent }
                    var effectiveGranularity = it.effectiveGranularity
                    if (effectiveGranularity == GRANULARITY_FINE && database.getForceCoarse(it.clientIdentity.packageName)) effectiveGranularity = GRANULARITY_COARSE
                    val lastLocation = lastLocationCapsule.getLocation(effectiveGranularity, request.maxUpdateAgeMillis)
                    if (lastLocation != null) it.processNewLocation(lastLocation)
                }
            } catch (e: Exception) {
                // Ignore
            }
            recalculateRequests()
        }
        notifyRequestDetailsUpdated()
    }

    suspend fun remove(pendingIntent: PendingIntent) {
        lock.withLock {
            cacheManager.removeIf { it.pendingIntent == pendingIntent }
            if (pendingIntentRequests.remove(pendingIntent) != null) recalculateRequests()
        }
        notifyRequestDetailsUpdated()
    }

    private fun <T> processNewLocation(lastLocationCapsule: LastLocationCapsule, map: Map<T, LocationRequestHolder>): Pair<Set<T>, Set<T>> {
        val toRemove = mutableSetOf<T>()
        val updated = mutableSetOf<T>()
        for ((key, holder) in map) {
            try {
                var effectiveGranularity = holder.effectiveGranularity
                if (effectiveGranularity == GRANULARITY_FINE && database.getForceCoarse(holder.clientIdentity.packageName)) effectiveGranularity = GRANULARITY_COARSE
                val location = lastLocationCapsule.getLocation(effectiveGranularity, holder.maxUpdateDelayMillis)
                postProcessor.process(location, effectiveGranularity, holder.clientIdentity.isGoogle(context))?.let {
                    if (holder.processNewLocation(it)) {
                        database.noteAppLocation(holder.clientIdentity.packageName, it)
                        updated.add(key)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception while processing for ${holder.workSource}: ${e.message}")
                toRemove.add(key)
            }
        }
        return toRemove to updated
    }

    suspend fun processNewLocation(lastLocationCapsule: LastLocationCapsule) {
        lock.withLock {
            val (pendingIntentsToRemove, pendingIntentsUpdated) = processNewLocation(lastLocationCapsule, pendingIntentRequests)
            for (pendingIntent in pendingIntentsToRemove) {
                cacheManager.removeIf { it.pendingIntent == pendingIntent }
                pendingIntentRequests.remove(pendingIntent)
            }
            for (pendingIntent in pendingIntentsUpdated) {
                cacheManager.add(pendingIntentRequests[pendingIntent]!!.asParcelable()) { it.pendingIntent == pendingIntent }
            }
            val (bindersToRemove, _) = processNewLocation(lastLocationCapsule, binderRequests)
            for (binder in bindersToRemove) {
                try {
                    binderRequests[binder]?.cancel()
                } catch (e: Exception) {
                    // Ignore
                }
                binderRequests.remove(binder)
            }
            if (pendingIntentsToRemove.isNotEmpty() || bindersToRemove.isNotEmpty()) {
                recalculateRequests()
            }
        }
        notifyRequestDetailsUpdated()
    }

    private fun recalculateRequests() {
        val merged = binderRequests.values + pendingIntentRequests.values
        val newGranularity = merged.maxOfOrNull { it.effectiveGranularity } ?: GRANULARITY_PERMISSION_LEVEL
        val newPriority = merged.minOfOrNull { it.effectivePriority } ?: PRIORITY_PASSIVE
        val newIntervalMillis = merged.minOfOrNull { it.intervalMillis } ?: Long.MAX_VALUE
        val newWorkSource = WorkSource()
        for (holder in merged) {
            newWorkSource.add(holder.workSource)
        }
        if (newPriority == PRIORITY_HIGH_ACCURACY && priority != PRIORITY_HIGH_ACCURACY) lifecycleScope.launchWhenStarted { checkWhileHighAccuracy() }
        if (newPriority != priority || newGranularity != granularity || newIntervalMillis != intervalMillis || newWorkSource != workSource || permissionChanged) {
            priority = newPriority
            granularity = newGranularity
            intervalMillis = newIntervalMillis
            workSource = newWorkSource
            requestDetailsUpdated = true
            permissionChanged = false
        }
    }

    private fun updateAppOps() {
        synchronized(appOpsLock) {
            val newAppOps = mutableMapOf<ClientIdentity, Boolean>()
            val merged = binderRequests.values + pendingIntentRequests.values
            for (request in merged) {
                if (request.effectivePriority >= PRIORITY_PASSIVE) continue
                if (!newAppOps.containsKey(request.clientIdentity)) {
                    newAppOps[request.clientIdentity] = request.effectiveHighPower
                } else if (request.effectiveHighPower) {
                    newAppOps[request.clientIdentity] = true
                }
            }
            Log.d(TAG, "Updating app ops for location requests, change attribution to: ${newAppOps.keys.map { it.packageName }.joinToString().takeIf { it.isNotEmpty() } ?: "none"}")

            for (oldAppOp in currentAppOps) {
                context.finishAppOp(AppOpsManager.OPSTR_MONITOR_LOCATION, oldAppOp.key)
                if (oldAppOp.value) {
                    context.finishAppOp(AppOpsManager.OPSTR_MONITOR_HIGH_POWER_LOCATION, oldAppOp.key)
                }
            }
            for (newAppOp in newAppOps) {
                context.startAppOp(AppOpsManager.OPSTR_MONITOR_LOCATION, newAppOp.key)
                if (newAppOp.value) {
                    context.startAppOp(AppOpsManager.OPSTR_MONITOR_HIGH_POWER_LOCATION, newAppOp.key)
                }
            }
            currentAppOps = newAppOps
        }
    }

    private suspend fun check() {
        lock.withLock {
            val pendingIntentsToRemove = mutableSetOf<PendingIntent>()
            for ((key, holder) in pendingIntentRequests) {
                try {
                    holder.check()
                } catch (e: Exception) {
                    Log.w(TAG, "Exception while checking for ${holder.workSource}", e)
                    pendingIntentsToRemove.add(key)
                }
            }
            for (pendingIntent in pendingIntentsToRemove) {
                cacheManager.removeIf { it.pendingIntent == pendingIntent }
                pendingIntentRequests.remove(pendingIntent)
            }
            val bindersToRemove = mutableSetOf<IBinder>()
            for ((key, holder) in binderRequests) {
                try {
                    holder.check()
                } catch (e: Exception) {
                    Log.w(TAG, "Exception while checking for ${holder.workSource}", e)
                    bindersToRemove.add(key)
                }
            }
            for (binder in bindersToRemove) {
                try {
                    binderRequests[binder]?.cancel()
                } catch (e: Exception) {
                    // Ignore
                }
                binderRequests.remove(binder)
            }
            if (grantedPermissions.any { it != PackageManager.PERMISSION_GRANTED }) {
                val grantedPermissions = locationPermissions.map { ContextCompat.checkSelfPermission(context, it) }
                if (grantedPermissions == this.grantedPermissions) {
                    this.grantedPermissions = grantedPermissions
                    permissionChanged = true
                }
            }
            if (pendingIntentsToRemove.isNotEmpty() || bindersToRemove.isNotEmpty() || permissionChanged) {
                recalculateRequests()
            }
        }
        notifyRequestDetailsUpdated()
    }

    private suspend fun checkWhileHighAccuracy() {
        if (checkingWhileHighAccuracy) return
        checkingWhileHighAccuracy = true
        while (priority == PRIORITY_HIGH_ACCURACY) {
            check()
            delay(1000)
        }
        checkingWhileHighAccuracy = false
    }

    private fun notifyRequestDetailsUpdated() {
        if (!requestDetailsUpdated) return
        requestDetailsUpdatedCallback()
        updateAppOps()
        requestDetailsUpdated = false
    }

    fun stop() {
        binderRequests.clear()
        pendingIntentRequests.clear()
        recalculateRequests()
    }

    fun start() {
        recalculateRequests()
        notifyRequestDetailsUpdated()
    }

    fun dump(writer: PrintWriter) {
        writer.println("Request cache: id=${cacheManager.getId()} size=${cacheManager.getEntries().size}")
        writer.println("Current location request (${GranularityUtil.granularityToString(granularity)}, ${PriorityUtil.priorityToString(priority)}, ${intervalMillis.formatDuration()} from ${workSource})")
        for (request in binderRequests.values.toList()) {
            writer.println("- bound ${request.workSource} ${request.intervalMillis.formatDuration()} ${GranularityUtil.granularityToString(request.effectiveGranularity)}, ${PriorityUtil.priorityToString(request.effectivePriority)} (pending: ${request.updatesPending.let { if (it == Int.MAX_VALUE) "\u221e" else "$it" }} ${request.timePendingMillis.formatDuration()}) app-op: ${when(currentAppOps[request.clientIdentity]) { null -> "false"; false -> "low"; true -> "high"}}")
        }
        for (request in pendingIntentRequests.values.toList()) {
            writer.println("- pending intent ${request.workSource} ${request.intervalMillis.formatDuration()} ${GranularityUtil.granularityToString(request.effectiveGranularity)}, ${PriorityUtil.priorityToString(request.effectivePriority)} (pending: ${request.updatesPending.let { if (it == Int.MAX_VALUE) "\u221e" else "$it" }} ${request.timePendingMillis.formatDuration()}) app-op: ${when(currentAppOps[request.clientIdentity]) { null -> "false"; false -> "low"; true -> "high"}}")
        }
    }

    fun handleCacheIntent(intent: Intent) {
        cacheManager.processIntent(intent)
        for (parcelable in cacheManager.getEntries()) {
            pendingIntentRequests[parcelable.pendingIntent] = LocationRequestHolder(context, parcelable)
        }
        recalculateRequests()
        notifyRequestDetailsUpdated()
    }

    companion object {
        private val locationPermissions = listOfNotNull(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            if (SDK_INT >= 29) Manifest.permission.ACCESS_BACKGROUND_LOCATION else null
        )
        const val CACHE_TYPE = 1

        private class LocationRequestHolderParcelable(
            val clientIdentity: ClientIdentity,
            val request: LocationRequest,
            val pendingIntent: PendingIntent,
            val start: Long,
            val updates: Int,
            val lastLocation: Location?
        ) : Parcelable {
            constructor(parcel: Parcel) : this(
                parcel.readParcelable(ClientIdentity::class.java.classLoader)!!,
                parcel.readParcelable(LocationRequest::class.java.classLoader)!!,
                parcel.readParcelable(PendingIntent::class.java.classLoader)!!,
                parcel.readLong(),
                parcel.readInt(),
                parcel.readParcelable(Location::class.java.classLoader)
            )

            override fun writeToParcel(parcel: Parcel, flags: Int) {
                parcel.writeParcelable(clientIdentity, flags)
                parcel.writeParcelable(request, flags)
                parcel.writeParcelable(pendingIntent, flags)
                parcel.writeLong(start)
                parcel.writeInt(updates)
                parcel.writeParcelable(lastLocation, flags)
            }

            override fun describeContents(): Int {
                return 0
            }

            companion object CREATOR : Parcelable.Creator<LocationRequestHolderParcelable> {
                override fun createFromParcel(parcel: Parcel): LocationRequestHolderParcelable {
                    return LocationRequestHolderParcelable(parcel)
                }

                override fun newArray(size: Int): Array<LocationRequestHolderParcelable?> {
                    return arrayOfNulls(size)
                }
            }
        }

        private class LocationRequestHolder(
            private val context: Context,
            val clientIdentity: ClientIdentity,
            private var request: LocationRequest,
            private var callback: ILocationCallback?,
            private val pendingIntent: PendingIntent?
        ) {
            private var start = SystemClock.elapsedRealtime()
            private var updates = 0
            private var lastLocation: Location? = null

            constructor(context: Context, parcelable: LocationRequestHolderParcelable) : this(context, parcelable.clientIdentity, parcelable.request, null, parcelable.pendingIntent) {
                start = parcelable.start
                updates = parcelable.updates
                lastLocation = parcelable.lastLocation
            }

            fun asParcelable() = LocationRequestHolderParcelable(clientIdentity, request, pendingIntent!!, start, updates, lastLocation)

            val permissionGranularity: @Granularity Int
                get() = context.granularityFromPermission(clientIdentity)
            val effectiveGranularity: @Granularity Int
                get() = getEffectiveGranularity(request.granularity, permissionGranularity)
            val effectivePriority: @Priority Int
                get() {
                    if (request.priority == PRIORITY_HIGH_ACCURACY && permissionGranularity < GRANULARITY_FINE) {
                        return PRIORITY_BALANCED_POWER_ACCURACY
                    }
                    return request.priority
                }
            val maxUpdateDelayMillis: Long
                get() = max(max(request.maxUpdateDelayMillis, intervalMillis), 0L)
            val intervalMillis: Long
                get() = request.intervalMillis
            val updatesPending: Int
                get() = request.maxUpdates - updates
            val timePendingMillis: Long
                get() = request.durationMillis - (SystemClock.elapsedRealtime() - start)
            var workSource: WorkSource = WorkSource(request.workSource).also { WorkSourceUtil.add(it, clientIdentity.uid, clientIdentity.packageName) }
                private set
            val effectiveHighPower: Boolean
                get() = request.intervalMillis < 60000 || effectivePriority == PRIORITY_HIGH_ACCURACY

            fun update(callback: ILocationCallback, request: LocationRequest): LocationRequestHolder {
                val changedGranularity = request.granularity != this.request.granularity || request.granularity == GRANULARITY_PERMISSION_LEVEL
                this.callback = callback
                this.request = request
                this.start = SystemClock.elapsedRealtime()
                this.updates = 0
                this.workSource = WorkSource(request.workSource).also { WorkSourceUtil.add(it, clientIdentity.uid, clientIdentity.packageName) }
                if (changedGranularity) {
                    if (!context.checkAppOpForEffectiveGranularity(clientIdentity, effectiveGranularity)) throw RuntimeException("Lack of permission")
                }
                return this
            }

            fun start(): LocationRequestHolder {
                if (!context.checkAppOpForEffectiveGranularity(clientIdentity, effectiveGranularity)) throw RuntimeException("Lack of permission")
                return this
            }

            fun cancel() {
                try {
                    callback?.cancel()
                } catch (e: Exception) {
                    Log.w(TAG, e)
                }
            }

            fun check() {
                if (!context.checkAppOpForEffectiveGranularity(clientIdentity, effectiveGranularity)) throw RuntimeException("Lack of permission")
                if (effectiveGranularity > permissionGranularity) throw RuntimeException("Lack of permission")
                if (timePendingMillis < 0) throw RuntimeException("duration limit reached (active for ${(SystemClock.elapsedRealtime() - start).formatDuration()}, duration ${request.durationMillis.formatDuration()})")
                if (updatesPending <= 0) throw RuntimeException("max updates reached")
                if (callback?.asBinder()?.isBinderAlive == false) throw RuntimeException("Binder died")
            }

            fun processNewLocation(location: Location): Boolean {
                check()
                if (lastLocation != null && location.elapsedMillis - lastLocation!!.elapsedMillis < request.minUpdateIntervalMillis) return false
                if (lastLocation != null && location.distanceTo(lastLocation!!) < request.minUpdateDistanceMeters) return false
                if (lastLocation == location) return false
                val returnedLocation = if (effectiveGranularity > permissionGranularity) {
                    throw RuntimeException("Lack of permission")
                } else {
                    if (!context.noteAppOpForEffectiveGranularity(clientIdentity, effectiveGranularity)) {
                        throw RuntimeException("app op denied")
                    } else if (clientIdentity.isSelfProcess()) {
                        Location(location)
                    } else {
                        Location(location).apply { provider = "fused" }
                    }
                }
                val result = LocationResult.create(listOf(returnedLocation))
                callback?.onLocationResult(result)
                pendingIntent?.send(context, 0, Intent().apply { putExtra(LocationResult.EXTRA_LOCATION_RESULT, result) })
                if (request.maxUpdates != Int.MAX_VALUE) updates++
                check()
                return true
            }

            init {
                require(callback != null || pendingIntent != null)
            }
        }
    }
}