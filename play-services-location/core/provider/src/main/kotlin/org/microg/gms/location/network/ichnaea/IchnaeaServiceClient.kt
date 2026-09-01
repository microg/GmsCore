/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.ichnaea

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.collection.LruCache
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import org.microg.gms.location.LocationSettings
import org.microg.gms.location.formatRealtime
import org.microg.gms.location.network.*
import org.microg.gms.location.network.cell.CellDetails
import org.microg.gms.location.network.precision
import org.microg.gms.location.network.verticalAccuracy
import org.microg.gms.location.network.wifi.*
import org.microg.gms.location.provider.BuildConfig
import org.microg.gms.utils.singleInstanceOf
import java.io.PrintWriter
import java.nio.ByteBuffer
import java.util.LinkedList
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.min

class IchnaeaServiceClient(private val context: Context) {
    private val queue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }
    private val settings = LocationSettings(context)
    private val omitables = LinkedList<String>()
    private val cache = LruCache<String, Location>(REQUEST_CACHE_SIZE)
    private val start = SystemClock.elapsedRealtime()

    private val hasTelephony by lazy {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    private fun GeolocateRequest.hash(): ByteArray? {
        if (cellTowers.isNullOrEmpty() && (wifiAccessPoints?.size ?: 0) < 3 || bluetoothBeacons?.isNotEmpty() == true) return null
        val minAge = min(
            cellTowers?.takeIf { it.isNotEmpty() }?.minOf { it.age?.takeIf { it > 0L } ?: 0L } ?: Long.MAX_VALUE,
            wifiAccessPoints?.takeIf { it.isNotEmpty() }?.minOf { it.age?.takeIf { it > 0L } ?: 0L } ?: Long.MAX_VALUE
        )
        val buffer = ByteBuffer.allocate(8 + (cellTowers?.size ?: 0) * 23 + (wifiAccessPoints?.size ?: 0) * 8)
        buffer.putInt(cellTowers?.size?: 0)
        buffer.putInt(wifiAccessPoints?.size?: 0)
        for (cell in cellTowers.orEmpty()) {
            buffer.put(cell.radioType?.ordinal?.toByte() ?: -1)
            buffer.putInt(cell.mobileCountryCode ?: -1)
            buffer.putInt(cell.mobileNetworkCode ?: -1)
            buffer.putInt(cell.locationAreaCode ?: -1)
            buffer.putInt(cell.cellId ?: -1)
            buffer.putInt(cell.psc ?: -1)
            buffer.put(((cell.age?.let { it - minAge }?: 0L) / (60 * 1000)).toByte())
            buffer.put(((cell.signalStrength ?: 0) / 20).toByte())
        }
        for (wifi in wifiAccessPoints.orEmpty()) {
            buffer.put(wifi.macBytes)
            buffer.put(((wifi.age?.let { it - minAge }?: 0L) / (60 * 1000)).toByte())
            buffer.put(((wifi.signalStrength ?: 0) / 20).toByte())
        }
        return buffer.array().digest("SHA-256")
    }

    fun isRequestable(wifi: WifiDetails): Boolean {
        return wifi.isRequestable && !omitables.contains(wifi.macClean)
    }

    suspend fun retrieveMultiWifiLocation(wifis: List<WifiDetails>, rawHandler: ((WifiDetails, Location) -> Unit)? = null): Location? = geoLocate(
        GeolocateRequest(
            considerIp = !hasTelephony,
            wifiAccessPoints = wifis.filter { isRequestable(it) }.map(WifiDetails::toWifiAccessPoint),
            fallbacks = Fallback(lacf = false, ipf = false)
        ),
        rawWifiHandler = rawHandler
    )?.apply {
        precision = wifis.size.toDouble() / WIFI_BASE_PRECISION_COUNT
    }

    suspend fun retrieveSingleCellLocation(cell: CellDetails, rawHandler: ((CellDetails, Location) -> Unit)? = null): Location? = geoLocate(
        GeolocateRequest(
            considerIp = false,
            radioType = cell.toCellTower().radioType,
            homeMobileCountryCode = cell.toCellTower().mobileCountryCode,
            homeMobileNetworkCode = cell.toCellTower().mobileNetworkCode,
            cellTowers = listOf(cell.toCellTower()),
            fallbacks = Fallback(
                lacf = true,
                ipf = false
            )
        ),
        rawCellHandler = rawHandler
    )?.apply {
        precision = if (extras?.getString(LOCATION_EXTRA_FALLBACK) != null) CELL_FALLBACK_PRECISION else CELL_DEFAULT_PRECISION
    }

    private suspend fun geoLocate(
        request: GeolocateRequest,
        rawWifiHandler: ((WifiDetails, Location) -> Unit)? = null,
        rawCellHandler: ((CellDetails, Location) -> Unit)? = null
    ): Location? {
        val requestHash = request.hash()
        if (requestHash != null) {
            val locationFromCache = cache[requestHash.toHexString()]
            if (locationFromCache == NEGATIVE_CACHE_ENTRY) return null
            if (locationFromCache != null) return Location(locationFromCache)
        }
        val response = rawGeoLocate(request)
        Log.d(TAG, "$request -> $response")
        for (entry in response.raw) {
            if (entry.omit && entry.wifiAccessPoint?.macAddress != null) {
                omitables.offer(entry.wifiAccessPoint.macAddress.lowercase().replace(":", ""))
                if (omitables.size > OMITABLES_LIMIT) omitables.remove()
                runCatching { rawWifiHandler?.invoke(entry.wifiAccessPoint.toWifiDetails(), NEGATIVE_CACHE_ENTRY) }
            }
            if (entry.omit && entry.cellTower?.radioType != null) {
                runCatching { rawCellHandler?.invoke(entry.cellTower.toCellDetails(), NEGATIVE_CACHE_ENTRY) }
            }
            if (!entry.omit && entry.wifiAccessPoint?.macAddress != null && entry.location != null) {
                val location = buildLocation(entry.location, entry.horizontalAccuracy, entry.verticalAccuracy).apply {
                    precision = 1.0
                }
                runCatching { rawWifiHandler?.invoke(entry.wifiAccessPoint.toWifiDetails(), location) }
            }
            if (!entry.omit && entry.cellTower?.radioType != null && entry.location != null) {
                val location = buildLocation(entry.location, entry.horizontalAccuracy, entry.verticalAccuracy).apply {
                    precision = 1.0
                }
                runCatching { rawCellHandler?.invoke(entry.cellTower.toCellDetails(), location) }
            }
        }
        val location = if (response.location != null) {
            buildLocation(response.location, response.horizontalAccuracy, response.verticalAccuracy).apply {
                if (response.fallback != null) extras = (extras ?: Bundle()).apply { putString(LOCATION_EXTRA_FALLBACK, response.fallback) }
            }
        } else if (response.error != null && response.error.code == 404) {
            NEGATIVE_CACHE_ENTRY
        } else if (response.error != null) {
            throw ServiceException(response.error)
        } else {
            throw RuntimeException("Invalid response JSON")
        }
        if (requestHash != null) {
            cache[requestHash.toHexString()] = if (location == NEGATIVE_CACHE_ENTRY) NEGATIVE_CACHE_ENTRY else Location(location)
        }
        if (location == NEGATIVE_CACHE_ENTRY) return null
        return location
    }

    private fun buildLocation(location: ResponseLocation, defaultHorizontalAccuracy: Double? = null, defaultVerticalAccuracy: Double? = null): Location {
        return Location(PROVIDER).apply {
            latitude = location.latitude
            longitude = location.longitude
            if (location.altitude != null) altitude = location.altitude
            if (defaultHorizontalAccuracy != null && defaultHorizontalAccuracy > 0.0) accuracy = defaultHorizontalAccuracy.toFloat()
            if (hasAltitude() && defaultVerticalAccuracy != null && defaultVerticalAccuracy > 0.0) verticalAccuracy = defaultVerticalAccuracy.toFloat()
            if (location.horizontalAccuracy != null && location.horizontalAccuracy > 0.0) accuracy = location.horizontalAccuracy.toFloat()
            if (hasAltitude() && location.verticalAccuracy != null && location.verticalAccuracy > 0.0) verticalAccuracy = location.verticalAccuracy.toFloat()
            time = System.currentTimeMillis()
        }
    }

    private fun getRequestHeaders(): Map<String, String> = buildMap {
        set("User-Agent", "${BuildConfig.ICHNAEA_USER_AGENT} (Linux; Android ${android.os.Build.VERSION.RELEASE}; ${context.packageName})")
        if (settings.ichnaeaContribute) {
            set("X-Ichnaea-Contribute-Opt-In", "1")
        }
    }

    private fun continueError(continuation: Continuation<GeolocateResponse>, error: VolleyError) {
        try {
            val response = JSONObject(error.networkResponse.data.decodeToString()).toGeolocateResponse()
            if (response.error != null) {
                continuation.resume(response)
                return
            } else if (response.location?.latitude != null){
                Log.w(TAG, "Received location in response with error code")
            } else {
                Log.w(TAG, "Received valid json without error in response with error code")
            }
        } catch (_: Exception) {
        }
        if (error.networkResponse != null) {
            continuation.resume(GeolocateResponse(error = ResponseError(error.networkResponse.statusCode, error.message)))
            return
        }
        continuation.resumeWithException(error)
    }

    private suspend fun rawGeoLocate(request: GeolocateRequest): GeolocateResponse = suspendCoroutine { continuation ->
        val url = Uri.parse(settings.effectiveEndpoint).buildUpon().appendPath("v1").appendPath("geolocate").build().toString()
        queue.add(object : JsonObjectRequest(Method.POST, url, request.toJson(), {
            try {
                continuation.resume(it.toGeolocateResponse())
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }, {
            continueError(continuation, it)
        }) {
            override fun getHeaders(): Map<String, String> = getRequestHeaders()
        })
    }

    private suspend fun rawGeoSubmit(request: GeosubmitRequest): Unit = suspendCoroutine { continuation ->
        val url = Uri.parse(settings.effectiveEndpoint).buildUpon().appendPath("v2").appendPath("geosubmit").build().toString()
        queue.add(object : JsonObjectRequest(Method.POST, url, request.toJson(), {
            continuation.resume(Unit)
        }, {
            continuation.resumeWithException(it)
        }) {
            override fun getHeaders(): Map<String, String> = getRequestHeaders()
        })
    }

    fun dump(writer: PrintWriter) {
        writer.println("Ichnaea start=${start.formatRealtime()} omitables=${omitables.size}")
        writer.println("Ichnaea request cache size=${cache.size()} hits=${cache.hitCount()} miss=${cache.missCount()} puts=${cache.putCount()} evicts=${cache.evictionCount()}")
    }

    private operator fun <K : Any, V : Any> LruCache<K, V>.set(key: K, value: V) {
        put(key, value)
    }

    companion object {
        private const val TAG = "IchnaeaLocation"
        private const val PROVIDER = "ichnaea"
        const val WIFI_BASE_PRECISION_COUNT = 4.0
        const val CELL_DEFAULT_PRECISION = 1.0
        const val CELL_FALLBACK_PRECISION = 0.5
        private const val OMITABLES_LIMIT = 100
        private const val REQUEST_CACHE_SIZE = 200
        const val LOCATION_EXTRA_FALLBACK = "fallback"
    }
}