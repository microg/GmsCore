package org.microg.gms.location

import android.content.Context
import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.microg.nlp.client.UnifiedLocationClient
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class UnifiedLocationProvider(context: Context?, changeListener: LocationChangeListener) {
    private val client: UnifiedLocationClient
    private var connectedMinTime: Long = 0
    private var lastLocation: Location? = null
    private val connected = AtomicBoolean(false)
    private val changeListener: LocationChangeListener
    private val requests: MutableList<LocationRequestHelper> = ArrayList()
    private val listener: UnifiedLocationClient.LocationListener = object : UnifiedLocationClient.LocationListener {
        override fun onLocation(location: Location) {
            lastLocation = location
            changeListener.onLocationChanged()
        }
    }
    private var ready = false
    private val invokeOnceReady = hashSetOf<Runnable>()

    private fun updateLastLocation() {
        GlobalScope.launch(Dispatchers.Main) {
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
        updateConnection()
    }

    fun removeRequest(request: LocationRequestHelper) {
        Log.d(TAG, "unified network: removeRequest $request")
        requests.remove(request)
        updateConnection()
    }

    fun getLastLocation(): Location? {
        if (lastLocation == null) {
            Log.d(TAG, "uh-ok: last location for unified network is null!")
        }
        return lastLocation
    }

    @Synchronized
    private fun updateConnection() {
        if (connected.get() && requests.isEmpty()) {
            Log.d(TAG, "unified network: no longer requesting location update")
            client.removeLocationUpdates(listener)
            connected.set(false)
        } else if (!requests.isEmpty()) {
            var minTime = Long.MAX_VALUE
            val sb = StringBuilder()
            var opPackageName: String? = null
            for (request in requests) {
                if (request.locationRequest.interval < minTime) {
                    opPackageName = request.packageName
                    minTime = request.locationRequest.interval
                }
                if (sb.isNotEmpty()) sb.append(", ")
                sb.append("${request.packageName}:${request.locationRequest.interval}ms")
            }
            client.opPackageName = opPackageName
            Log.d(TAG, "unified network: requesting location updates with interval ${minTime}ms ($sb)")
            if (!connected.get() || connectedMinTime != minTime) {
                client.requestLocationUpdates(listener, minTime)
            }
            connected.set(true)
            connectedMinTime = minTime
        }
    }

    companion object {
        const val TAG = "GmsLocProviderU"
    }

    init {
        client = UnifiedLocationClient[context!!]
        this.changeListener = changeListener
        updateLastLocation()
    }
}
