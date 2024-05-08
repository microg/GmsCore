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
import org.microg.gms.location.network.ichnaea.IchnaeaServiceClient
import org.microg.gms.location.network.ichnaea.ServiceException
import org.microg.gms.location.network.wifi.*
import java.io.FileDescriptor
import java.io.PrintWriter
import java.lang.Math.pow
import java.nio.ByteBuffer
import java.util.LinkedList
import kotlin.math.max
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
    private val ichnaea by lazy { IchnaeaServiceClient(this) }
    private val cache by lazy { LocationCacheDatabase(this) }
    private val movingWifiHelper by lazy { MovingWifiHelper(this) }
    private val settings by lazy { LocationSettings(this) }
    private val wifiScanCache = LruCache<String, Location>(WIFI_SCAN_CACHE_SIZE)

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

    private var currentLocalMovingWifi: WifiDetails? = null
    private var lastLocalMovingWifiLocationCandidate: Location? = null

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
            Log.d(TAG, "GPS location retriever not initialized due to lack of permission")
        } catch (e: Exception) {
            Log.d(TAG, "GPS location retriever not initialized", e)
        }
    }

    @SuppressLint("WrongConstant")
    private fun scan(lowPower: Boolean) {
        if (!lowPower) lastHighPowerScanRealtime = SystemClock.elapsedRealtime()
        lastLowPowerScanRealtime = SystemClock.elapsedRealtime()
        val currentLocalMovingWifi = currentLocalMovingWifi
        if (SDK_INT >= 19 && currentLocalMovingWifi != null && (lastLocalMovingWifiLocationCandidate?.elapsedRealtimeNanos ?: 0L) > SystemClock.elapsedRealtimeNanos() - MAX_LOCAL_WIFI_AGE_NS && (lastWifiDetailsRealtimeMillis) > SystemClock.elapsedRealtimeNanos() - MAX_LOCAL_WIFI_SCAN_AGE_NS && getSystemService<WifiManager>()?.connectionInfo?.bssid == currentLocalMovingWifi.macAddress) {
            Log.d(TAG, "Skip network scan and use current local wifi instead.")
            updateWifiLocation(listOf(currentLocalMovingWifi))
        } else {
            val workSource = synchronized(activeRequests) { activeRequests.minByOrNull { it.intervalMillis }?.workSource }
            Log.d(TAG, "Start network scan for $workSource")
            getSystemService<WifiManager>()?.connectionInfo
            wifiDetailsSource?.startScan(workSource)
            cellDetailsSource?.startScan(workSource)
        }
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

    suspend fun requestWifiLocation(requestableWifis: List<WifiDetails>): Location? {
        var candidate: Location? = null
        val currentLocalMovingWifi = currentLocalMovingWifi
        if (currentLocalMovingWifi != null && settings.wifiMoving) {
            try {
                withTimeout(5000L) {
                    lastLocalMovingWifiLocationCandidate = movingWifiHelper.retrieveMovingLocation(currentLocalMovingWifi)
                }
                candidate = lastLocalMovingWifiLocationCandidate
            } catch (e: Exception) {
                lastLocalMovingWifiLocationCandidate = null
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
                        if (settings.wifiIchnaea) {
                            val location = ichnaea.retrieveMultiWifiLocation(requestableWifis)
                            location.time = System.currentTimeMillis()
                            requestableWifis.hash()?.let { wifiScanCache[it.toHexString()] = location }
                            location
                        } else {
                            null
                        }
                    }

                    else -> Location(cacheLocation)
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
        @Suppress("DEPRECATION")
        currentLocalMovingWifi = getSystemService<WifiManager>()?.connectionInfo
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
        updateWifiLocation(requestableWifis, scanResultRealtimeMillis, scanResultTimestamp)
    }

    private fun updateWifiLocation(requestableWifis: List<WifiDetails>, scanResultRealtimeMillis: Long = 0, scanResultTimestamp: Long = 0) {
        if (scanResultRealtimeMillis < lastWifiDetailsRealtimeMillis + interval / 2 && lastWifiDetailsRealtimeMillis != 0L) {
            Log.d(TAG, "Ignoring wifi details, similar age as last ($scanResultRealtimeMillis < $lastWifiDetailsRealtimeMillis + $interval / 2)")
            return
        }
        val previousLastRealtimeMillis = lastWifiDetailsRealtimeMillis
        if (scanResultRealtimeMillis != 0L) lastWifiDetailsRealtimeMillis = scanResultRealtimeMillis
        lifecycleScope.launch {
            val location = requestWifiLocation(requestableWifis)
            if (location == null) {
                lastWifiDetailsRealtimeMillis = previousLastRealtimeMillis
                return@launch
            }
            if (scanResultTimestamp != 0L) location.time = max(scanResultTimestamp, location.time)
            if (SDK_INT >= 17 && scanResultRealtimeMillis != 0L) location.elapsedRealtimeNanos =
                max(location.elapsedRealtimeNanos, scanResultRealtimeMillis * 1_000_000L)
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

                    null -> if (settings.cellIchnaea) {
                        ichnaea.retrieveSingleCellLocation(singleCell).also {
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
        fun cliffLocations(old: Location?, new: Location?): Location? {
            // We move from wifi towards cell with accuracy
            if (old == null) return new
            if (new == null) return old
            val diff = new.elapsedMillis - old.elapsedMillis
            if (diff < LOCATION_TIME_CLIFF_START_MS) return old
            if (diff > LOCATION_TIME_CLIFF_END_MS) return new
            val pct = (diff - LOCATION_TIME_CLIFF_START_MS).toDouble() / (LOCATION_TIME_CLIFF_END_MS - LOCATION_TIME_CLIFF_START_MS).toDouble()
            return Location(old).apply {
                provider = "cliff"
                latitude = old.latitude * (1.0-pct) + new.latitude * pct
                longitude = old.longitude * (1.0-pct) + new.longitude * pct
                accuracy = (old.accuracy * (1.0-pct) + new.accuracy * pct).toFloat()
                altitude = old.altitude * (1.0-pct) + new.altitude * pct
                time = (old.time.toDouble() * (1.0-pct) + new.time.toDouble() * pct).toLong()
                elapsedRealtimeNanos = (old.elapsedRealtimeNanos.toDouble() * (1.0-pct) + new.elapsedRealtimeNanos.toDouble() * pct).toLong()
            }
        }
        val location = synchronized(locationLock) {
            if (lastCellLocation == null && lastWifiLocation == null) return
            when {
                // Only non-null
                lastCellLocation == null -> lastWifiLocation
                lastWifiLocation == null -> lastCellLocation
                // Consider cliff end
                lastCellLocation!!.elapsedMillis > lastWifiLocation!!.elapsedMillis + LOCATION_TIME_CLIFF_END_MS -> lastCellLocation
                lastWifiLocation!!.elapsedMillis > lastCellLocation!!.elapsedMillis + LOCATION_TIME_CLIFF_START_MS -> lastWifiLocation
                // Wifi out of cell range with higher precision
                lastCellLocation!!.precision > lastWifiLocation!!.precision && lastWifiLocation!!.distanceTo(lastCellLocation!!) > 2 * lastCellLocation!!.accuracy -> lastCellLocation
                // Consider cliff start
                lastCellLocation!!.elapsedMillis > lastWifiLocation!!.elapsedMillis + LOCATION_TIME_CLIFF_START_MS -> cliffLocations(lastWifiLocation, lastCellLocation)
                else -> lastWifiLocation
            }
        } ?: return
        if (location == lastLocation) return
        if (lastLocation == lastWifiLocation && lastLocation.let { it != null && location.accuracy > it.accuracy } && !now) {
            Log.d(TAG, "Debounce inaccurate location update")
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
            } else if (gpsLocationBuffer.size >= GPS_BUFFER_SIZE) {
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
        writer.println("Wifi settings: ichnaea=${settings.wifiIchnaea} moving=${settings.wifiMoving} learn=${settings.wifiLearning}")
        writer.println("Cell settings: ichnaea=${settings.cellIchnaea} learn=${settings.cellLearning}")
        writer.println("Ichnaea settings: endpoint=${settings.ichneaeEndpoint} contribute=${settings.ichnaeaContribute}")
        writer.println("Wifi scan cache size=${wifiScanCache.size()} hits=${wifiScanCache.hitCount()} miss=${wifiScanCache.missCount()} puts=${wifiScanCache.putCount()} evicts=${wifiScanCache.evictionCount()}")
        writer.println("GPS location buffer size=${gpsLocationBuffer.size} first=${gpsLocationBuffer.firstOrNull()?.elapsedMillis?.formatRealtime()} last=${gpsLocationBuffer.lastOrNull()?.elapsedMillis?.formatRealtime()}")
        cache.dump(writer)
        synchronized(activeRequests) {
            if (activeRequests.isNotEmpty()) {
                writer.println("Active requests:")
                for (request in activeRequests) {
                    writer.println("- ${request.workSource} ${request.intervalMillis.formatDuration()} (low power: ${request.lowPower}, bypass: ${request.bypass}) reported ${request.lastRealtime.formatRealtime()}")
                }
            }
        }
    }

    companion object {
        const val GPS_BUFFER_SIZE = 60
        const val GPS_PASSIVE_INTERVAL = 1000L
        const val GPS_PASSIVE_MIN_ACCURACY = 25f
        const val LOCATION_TIME_CLIFF_START_MS = 30000L
        const val LOCATION_TIME_CLIFF_END_MS = 60000L
        const val DEBOUNCE_DELAY_MS = 5000L
        const val MAX_WIFI_SCAN_CACHE_AGE = 1000L * 60 * 60 * 24 // 1 day
        const val MAX_LOCAL_WIFI_AGE_NS = 60_000_000_000L // 1 minute
        const val MAX_LOCAL_WIFI_SCAN_AGE_NS = 600_000_000_000L // 10 minutes
        const val WIFI_SCAN_CACHE_SIZE = 200
    }
}

private operator fun <K : Any, V : Any> LruCache<K, V>.set(key: K, value: V) {
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
            ((signalStrength ?: 0) / 20).toByte() // signal strength
        )
    }

    val buffer = ByteBuffer.allocate(filtered.size * 8)
    for (wifi in filtered) {
        buffer.put(wifi.hashBytes())
    }
    return buffer.array().digest("SHA-256")
}