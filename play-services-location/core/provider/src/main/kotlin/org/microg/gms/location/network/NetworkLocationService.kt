/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.os.WorkSource
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.collection.LruCache
import androidx.core.content.getSystemService
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.android.volley.VolleyError
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.microg.gms.location.*
import org.microg.gms.location.network.LocationCacheDatabase.Companion.NEGATIVE_CACHE_ENTRY
import org.microg.gms.location.network.cell.CellDetails
import org.microg.gms.location.network.cell.CellDetailsCallback
import org.microg.gms.location.network.cell.CellDetailsSource
import org.microg.gms.location.network.mozilla.MozillaLocationServiceClient
import org.microg.gms.location.network.mozilla.ServiceException
import org.microg.gms.location.network.wifi.*
import java.io.FileDescriptor
import java.io.PrintWriter
import java.lang.Math.pow
import java.nio.ByteBuffer
import java.util.LinkedList
import kotlin.math.min

class NetworkLocationService : LifecycleService(), WifiDetailsCallback, CellDetailsCallback {
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler

    @GuardedBy("activeRequests")
    private val activeRequests = HashSet<NetworkLocationRequest>()
    private val highPowerScanRunnable = Runnable { this.scan(false) }
    private val lowPowerScanRunnable = Runnable { this.scan(true) }
    private var wifiDetailsSource: WifiDetailsSource? = null
    private var cellDetailsSource: CellDetailsSource? = null
    private val mozilla by lazy { MozillaLocationServiceClient(this) }
    private val cache by lazy { LocationCacheDatabase(this) }
    private val movingWifiHelper by lazy { MovingWifiHelper(this) }
    private val settings by lazy { LocationSettings(this) }
    private val wifiScanCache = LruCache<String?, Location>(100)

    private var lastHighPowerScanRealtime = 0L
    private var lastLowPowerScanRealtime = 0L
    private var highPowerIntervalMillis = Long.MAX_VALUE
    private var lowPowerIntervalMillis = Long.MAX_VALUE

    private var lastWifiDetailsRealtimeMillis = 0L
    private var lastCellDetailsRealtimeMillis = 0L

    private val locationLock = Any()
    private var lastWifiLocation: Location? = null
    private var lastCellLocation: Location? = null
    private var lastLocation: Location? = null

    private val gpsLocationListener by lazy { LocationListenerCompat { onNewGpsLocation(it) } }

    @GuardedBy("gpsLocationBuffer")
    private val gpsLocationBuffer = LinkedList<Location>()

    private val interval: Long
        get() = min(highPowerIntervalMillis, lowPowerIntervalMillis)

    override fun onCreate() {
        super.onCreate()
        handlerThread = HandlerThread(NetworkLocationService::class.java.simpleName)
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        wifiDetailsSource = WifiDetailsSource.create(this, this).apply { enable() }
        cellDetailsSource = CellDetailsSource.create(this, this).apply { enable() }
        try {
            getSystemService<LocationManager>()?.let { locationManager ->
                LocationManagerCompat.requestLocationUpdates(
                    locationManager,
                    LocationManager.GPS_PROVIDER,
                    LocationRequestCompat.Builder(LocationRequestCompat.PASSIVE_INTERVAL).setMinUpdateIntervalMillis(GPS_PASSIVE_INTERVAL).build(),
                    gpsLocationListener,
                    handlerThread.looper
                )
            }
        } catch (e: SecurityException) {
            Log.d(TAG, "GPS location retriever not initialized", e)
        }
    }

    @SuppressLint("WrongConstant")
    private fun scan(lowPower: Boolean) {
        if (!lowPower) lastHighPowerScanRealtime = SystemClock.elapsedRealtime()
        lastLowPowerScanRealtime = SystemClock.elapsedRealtime()
        val workSource = synchronized(activeRequests) { activeRequests.minByOrNull { it.intervalMillis }?.workSource }
        wifiDetailsSource?.startScan(workSource)
        cellDetailsSource?.startScan(workSource)
        updateRequests()
    }

