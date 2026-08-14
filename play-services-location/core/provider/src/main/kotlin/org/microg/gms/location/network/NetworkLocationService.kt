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
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.*
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.core.content.IntentCompat
import androidx.core.content.getSystemService
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.microg.gms.location.*
import org.microg.gms.location.network.cell.CellDetails
import org.microg.gms.location.network.cell.CellDetailsCallback
import org.microg.gms.location.network.cell.CellDetailsSource
import org.microg.gms.location.network.ichnaea.IchnaeaServiceClient
import org.microg.gms.location.network.wifi.*
import java.io.FileDescriptor
import java.io.PrintWriter
import java.util.*
import kotlin.math.*

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
    private val database by lazy { LocationDatabase(this) }
    private val movingWifiHelper by lazy { MovingWifiHelper(this) }
    private val settings by lazy { LocationSettings(this) }

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

    private val passiveLocationListener by lazy { LocationListenerCompat { onNewPassiveLocation(it) } }

    @GuardedBy("gpsLocationBuffer")
    private val gpsLocationBuffer = LinkedList<Location>()
    private var passiveListenerActive = false

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
        if (settings.effectiveEndpoint == null && (settings.wifiIchnaea || settings.cellIchnaea)) {
            sendBroadcast(Intent(ACTION_CONFIGURATION_REQUIRED).apply {
                `package` = packageName
                putExtra(EXTRA_CONFIGURATION, CONFIGURATION_FIELD_ONLINE_SOURCE)
            })
        }
    }

    private fun updatePassiveGpsListenerRegistration() {
        try {
            getSystemService<LocationManager>()?.let { locationManager ->
                if ((settings.cellLearning || settings.wifiLearning) && (highPowerIntervalMillis != Long.MAX_VALUE)) {
                    if (!passiveListenerActive) {
                        LocationManagerCompat.requestLocationUpdates(
                            locationManager,
                            LocationManager.PASSIVE_PROVIDER,
                            LocationRequestCompat.Builder(LocationRequestCompat.PASSIVE_INTERVAL)
                                .setQuality(LocationRequestCompat.QUALITY_LOW_POWER)
                                .setMinUpdateIntervalMillis(GPS_PASSIVE_INTERVAL)
                                .build(),
                            passiveLocationListener,
                            handlerThread.looper
                        )
                        passiveListenerActive = true
                    }
                } else {
                    if (passiveListenerActive) {
                        LocationManagerCompat.removeUpdates(locationManager, passiveLocationListener)
                        passiveListenerActive = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.d(TAG, "GPS location retriever not initialized due to lack of permission")
        } catch (e: Exception) {
            Log.d(TAG, "GPS location retriever not initialized", e)
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("WrongConstant")
    private fun scan(lowPower: Boolean) {
        if (!lowPower) lastHighPowerScanRealtime = SystemClock.elapsedRealtime()
        lastLowPowerScanRealtime = SystemClock.elapsedRealtime()
        val currentLocalMovingWifi = currentLocalMovingWifi
        val lastWifiScanIsSufficientlyNewForMoving = lastWifiDetailsRealtimeMillis > SystemClock.elapsedRealtime() - MAX_LOCAL_WIFI_SCAN_AGE_MS
        val movingWifiWasProducingRecentResults = (lastLocalMovingWifiLocationCandidate?.elapsedMillis ?: 0L) > SystemClock.elapsedRealtime() - max(MAX_LOCAL_WIFI_AGE_MS, interval * 2)
        val movingWifiLocationWasAccurate = (lastLocalMovingWifiLocationCandidate?.accuracy ?: Float.MAX_VALUE) <= MOVING_WIFI_HIGH_POWER_ACCURACY
        if (currentLocalMovingWifi != null &&
            movingWifiWasProducingRecentResults &&
            lastWifiScanIsSufficientlyNewForMoving &&
            (movingWifiLocationWasAccurate || lowPower) &&
            getSystemService<WifiManager>()?.connectionInfo?.bssid == currentLocalMovingWifi.macAddress
        ) {
            Log.d(TAG, "Skip network scan and use current local wifi instead. low=$lowPower accurate=$movingWifiLocationWasAccurate")
            onWifiDetailsAvailable(listOf(currentLocalMovingWifi.copy(timestamp = System.currentTimeMillis())))
        } else {
            val workSource = synchronized(activeRequests) { activeRequests.minByOrNull { it.intervalMillis }?.workSource }
            Log.d(TAG, "Start network scan for $workSource")
            if (settings.wifiLearning || settings.wifiCaching || settings.wifiIchnaea) {
                wifiDetailsSource?.startScan(workSource)
            } else if (settings.wifiMoving) {
                // No need to scan if only moving wifi enabled, instead simulate scan based on current connection info
                val connectionInfo = getSystemService<WifiManager>()?.connectionInfo
                if (SDK_INT >= 31 && connectionInfo != null && connectionInfo.toWifiDetails() != null) {
                    onWifiDetailsAvailable(listOfNotNull(connectionInfo.toWifiDetails()))
                } else if (currentLocalMovingWifi != null && connectionInfo?.bssid == currentLocalMovingWifi.macAddress) {
                    onWifiDetailsAvailable(listOf(currentLocalMovingWifi.copy(timestamp = System.currentTimeMillis())))
                } else {
                    // Can't simulate scan, so just scan
                    wifiDetailsSource?.startScan(workSource)
                }
            }
            if (settings.cellLearning || settings.cellCaching || settings.cellIchnaea) {
                cellDetailsSource?.startScan(workSource)
            }
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

        updatePassiveGpsListenerRegistration()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_NETWORK_LOCATION_SERVICE) {
            handler.post {
                val pendingIntent = IntentCompat.getParcelableExtra(intent, EXTRA_PENDING_INTENT, PendingIntent::class.java) ?: return@post
                val enable = intent.getBooleanExtra(EXTRA_ENABLE, false)
                if (enable) {
                    val intervalMillis = intent.getLongExtra(EXTRA_INTERVAL_MILLIS, -1L)
                    if (intervalMillis < 0) return@post
                    var forceNow = intent.getBooleanExtra(EXTRA_FORCE_NOW, false)
                    val lowPower = intent.getBooleanExtra(EXTRA_LOW_POWER, true)
                    val bypass = intent.getBooleanExtra(EXTRA_BYPASS, false)
                    val workSource = IntentCompat.getParcelableExtra(intent, EXTRA_WORK_SOURCE, WorkSource::class.java) ?: WorkSource()
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
        } else if (intent?.action == ACTION_NETWORK_IMPORT_EXPORT) {
            handler.post {
                val callback = IntentCompat.getParcelableExtra(intent, EXTRA_MESSENGER, Messenger::class.java)
                val replyWhat = intent.getIntExtra(EXTRA_REPLY_WHAT, 0)
                when (intent.getStringExtra(EXTRA_DIRECTION)) {
                    DIRECTION_EXPORT -> {
                        val name = intent.getStringExtra(EXTRA_NAME)
                        val uri = name?.let { database.exportLearned(it) }
                        callback?.send(Message.obtain().apply {
                            what = replyWhat
                            data = bundleOf(
                                EXTRA_DIRECTION to DIRECTION_EXPORT,
                                EXTRA_NAME to name,
                                EXTRA_URI to uri,
                            )
                        })
                    }
                    DIRECTION_IMPORT -> {
                        val uri = IntentCompat.getParcelableExtra(intent, EXTRA_URI, Uri::class.java)
                        val counter = uri?.let { database.importLearned(it) } ?: 0
                        callback?.send(Message.obtain().apply {
                            what = replyWhat
                            arg1 = counter
                            data = bundleOf(
                                EXTRA_DIRECTION to DIRECTION_IMPORT,
                                EXTRA_URI to uri,
                            )
                        })
                    }
                }
            }
        }
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        handlerThread.quitSafely()
        wifiDetailsSource?.disable()
        wifiDetailsSource = null
        cellDetailsSource?.disable()
        cellDetailsSource = null
        super.onDestroy()
    }

    fun Location.mayTakeAltitude(location: Location?) {
        if (location != null && !hasAltitude() && location.hasAltitude()) {
            altitude = location.altitude
            verticalAccuracy = location.verticalAccuracy
        }
    }

    suspend fun queryWifiLocation(wifis: List<WifiDetails>): Location? {
        var candidate: Location? = queryCurrentLocalMovingWifiLocation()
        if ((candidate?.accuracy ?: Float.MAX_VALUE) <= 50f) return candidate
        val databaseCandidate = queryWifiLocationFromDatabase(wifis)
        if (databaseCandidate != null && (candidate == null || databaseCandidate.precision > candidate.precision)) {
            databaseCandidate.mayTakeAltitude(candidate)
            candidate = databaseCandidate
        }
        if ((candidate?.accuracy ?: Float.MAX_VALUE) <= 50f && (candidate?.precision ?: 0.0) > 1.0) return candidate
        val ichnaeaCandidate = queryIchnaeaWifiLocation(wifis, minimumPrecision = (candidate?.precision ?: 0.0))
        if (ichnaeaCandidate != null && ichnaeaCandidate.accuracy >= (candidate?.accuracy ?: 0f)) {
            ichnaeaCandidate.mayTakeAltitude(candidate)
            candidate = ichnaeaCandidate
        }
        return candidate
    }

    private suspend fun queryCurrentLocalMovingWifiLocation(): Location? {
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
        return candidate
    }

    private suspend fun queryWifiLocationFromDatabase(wifis: List<WifiDetails>): Location? =
        queryLocationFromRetriever(wifis, 1000.0) { database.getWifiLocation(it, settings.wifiLearning) }

    private suspend fun queryCellLocationFromDatabase(cells: List<CellDetails>): Location? =
        queryLocationFromRetriever(cells, 50000.0) { it.location ?: database.getCellLocation(it, settings.cellLearning) }

    private val NetworkDetails.signalStrengthBounded: Int
        get() = (signalStrength ?: -100).coerceIn(-100, -10)

    private val NetworkDetails.ageBounded: Long
        get() = (System.currentTimeMillis() - (timestamp ?: 0)).coerceIn(0, 60000)

    private val NetworkDetails.weight: Double
        get() = min(1.0, sqrt(2000.0 / ageBounded)) / signalStrengthBounded.toDouble().pow(2)

    private fun <T: NetworkDetails> queryLocationFromRetriever(data: List<T>, maxClusterDistance: Double = 0.0, retriever: (T) -> Location?): Location? {
        val locations = data.mapNotNull { detail -> retriever(detail)?.takeIf { it != NEGATIVE_CACHE_ENTRY }?.let { detail to it } }
        if (locations.isNotEmpty()) {
            val clusters = locations.map { mutableListOf(it) }
            for (cellLocation in locations) {
                for (cluster in clusters) {
                    if (cluster.first() == cellLocation) continue;
                    if (cluster.first().second.distanceTo(cellLocation.second) < max(cluster.first().second.accuracy * 2.0, maxClusterDistance)) {
                        cluster.add(cellLocation)
                    }
                }
            }
            val cluster = clusters.maxBy { it.sumOf { it.second.precision } }

            return Location(PROVIDER_CACHE).apply {
                latitude = cluster.weightedAverage { it.second.latitude to it.first.weight }
                longitude = cluster.weightedAverage { it.second.longitude to it.first.weight }
                accuracy = min(
                    cluster.map { it.second.distanceTo(this) + it.second.accuracy }.average().toFloat() / cluster.size,
                    cluster.minOf { it.second.accuracy }
                )
                val altitudeCluster = cluster.filter { it.second.hasAltitude() }.takeIf { it.isNotEmpty() }
                if (altitudeCluster != null) {
                    altitude = altitudeCluster.weightedAverage { it.second.altitude to it.first.weight }
                    verticalAccuracy = min(
                        altitudeCluster.map { abs(altitude - it.second.altitude) + (it.second.verticalAccuracy ?: it.second.accuracy) }.average().toFloat() / cluster.size,
                        altitudeCluster.minOf { it.second.verticalAccuracy ?: it.second.accuracy }
                    )
                }
                precision = cluster.sumOf { it.second.precision }
                time = System.currentTimeMillis()
            }
        }
        return null
    }

    private suspend fun queryIchnaeaWifiLocation(wifis: List<WifiDetails>, minimumPrecision: Double = 0.0): Location? {
        if (settings.wifiIchnaea && wifis.size >= 3 && wifis.size / IchnaeaServiceClient.WIFI_BASE_PRECISION_COUNT >= minimumPrecision) {
            try {
                val ichnaeaCandidate = ichnaea.retrieveMultiWifiLocation(wifis) { wifi, location ->
                    if (settings.wifiCaching) database.putWifiLocation(wifi, location)
                }!!
                ichnaeaCandidate.time = System.currentTimeMillis()
                return ichnaeaCandidate
            } catch (e: Exception) {
                Log.w(TAG, "Failed retrieving location for ${wifis.size} wifi networks", e)
            }
        }
        return null
    }

    private fun <T> List<T>.weightedAverage(f: (T) -> Pair<Double, Double>): Double {
        val valuesAndWeights = map { f(it) }
        return valuesAndWeights.sumOf { it.first * it.second } / valuesAndWeights.sumOf { it.second }
    }

    override fun onWifiDetailsAvailable(wifis: List<WifiDetails>) {
        if (wifis.isEmpty()) return
        val scanResultTimestamp = min(wifis.maxOf { it.timestamp ?: Long.MAX_VALUE }, System.currentTimeMillis())
        val scanResultRealtimeMillis = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - scanResultTimestamp)
        @Suppress("DEPRECATION")
        currentLocalMovingWifi = getSystemService<WifiManager>()?.connectionInfo
            ?.let { wifiInfo -> wifis.filter { it.macAddress == wifiInfo.bssid && it.isMoving } }
            ?.filter { movingWifiHelper.isLocallyRetrievable(it) }
            ?.singleOrNull()
        val requestableWifis = wifis.filter(WifiDetails::isRequestable)
        if (requestableWifis.isEmpty() && currentLocalMovingWifi == null) return
        updateWifiLocation(requestableWifis, scanResultRealtimeMillis, scanResultTimestamp)
    }

    private fun updateWifiLocation(requestableWifis: List<WifiDetails>, scanResultRealtimeMillis: Long = 0, scanResultTimestamp: Long = 0) {
        if (settings.wifiLearning) {
            for (wifi in requestableWifis.filter { it.timestamp != null }) {
                val wifiElapsedMillis = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - wifi.timestamp!!)
                getGpsLocation(wifiElapsedMillis)?.let {
                    database.learnWifiLocation(wifi, it)
                }
            }
        }
        if (scanResultRealtimeMillis < lastWifiDetailsRealtimeMillis + interval / 2 && lastWifiDetailsRealtimeMillis != 0L && scanResultRealtimeMillis != 0L) {
            Log.d(TAG, "Ignoring wifi details, similar age as last ($scanResultRealtimeMillis < $lastWifiDetailsRealtimeMillis + $interval / 2)")
            return
        }
        val previousLastRealtimeMillis = lastWifiDetailsRealtimeMillis
        if (scanResultRealtimeMillis != 0L) lastWifiDetailsRealtimeMillis = scanResultRealtimeMillis
        lifecycleScope.launch {
            val location = queryWifiLocation(requestableWifis)
            if (location == null) {
                lastWifiDetailsRealtimeMillis = previousLastRealtimeMillis
                return@launch
            }
            if (scanResultTimestamp != 0L) location.time = max(scanResultTimestamp, location.time)
            if (scanResultRealtimeMillis != 0L) location.elapsedRealtimeNanos = max(location.elapsedRealtimeNanos, scanResultRealtimeMillis * 1_000_000L)
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

    private suspend fun queryCellLocation(cells: List<CellDetails>): Location? {
        val candidate = queryCellLocationFromDatabase(cells)
        if ((candidate?.precision ?: 0.0) > 1.0) return candidate
        val cellsToUpdate = cells.filter { it.location == null && database.getCellLocation(it, settings.cellLearning) == null }
        for (cell in cellsToUpdate) {
            queryIchnaeaCellLocation(cell)
        }
        // Try again after fetching records from internet
        return queryCellLocationFromDatabase(cells)
    }

    private suspend fun queryIchnaeaCellLocation(cell: CellDetails): Location? {
        if (settings.cellIchnaea) {
            try {
                val ichnaeaCandidate = ichnaea.retrieveSingleCellLocation(cell) { cell, location ->
                    if (settings.cellCaching) database.putCellLocation(cell, location)
                } ?: NEGATIVE_CACHE_ENTRY
                if (settings.cellCaching) {
                    if (ichnaeaCandidate == NEGATIVE_CACHE_ENTRY) {
                        database.putCellLocation(cell, NEGATIVE_CACHE_ENTRY)
                        return null
                    } else {
                        ichnaeaCandidate.time = System.currentTimeMillis()
                        database.putCellLocation(cell, ichnaeaCandidate)
                        return ichnaeaCandidate
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed retrieving location for cell network", e)
            }
        }
        return null
    }

    override fun onCellDetailsAvailable(cells: List<CellDetails>) {
        if (settings.cellLearning) {
            for (cell in cells.filter { it.timestamp != null && it.location == null }) {
                val cellElapsedMillis = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - cell.timestamp!!)
                getGpsLocation(cellElapsedMillis)?.let {
                    database.learnCellLocation(cell, it)
                }
            }
        }
        val scanResultTimestamp = min(cells.maxOf { it.timestamp ?: Long.MAX_VALUE }, System.currentTimeMillis())
        val scanResultRealtimeMillis = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - scanResultTimestamp)
        if (scanResultRealtimeMillis < lastCellDetailsRealtimeMillis + interval / 2 && lastCellDetailsRealtimeMillis != 0L) {
            Log.d(TAG, "Ignoring cell details, similar age as last ($scanResultRealtimeMillis < $lastCellDetailsRealtimeMillis + $interval / 2)")
            return
        }
        val previousLastRealtimeMillis = lastWifiDetailsRealtimeMillis
        lastCellDetailsRealtimeMillis = scanResultRealtimeMillis
        lifecycleScope.launch {
            val location = queryCellLocation(cells)
            if (location == null) {
                lastCellDetailsRealtimeMillis = previousLastRealtimeMillis
                return@launch
            }
            if (scanResultTimestamp != 0L) location.time = max(scanResultTimestamp, location.time)
            if (scanResultRealtimeMillis != 0L) location.elapsedRealtimeNanos = max(location.elapsedRealtimeNanos, scanResultRealtimeMillis * 1_000_000L)
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

    private fun onNewPassiveLocation(location: Location) {
        if (location.provider != LocationManager.GPS_PROVIDER || location.accuracy > GPS_PASSIVE_MIN_ACCURACY) return
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
        writer.println("Ichnaea settings: source=${settings.onlineSource?.id} endpoint=${settings.effectiveEndpoint} contribute=${settings.ichnaeaContribute}")
        ichnaea.dump(writer)
        writer.println("GPS location buffer size=${gpsLocationBuffer.size} first=${gpsLocationBuffer.firstOrNull()?.elapsedMillis?.formatRealtime()} last=${gpsLocationBuffer.lastOrNull()?.elapsedMillis?.formatRealtime()}")
        database.dump(writer)
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
        const val MAX_LOCAL_WIFI_AGE_MS = 60_000_000L // 1 minute
        const val MAX_LOCAL_WIFI_SCAN_AGE_MS = 600_000_000L // 10 minutes
        const val MOVING_WIFI_HIGH_POWER_ACCURACY = 100f
    }
}