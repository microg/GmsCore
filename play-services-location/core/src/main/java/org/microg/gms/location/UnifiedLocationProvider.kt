package org.microg.gms.location

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.microg.nlp.client.LocationClient
import org.microg.nlp.service.api.Constants
import org.microg.nlp.service.api.ILocationListener
import org.microg.nlp.service.api.LocationRequest
import java.io.PrintWriter
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class UnifiedLocationProvider(private val context: Context, private val changeListener: LocationChangeListener, private val lifecycle: Lifecycle): LifecycleOwner {
    private val client: LocationClient = LocationClient(context, lifecycle)
    private var lastLocation: Location? = null
    private val requests: MutableList<LocationRequestHelper> = ArrayList()
    private val activeRequestIds = hashSetOf<String>()
    private val activeRequestMutex = Mutex(false)
    private val listener: ILocationListener = object : ILocationListener.Stub() {
        override fun onLocation(statusCode: Int, location: Location?) {
            if (statusCode == Constants.STATUS_OK && location != null) {
                lastLocation = Location(location)
                try {
                    for (key in lastLocation?.extras?.keySet()?.toList().orEmpty()) {
                        if (key?.startsWith("org.microg.nlp.") == true) {
                            lastLocation?.extras?.remove(key)
                        }
                    }
                } catch (e:Exception){
                    // Sometimes we need to define the correct ClassLoader before unparcel(). Ignore those.
                }
                changeListener.onLocationChanged()
            }
        }
    }
    private var ready = false
    private val invokeOnceReady = hashSetOf<Runnable>()

    init {
        updateLastLocation()
    }

    private fun updateLastLocation() {
        lifecycleScope.launchWhenStarted {
            Log.d(TAG, "unified network: requesting last location")
            val lastLocation = client.getLastLocation()
            Log.d(TAG, "unified network: got last location: $lastLocation")
            if (lastLocation != null) {
                this@UnifiedLocationProvider.lastLocation = lastLocation
            }
            synchronized(invokeOnceReady) {
                for (runnable in invokeOnceReady) {
                    runnable.run()
                }
                ready = true
            }
        }
    }

    fun invokeOnceReady(runnable: Runnable) {
        synchronized(invokeOnceReady) {
            if (ready) runnable.run()
            else invokeOnceReady.add(runnable)
        }
    }

    fun addRequest(request: LocationRequestHelper) {
        Log.d(TAG, "unified network: addRequest $request")
        for (i in 0..requests.size) {
            if (i >= requests.size) break
            val req = requests[i]
            if (req.respondsTo(request.pendingIntent) || req.respondsTo(request.listener) || req.respondsTo(request.callback)) {
                requests.removeAt(i)
            }
        }
        requests.add(request)
        lifecycleScope.launchWhenStarted {
            updateConnection()
        }
    }

    fun removeRequest(request: LocationRequestHelper) {
        Log.d(TAG, "unified network: removeRequest $request")
        requests.remove(request)

        lifecycleScope.launchWhenStarted {
            updateConnection()
        }
    }

    fun getLastLocation(): Location? {
        if (lastLocation == null) {
            Log.d(TAG, "uh-ok: last location for unified network is null!")
        }
        return lastLocation
    }

    private suspend fun updateConnection() {
        activeRequestMutex.withLock {
            if (activeRequestIds.isNotEmpty() && requests.isEmpty()) {
                Log.d(TAG, "unified network: no longer requesting location update")
                for (id in activeRequestIds) {
                    client.cancelLocationRequestById(id)
                }
                activeRequestIds.clear()
            } else if (requests.isNotEmpty()) {
                val requests = ArrayList(requests).filter { it.isActive }
                for (id in activeRequestIds.filter { id -> requests.none { it.id == id } }) {
                    client.cancelLocationRequestById(id)
                }
                for (request in requests.filter { it.id !in activeRequestIds }) {
                    client.updateLocationRequest(LocationRequest(listener, request.locationRequest.intervalMillis, request.locationRequest.maxUpdates, request.id), Bundle().apply {
                        putString("packageName", request.packageName)
                        putString("source", "GoogleLocationManager")
                    })
                    activeRequestIds.add(request.id)
                }
            }
        }
    }

    override fun getLifecycle(): Lifecycle = lifecycle

    fun dump(writer: PrintWriter) {
        writer.println("network provider (via direct client):")
        writer.println("  last location: ${lastLocation?.let { Location(it) }}")
        writer.println("  ready: $ready")
    }

    companion object {
        const val TAG = "GmsLocProviderU"
    }
}