    private fun updateRequests(forceNow: Boolean = false, lowPower: Boolean = true) {
        synchronized(activeRequests) {
            lowPowerIntervalMillis = Long.MAX_VALUE
            highPowerIntervalMillis = Long.MAX_VALUE
            for (request in activeRequests) {
                if (request.lowPower) lowPowerIntervalMillis = min(lowPowerIntervalMillis, request.intervalMillis)
                else highPowerIntervalMillis = min(highPowerIntervalMillis, request.intervalMillis)
            }
        }

        // Low power must be strictly less than high power
        if (highPowerIntervalMillis <= lowPowerIntervalMillis) lowPowerIntervalMillis = Long.MAX_VALUE

        val nextHighPowerRequestIn =
            if (highPowerIntervalMillis == Long.MAX_VALUE) Long.MAX_VALUE else highPowerIntervalMillis - (SystemClock.elapsedRealtime() - lastHighPowerScanRealtime)
        val nextLowPowerRequestIn =
            if (lowPowerIntervalMillis == Long.MAX_VALUE) Long.MAX_VALUE else lowPowerIntervalMillis - (SystemClock.elapsedRealtime() - lastLowPowerScanRealtime)

        handler.removeCallbacks(highPowerScanRunnable)
        handler.removeCallbacks(lowPowerScanRunnable)
        if ((forceNow && !lowPower) || nextHighPowerRequestIn <= 0) {
            Log.d(TAG, "Schedule high-power scan now")
            handler.post(highPowerScanRunnable)
        } else if (forceNow || nextLowPowerRequestIn <= 0) {
            Log.d(TAG, "Schedule low-power scan now")
            handler.post(lowPowerScanRunnable)
        } else {
            // Reschedule next request
            if (nextLowPowerRequestIn < nextHighPowerRequestIn) {
                Log.d(TAG, "Schedule low-power scan in ${nextLowPowerRequestIn}ms")
                handler.postDelayed(lowPowerScanRunnable, nextLowPowerRequestIn)
            } else if (nextHighPowerRequestIn != Long.MAX_VALUE) {
                Log.d(TAG, "Schedule high-power scan in ${nextHighPowerRequestIn}ms")
                handler.postDelayed(highPowerScanRunnable, nextHighPowerRequestIn)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.post {
            val pendingIntent = intent?.getParcelableExtra<PendingIntent>(EXTRA_PENDING_INTENT) ?: return@post
            val enable = intent.getBooleanExtra(EXTRA_ENABLE, false)
            if (enable) {
                val intervalMillis = intent.getLongExtra(EXTRA_INTERVAL_MILLIS, -1L)
                if (intervalMillis < 0) return@post
                var forceNow = intent.getBooleanExtra(EXTRA_FORCE_NOW, false)
                val lowPower = intent.getBooleanExtra(EXTRA_LOW_POWER, true)
                val bypass = intent.getBooleanExtra(EXTRA_BYPASS, false)
                val workSource = intent.getParcelableExtra(EXTRA_WORK_SOURCE) ?: WorkSource()
                synchronized(activeRequests) {
                    if (activeRequests.any { it.pendingIntent == pendingIntent }) {
                        forceNow = false
                        activeRequests.removeAll { it.pendingIntent == pendingIntent }
                    }
                    activeRequests.add(NetworkLocationRequest(pendingIntent, intervalMillis, lowPower, bypass, workSource))
                }
                handler.post { updateRequests(forceNow, lowPower) }
            } else {
                synchronized(activeRequests) {
                    activeRequests.removeAll { it.pendingIntent == pendingIntent }
                }
                handler.post { updateRequests() }
            }
        }
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        handlerThread.stop()
        wifiDetailsSource?.disable()
        wifiDetailsSource = null
        cellDetailsSource?.disable()
        cellDetailsSource = null
        super.onDestroy()
    }

    suspend fun requestWifiLocation(requestableWifis: List<WifiDetails>, currentLocalMovingWifi: WifiDetails?): Location? {
        var candidate: Location? = null
        if (currentLocalMovingWifi != null && settings.wifiMoving) {
            try {
                withTimeout(5000L) {
                    candidate = movingWifiHelper.retrieveMovingLocation(currentLocalMovingWifi)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed retrieving location for current moving wifi ${currentLocalMovingWifi.ssid}", e)
            }
        }
        if ((candidate?.accuracy ?: Float.MAX_VALUE) <= 50f) return candidate
        if (requestableWifis.size >= 3) {
            try {
                candidate = when (val cacheLocation = requestableWifis.hash()?.let { wifiScanCache[it.toHexString()] }
                    ?.takeIf { it.time > System.currentTimeMillis() - MAX_WIFI_SCAN_CACHE_AGE }) {
                    NEGATIVE_CACHE_ENTRY -> null
                    null -> {
                        if (settings.wifiMls) {
                            val location = mozilla.retrieveMultiWifiLocation(requestableWifis)
                            location.time = System.currentTimeMillis()
                            requestableWifis.hash()?.let { wifiScanCache[it.toHexString()] = location }
                            location
                        } else {
                            null
                        }
                    }

                    else -> cacheLocation
                }?.takeIf { candidate == null || it.accuracy < candidate?.accuracy!! } ?: candidate
            } catch (e: Exception) {
                Log.w(TAG, "Failed retrieving location for ${requestableWifis.size} wifi networks", e)
                if (e is ServiceException && e.error.code == 404 || e is VolleyError && e.networkResponse?.statusCode == 404) {
                    requestableWifis.hash()?.let { wifiScanCache[it.toHexString()] = NEGATIVE_CACHE_ENTRY }
                }
            }
        }
        if ((candidate?.accuracy ?: Float.MAX_VALUE) <= 50f) return candidate
        if (requestableWifis.isNotEmpty() && settings.wifiLearning) {
            val wifiLocations = requestableWifis.mapNotNull { wifi -> cache.getWifiLocation(wifi)?.let { wifi to it } }
            if (wifiLocations.size == 1 && (candidate == null || wifiLocations.single().second.accuracy < candidate!!.accuracy)) {
                return wifiLocations.single().second
            } else if (wifiLocations.isNotEmpty()) {
                val location = Location(LocationCacheDatabase.PROVIDER_CACHE).apply {
                    latitude = wifiLocations.weightedAverage { it.second.latitude to pow(10.0, it.first.signalStrength?.toDouble() ?: 1.0) }
                    longitude = wifiLocations.weightedAverage { it.second.longitude to pow(10.0, it.first.signalStrength?.toDouble() ?: 1.0) }
                    precision = wifiLocations.size.toDouble() / 4.0
                }
                location.accuracy = wifiLocations.maxOf { it.second.accuracy } - wifiLocations.minOf { it.second.distanceTo(location) } / 2
                return location
            }

        }
        return candidate
    }

    fun <T> List<T>.weightedAverage(f: (T) -> Pair<Double, Double>): Double {
        val valuesAndWeights = map { f(it) }
        return valuesAndWeights.sumOf { it.first * it.second } / valuesAndWeights.sumOf { it.second }
    }

    override fun onWifiDetailsAvailable(wifis: List<WifiDetails>) {
        if (wifis.isEmpty()) return
        val scanResultTimestamp = min(wifis.maxOf { it.timestamp ?: Long.MAX_VALUE }, System.currentTimeMillis())
        val scanResultRealtimeMillis =
            if (SDK_INT >= 17) SystemClock.elapsedRealtime() - (System.currentTimeMillis() - scanResultTimestamp) else scanResultTimestamp
        if (scanResultRealtimeMillis < lastWifiDetailsRealtimeMillis + interval / 2 && lastWifiDetailsRealtimeMillis != 0L) {
            Log.d(TAG, "Ignoring wifi details, similar age as last ($scanResultRealtimeMillis < $lastWifiDetailsRealtimeMillis + $interval / 2)")
            return
        }
        @Suppress("DEPRECATION")
        val currentLocalMovingWifi = getSystemService<WifiManager>()?.connectionInfo
            ?.let { wifiInfo -> wifis.filter { it.macAddress == wifiInfo.bssid && it.isMoving } }
            ?.filter { movingWifiHelper.isLocallyRetrievable(it) }
            ?.singleOrNull()
        val requestableWifis = wifis.filter(WifiDetails::isRequestable)
        if (SDK_INT >= 17 && settings.wifiLearning) {
            for (wifi in requestableWifis.filter { it.timestamp != null }) {
                val wifiElapsedMillis = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - wifi.timestamp!!)
                getGpsLocation(wifiElapsedMillis)?.let {
                    cache.learnWifiLocation(wifi, it)
                }
            }
        }
        if (requestableWifis.isEmpty() && currentLocalMovingWifi == null) return
        val previousLastRealtimeMillis = lastWifiDetailsRealtimeMillis
        lastWifiDetailsRealtimeMillis = scanResultRealtimeMillis
        lifecycleScope.launch {
            val location = requestWifiLocation(requestableWifis, currentLocalMovingWifi)
            if (location == null) {
                lastWifiDetailsRealtimeMillis = previousLastRealtimeMillis
                return@launch
            }
            location.time = scanResultTimestamp
            if (SDK_INT >= 17) location.elapsedRealtimeNanos = scanResultRealtimeMillis * 1_000_000L
            synchronized(locationLock) {
                lastWifiLocation = location
            }
            sendLocationUpdate()
        }
    }

    override fun onWifiSourceFailed() {
        // Wifi source failed, create a new one
        wifiDetailsSource?.disable()
        wifiDetailsSource = WifiDetailsSource.create(this, this).apply { enable() }
    }

    override fun onCellDetailsAvailable(cells: List<CellDetails>) {
        val scanResultTimestamp = min(cells.maxOf { it.timestamp ?: Long.MAX_VALUE }, System.currentTimeMillis())
        val scanResultRealtimeMillis =
            if (SDK_INT >= 17) SystemClock.elapsedRealtime() - (System.currentTimeMillis() - scanResultTimestamp) else scanResultTimestamp
        if (scanResultRealtimeMillis < lastCellDetailsRealtimeMillis + interval / 2) {
            Log.d(TAG, "Ignoring cell details, similar age as last")
            return
        }
        if (SDK_INT >= 17 && settings.cellLearning) {
            for (cell in cells.filter { it.timestamp != null && it.location == null }) {
                val cellElapsedMillis = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - cell.timestamp!!)
                getGpsLocation(cellElapsedMillis)?.let {
                    cache.learnCellLocation(cell, it)
                }
            }
        }
        lastCellDetailsRealtimeMillis = scanResultRealtimeMillis
        lifecycleScope.launch {
            val singleCell =
                cells.filter { it.location != NEGATIVE_CACHE_ENTRY }.maxByOrNull { it.timestamp ?: it.signalStrength?.toLong() ?: 0L } ?: return@launch
            val location = singleCell.location ?: try {
                when (val cacheLocation = cache.getCellLocation(singleCell, allowLearned = settings.cellLearning)) {
                    NEGATIVE_CACHE_ENTRY -> null

                    null -> if (settings.cellMls) {
                        mozilla.retrieveSingleCellLocation(singleCell).also {
                            it.time = System.currentTimeMillis()
                            cache.putCellLocation(singleCell, it)
                        }
                    } else {
                        null
                    }

                    else -> cacheLocation
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed retrieving location for $singleCell", e)
                if (e is ServiceException && e.error.code == 404 || e is VolleyError && e.networkResponse?.statusCode == 404) {
                    cache.putCellLocation(singleCell, NEGATIVE_CACHE_ENTRY)
                }
                null
            } ?: return@launch
            location.time = singleCell.timestamp ?: scanResultTimestamp
            if (SDK_INT >= 17) location.elapsedRealtimeNanos =
                singleCell.timestamp?.let { SystemClock.elapsedRealtimeNanos() - (System.currentTimeMillis() - it) * 1_000_000L }
                    ?: (scanResultRealtimeMillis * 1_000_000L)
            synchronized(locationLock) {
                lastCellLocation = location
            }
            sendLocationUpdate()
        }
    }

    private fun sendLocationUpdate(now: Boolean = false) {
        val location = synchronized(locationLock) {
            if (lastCellLocation == null && lastWifiLocation == null) return
            when {
                // Only non-null
                lastCellLocation == null -> lastWifiLocation
                lastWifiLocation == null -> lastCellLocation
                // Consider cliff
                lastCellLocation!!.elapsedMillis > lastWifiLocation!!.elapsedMillis + LOCATION_TIME_CLIFF_MS -> lastCellLocation
                lastWifiLocation!!.elapsedMillis > lastCellLocation!!.elapsedMillis + LOCATION_TIME_CLIFF_MS -> lastWifiLocation
                // Wifi out of cell range with higher precision
                lastCellLocation!!.precision > lastWifiLocation!!.precision && lastWifiLocation!!.distanceTo(lastCellLocation!!) > 2 * lastCellLocation!!.accuracy -> lastCellLocation
                else -> lastWifiLocation
            }
        } ?: return
        if (location == lastLocation) return
        if (lastLocation == lastWifiLocation && location == lastCellLocation && !now) {
            handler.postDelayed({
                sendLocationUpdate(true)
            }, DEBOUNCE_DELAY_MS)
            return
        }
        lastLocation = location
        synchronized(activeRequests) {
            for (request in activeRequests.toList()) {
                try {
                    request.send(this@NetworkLocationService, location)
                } catch (e: Exception) {
                    Log.w(TAG, "Pending intent error $request")
                    activeRequests.remove(request)
                }
            }
        }
    }

    private fun onNewGpsLocation(location: Location) {
        if (location.accuracy > GPS_PASSIVE_MIN_ACCURACY) return
        synchronized(gpsLocationBuffer) {
            if (gpsLocationBuffer.isNotEmpty() && gpsLocationBuffer.last.elapsedMillis < SystemClock.elapsedRealtime() - GPS_BUFFER_SIZE * GPS_PASSIVE_INTERVAL) {
                gpsLocationBuffer.clear()
            } else  if (gpsLocationBuffer.size >= GPS_BUFFER_SIZE) {
                gpsLocationBuffer.remove()
            }
            gpsLocationBuffer.offer(location)
        }
    }

    private fun getGpsLocation(elapsedMillis: Long): Location? {
        if (elapsedMillis + GPS_BUFFER_SIZE * GPS_PASSIVE_INTERVAL < SystemClock.elapsedRealtime()) return null
        synchronized(gpsLocationBuffer) {
            if (gpsLocationBuffer.isEmpty()) return null
            for (location in gpsLocationBuffer.descendingIterator()) {
                if (location.elapsedMillis in (elapsedMillis - GPS_PASSIVE_INTERVAL)..(elapsedMillis + GPS_PASSIVE_INTERVAL)) return location
                if (location.elapsedMillis < elapsedMillis) return null
            }
        }
        return null
    }

    override fun dump(fd: FileDescriptor?, writer: PrintWriter, args: Array<out String>?) {
        writer.println("Last scan elapsed realtime: high-power: ${lastHighPowerScanRealtime.formatRealtime()}, low-power: ${lastLowPowerScanRealtime.formatRealtime()}")
        writer.println("Last scan result time: wifi: ${lastWifiDetailsRealtimeMillis.formatRealtime()}, cells: ${lastCellDetailsRealtimeMillis.formatRealtime()}")
        writer.println("Interval: high-power: ${highPowerIntervalMillis.formatDuration()}, low-power: ${lowPowerIntervalMillis.formatDuration()}")
        writer.println("Last wifi location: $lastWifiLocation${if (lastWifiLocation == lastLocation) " (active)" else ""}")
        writer.println("Last cell location: $lastCellLocation${if (lastCellLocation == lastLocation) " (active)" else ""}")
        writer.println("Settings: Wi-Fi MLS=${settings.wifiMls} moving=${settings.wifiMoving} learn=${settings.wifiLearning} Cell MLS=${settings.cellMls} learn=${settings.cellLearning}")
        writer.println("Wifi scan cache size=${wifiScanCache.size()} hits=${wifiScanCache.hitCount()} miss=${wifiScanCache.missCount()} puts=${wifiScanCache.putCount()} evicts=${wifiScanCache.evictionCount()}")
        writer.println("GPS location buffer size=${gpsLocationBuffer.size} first=${gpsLocationBuffer.firstOrNull()?.elapsedMillis?.formatRealtime()} last=${gpsLocationBuffer.lastOrNull()?.elapsedMillis?.formatRealtime()}")
        cache.dump(writer)
        synchronized(activeRequests) {
            if (activeRequests.isNotEmpty()) {
                writer.println("Active requests:")
                for (request in activeRequests) {
                    writer.println("- ${request.workSource} ${request.intervalMillis.formatDuration()} (low power: ${request.lowPower}, bypass: ${request.bypass})")
                }
            }
        }
    }

    companion object {
        const val GPS_BUFFER_SIZE = 60
        const val GPS_PASSIVE_INTERVAL = 1000L
        const val GPS_PASSIVE_MIN_ACCURACY = 25f
        const val LOCATION_TIME_CLIFF_MS = 30000L
        const val DEBOUNCE_DELAY_MS = 5000L
        const val MAX_WIFI_SCAN_CACHE_AGE = 1000L * 60 * 60 * 24 // 1 day
    }
}

private operator fun <K, V> LruCache<K, V>.set(key: K, value: V) {
    put(key, value)
}

fun List<WifiDetails>.hash(): ByteArray? {
    val filtered = sortedBy { it.macClean }
        .filter { it.timestamp == null || it.timestamp!! > System.currentTimeMillis() - 60000 }
        .filter { it.signalStrength == null || it.signalStrength!! > -90 }
    if (filtered.size < 3) return null
    val maxTimestamp = maxOf { it.timestamp ?: 0L }
    fun WifiDetails.hashBytes(): ByteArray {
        return macBytes + byteArrayOf(
            ((maxTimestamp - (timestamp ?: 0L)) / (60 * 1000)).toByte(), // timestamp
            ((signalStrength ?: 0) / 10).toByte() // signal strength
        )
    }

    val buffer = ByteBuffer.allocate(filtered.size * 8)
    for (wifi in filtered) {
        buffer.put(wifi.hashBytes())
    }
    return buffer.array().digest("SHA-256")
}