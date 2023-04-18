/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.manager

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.SystemClock
import android.os.WorkSource
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.Granularity.GRANULARITY_FINE
import com.google.android.gms.location.Granularity.GRANULARITY_PERMISSION_LEVEL
import com.google.android.gms.location.ILocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.internal.ClientIdentity
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.microg.gms.location.GranularityUtil
import org.microg.gms.location.elapsedMillis
import org.microg.gms.utils.WorkSourceUtil
import java.io.PrintWriter

class LocationRequestManager(private val context: Context, private val lifecycle: Lifecycle, private val postProcessor: LocationPostProcessor, private val requestDetailsUpdatedCallback: () -> Unit) :
    IBinder.DeathRecipient, LifecycleOwner {
    private val lock = Mutex()
    private val binderRequests = mutableMapOf<IBinder, LocationRequestHolder>()
    private val pendingIntentRequests = mutableMapOf<PendingIntent, LocationRequestHolder>()
    var granularity: @Granularity Int = GRANULARITY_PERMISSION_LEVEL
        private set
    var intervalMillis: Long = Long.MAX_VALUE
        private set
    var workSource = WorkSource()
        private set
    private var requestDetailsUpdated = false
    private var checkingWhileFine = false

    override fun getLifecycle(): Lifecycle = lifecycle

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

    suspend fun add(binder: IBinder, clientIdentity: ClientIdentity, callback: ILocationCallback, request: LocationRequest) {
        lock.withLock {
            val holder = LocationRequestHolder(context, clientIdentity, request, callback, null)
            try {
                holder.start()
                binderRequests[binder] = holder
                binder.linkToDeath(this, 0)
            } catch (e: Exception) {
                holder.cancel()
            }
            recalculateRequests()
        }
        notifyRequestDetailsUpdated()
    }

    suspend fun update(oldBinder: IBinder, binder: IBinder, clientIdentity: ClientIdentity, callback: ILocationCallback, request: LocationRequest) {
        lock.withLock {
            oldBinder.unlinkToDeath(this, 0)
            val holder = binderRequests.remove(oldBinder)
            try {
                val startedHolder = holder?.update(callback, request) ?: LocationRequestHolder(context, clientIdentity, request, callback, null).start()
                binderRequests[binder] = startedHolder
                binder.linkToDeath(this, 0)
            } catch (e: Exception) {
                holder?.cancel()
            }
            recalculateRequests()
        }
        notifyRequestDetailsUpdated()
    }

    suspend fun remove(oldBinder: IBinder) {
        lock.withLock {
            oldBinder.unlinkToDeath(this, 0)
            if (binderRequests.remove(oldBinder) != null) recalculateRequests()
        }
        notifyRequestDetailsUpdated()
    }

    suspend fun add(pendingIntent: PendingIntent, clientIdentity: ClientIdentity, request: LocationRequest) {
        lock.withLock {
            try {
                pendingIntentRequests[pendingIntent] = LocationRequestHolder(context, clientIdentity, request, null, pendingIntent).start()
            } catch (e: Exception) {
                // Ignore
            }
            recalculateRequests()
        }
        notifyRequestDetailsUpdated()
    }

    suspend fun remove(pendingIntent: PendingIntent) {
        lock.withLock {
            if (pendingIntentRequests.remove(pendingIntent) != null) recalculateRequests()
        }
        notifyRequestDetailsUpdated()
    }

    private fun <T> processNewLocation(location: Location, map: Map<T, LocationRequestHolder>): Set<T> {
        val toRemove = mutableSetOf<T>()
        for ((key, holder) in map) {
            try {
                postProcessor.process(location, holder.effectiveGranularity, holder.clientIdentity.isGoogle(context))?.let {
                    holder.processNewLocation(it)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception while processing for ${holder.workSource}", e)
                toRemove.add(key)
            }
        }
        return toRemove
    }

    suspend fun processNewLocation(location: Location) {
        lock.withLock {
            val pendingIntentsToRemove = processNewLocation(location, pendingIntentRequests)
            for (pendingIntent in pendingIntentsToRemove) {
                pendingIntentRequests.remove(pendingIntent)
            }
            val bindersToRemove = processNewLocation(location, binderRequests)
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
        val newIntervalMillis = merged.minOfOrNull { it.intervalMillis } ?: Long.MAX_VALUE
        val newWorkSource = WorkSource()
        for (holder in merged) {
            newWorkSource.add(holder.workSource)
        }
        if (newGranularity == GRANULARITY_FINE && granularity != GRANULARITY_FINE) lifecycleScope.launchWhenStarted { checkWhileFine() }
        if (newGranularity != granularity || newIntervalMillis != intervalMillis || newWorkSource != workSource) {
            granularity = newGranularity
            intervalMillis = newIntervalMillis
            workSource = newWorkSource
            requestDetailsUpdated = true
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
            if (pendingIntentsToRemove.isNotEmpty() || bindersToRemove.isNotEmpty()) {
                recalculateRequests()
            }
        }
        notifyRequestDetailsUpdated()
    }

    private suspend fun checkWhileFine() {
        if (checkingWhileFine) return
        checkingWhileFine = true
        while (granularity == GRANULARITY_FINE) {
            check()
            delay(1000)
        }
        checkingWhileFine = false
    }

    private fun notifyRequestDetailsUpdated() {
        if (!requestDetailsUpdated) return
        requestDetailsUpdatedCallback()
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
        writer.println("Current location request (${GranularityUtil.granularityToString(granularity)}, ${intervalMillis}ms from ${workSource})")
        for (request in binderRequests.values.toList()) {
            writer.println("- ${request.workSource} ${request.intervalMillis}ms ${GranularityUtil.granularityToString(request.effectiveGranularity)} (pending: ${request.updatesPending} ${request.timePendingMillis}ms)")
        }
    }

    companion object {
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

            val permissionGranularity: @Granularity Int
                get() = context.granularityFromPermission(clientIdentity)
            val effectiveGranularity: @Granularity Int
                get() = getEffectiveGranularity(request.granularity, permissionGranularity)
            val intervalMillis: Long
                get() = request.intervalMillis
            val updatesPending: Int
                get() = request.maxUpdates - updates
            val timePendingMillis: Long
                get() = request.durationMillis - (SystemClock.elapsedRealtime() - start)
            var workSource: WorkSource = WorkSource(request.workSource).also { WorkSourceUtil.add(it, clientIdentity.uid, clientIdentity.packageName) }
                private set

            fun update(callback: ILocationCallback, request: LocationRequest): LocationRequestHolder {
                val changedGranularity = request.granularity != this.request.granularity
                if (changedGranularity) context.finishAppOpForEffectiveGranularity(clientIdentity, effectiveGranularity)
                this.callback = callback
                this.request = request
                this.start = SystemClock.elapsedRealtime()
                this.updates = 0
                this.workSource = WorkSource(request.workSource).also { WorkSourceUtil.add(it, clientIdentity.uid, clientIdentity.packageName) }
                if (changedGranularity && !context.startAppOpForEffectiveGranularity(clientIdentity, effectiveGranularity)) throw RuntimeException("Lack of permission")
                return this
            }

            fun start(): LocationRequestHolder {
                if (!context.startAppOpForEffectiveGranularity(clientIdentity, effectiveGranularity)) throw RuntimeException("Lack of permission")
                // TODO: Register app op watch
                return this
            }

            fun cancel() {
                try {
                    context.finishAppOpForEffectiveGranularity(clientIdentity, effectiveGranularity)
                    callback?.cancel()
                } catch (e: Exception) {
                    Log.w(TAG, e)
                }
            }

            fun check() {
                if (!context.checkAppOpForEffectiveGranularity(clientIdentity, effectiveGranularity)) throw RuntimeException("Lack of permission")
                if (timePendingMillis < 0) throw RuntimeException("duration limit reached (active for ${SystemClock.elapsedRealtime() - start}ms, duration ${request.durationMillis}ms)")
                if (updatesPending <= 0) throw RuntimeException("max updates reached")
                if (callback?.asBinder()?.isBinderAlive == false) throw RuntimeException("Binder died")
            }

            fun processNewLocation(location: Location) {
                check()
                if (lastLocation != null && location.elapsedMillis - lastLocation!!.elapsedMillis < request.minUpdateIntervalMillis) return
                if (lastLocation != null && location.distanceTo(lastLocation!!) < request.minUpdateDistanceMeters) return
                if (lastLocation == location) return
                val returnedLocation = if (effectiveGranularity > permissionGranularity) {
                    throw RuntimeException("lack of permission")
                } else {
                    if (!context.noteAppOpForEffectiveGranularity(clientIdentity, effectiveGranularity)) {
                        throw RuntimeException("app op denied")
                    } else if (clientIdentity.isSelfProcess()) {
                        // When the request is coming from us, we want to make sure to return a new object to not accidentally modify the internal state
                        Location(location)
                    } else {
                        location
                    }
                }
                val result = LocationResult.create(listOf(returnedLocation))
                callback?.onLocationResult(result)
                pendingIntent?.send(context, 0, Intent().apply { putExtra(LocationResult.EXTRA_LOCATION_RESULT, result) })
                updates++
                check()
            }

            init {
                require(callback != null || pendingIntent != null)
            }
        }
    }
}