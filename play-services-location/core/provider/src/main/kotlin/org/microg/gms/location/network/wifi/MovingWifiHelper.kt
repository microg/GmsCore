/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.wifi

import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.ConnectivityManager.TYPE_WIFI
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.core.content.getSystemService
import androidx.core.location.LocationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

private val MOVING_WIFI_HOTSPOTS = setOf(
    // Austria
    "OEBB",
    "Austrian Flynet",
    // Belgium
    "THALYSNET",
    // Canada
    "Air Canada",
    // Czech Republic
    "CDWiFi",
    // France
    "_SNCF_WIFI_INOUI",
    "_SNCF_WIFI_INTERCITES",
    "_WIFI_LYRIA",
    "OUIFI",
    "NormandieTrainConnecte",
    // Germany
    "WIFIonICE",
    "WIFI@DB",
    "WiFi@DB",
    "RRX Hotspot",
    "FlixBux",
    "FlixBus Wi-Fi",
    "FlixTrain Wi-Fi",
    "FlyNet",
    "Telekom_FlyNet",
    "Vestische WLAN",
    // Greece
    "AegeanWiFi",
    // Hong Kong
    "Cathay Pacific",
    // Hungary
    "MAVSTART-WIFI",
    // Netherlands
    "KEOLIS Nederland",
    // Sweden
    "SJ",
    // Switzerland
    "SBB-Free",
    // United Kingdom
    "CrossCountryWiFi",
    "GWR WiFi",
    // United States
    "Amtrak_WiFi",
)

private val PHONE_HOTSPOT_KEYWORDS = setOf(
    "iPhone",
    "Galaxy",
    "AndroidAP"
)

/**
 * A Wi-Fi hotspot that changes its location dynamically and thus is unsuitable for use with location services that assume stable locations.
 *
 * Some moving Wi-Fi hotspots allow to determine their location when connected or through a public network API.
 */
val WifiDetails.isMoving: Boolean
    get() {
        if (open && MOVING_WIFI_HOTSPOTS.contains(ssid)) {
            return true
        }
        if (PHONE_HOTSPOT_KEYWORDS.any { ssid?.contains(it) == true }) {
            return true
        }
        return false
    }

class MovingWifiHelper(private val context: Context) {
    suspend fun retrieveMovingLocation(current: WifiDetails): Location {
        if (!isLocallyRetrievable(current)) throw IllegalArgumentException()
        val connectivityManager = context.getSystemService<ConnectivityManager>() ?: throw IllegalStateException()
        val url = URL(MOVING_WIFI_HOTSPOTS_LOCALLY_RETRIEVABLE[current.ssid])
        return withContext(Dispatchers.IO) {
            val network = if (isLocallyRetrievable(current) && SDK_INT >= 23) {
                @Suppress("DEPRECATION")
                (connectivityManager.allNetworks.singleOrNull {
                    val networkInfo = connectivityManager.getNetworkInfo(it)
                    Log.d(org.microg.gms.location.network.TAG, "Network info: $networkInfo")
                    networkInfo?.type == TYPE_WIFI && networkInfo.isConnected
                })
            } else {
                null
            }
            val connection = (if (SDK_INT >= 21) {
                network?.openConnection(url)
            } else {
                null
            } ?: url.openConnection()) as HttpURLConnection
            try {
                connection.doInput = true
                if (connection.responseCode != 200) throw RuntimeException("Got error")
                parseInput(current.ssid!!, connection.inputStream.readBytes())
            } finally {
                connection.inputStream.close()
                connection.disconnect()
            }
        }
    }

    private fun parseWifiOnIce(location: Location, data: ByteArray): Location {
        val json = JSONObject(data.decodeToString())
        if (json.getString("gpsStatus") != "VALID") throw RuntimeException("GPS not valid")
        location.accuracy = 100f
        location.time = json.getLong("serverTime") - 15000L
        location.latitude = json.getDouble("latitude")
        location.longitude = json.getDouble("longitude")
        json.optDouble("speed").takeIf { !it.isNaN() }?.let {
            location.speed = (it / 3.6).toFloat()
            LocationCompat.setSpeedAccuracyMetersPerSecond(location, location.speed * 0.1f)
        }
        return location
    }

    private fun parseFlixbus(location: Location, data: ByteArray): Location {
        val json = JSONObject(data.decodeToString())
        location.accuracy = 100f
        location.latitude = json.getDouble("latitude")
        location.longitude = json.getDouble("longitude")
        json.optDouble("speed").takeIf { !it.isNaN() }?.let {
            location.speed = it.toFloat()
            LocationCompat.setSpeedAccuracyMetersPerSecond(location, location.speed * 0.1f)
        }
        return location
    }

    private fun parsePassengera(location: Location, data: ByteArray): Location {
        val json = JSONObject(data.decodeToString())
        location.accuracy = 100f
        location.latitude = json.getDouble("gpsLat")
        location.longitude = json.getDouble("gpsLng")
        json.optDouble("speed").takeIf { !it.isNaN() }?.let {
            location.speed = (it / 3.6).toFloat()
            LocationCompat.setSpeedAccuracyMetersPerSecond(location, location.speed * 0.1f)
        }
        json.optDouble("altitude").takeIf { !it.isNaN() }?.let { location.altitude = it }
        return location
    }

    private fun parseDisplayUgo(location: Location, data: ByteArray): Location {
        val json = JSONArray(data.decodeToString()).getJSONObject(0)
        location.accuracy = 100f
        location.latitude = json.getDouble("latitude")
        location.longitude = json.getDouble("longitude")
        runCatching { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).parse(json.getString("created_at"))?.time }.getOrNull()?.let { location.time = it }
        json.optDouble("speed_kilometers_per_hour").takeIf { !it.isNaN() }?.let {
            location.speed = (it / 3.6).toFloat()
            LocationCompat.setSpeedAccuracyMetersPerSecond(location, location.speed * 0.1f)
        }
        json.optDouble("altitude_meters").takeIf { !it.isNaN() }?.let { location.altitude = it }
        json.optDouble("bearing_in_degree").takeIf { !it.isNaN() }?.let {
            location.bearing = it.toFloat()
            LocationCompat.setBearingAccuracyDegrees(location, 90f)
        }
        return location
    }

    private fun parsePanasonic(location: Location, data: ByteArray): Location {
        val json = JSONObject(data.decodeToString())
        location.accuracy = 100f
        location.latitude = json.getJSONObject("current_coordinates").getDouble("latitude")
        location.longitude = json.getJSONObject("current_coordinates").getDouble("longitude")
        json.optDouble("ground_speed_knots").takeIf { !it.isNaN() }?.let {
            location.speed = (it * 0.5144).toFloat()
            LocationCompat.setSpeedAccuracyMetersPerSecond(location, location.speed * 0.1f)
        }
        json.optDouble("altitude_feet").takeIf { !it.isNaN() }?.let { location.altitude = it * 0.3048 }
        json.optDouble("true_heading_degree").takeIf { !it.isNaN() }?.let {
            location.bearing = it.toFloat()
            LocationCompat.setBearingAccuracyDegrees(location, 90f)
        }
        return location
    }

    private fun parseBoardConnect(location: Location, data: ByteArray): Location {
        val json = JSONObject(data.decodeToString())
        location.accuracy = 100f
        location.latitude = json.getDouble("lat")
        location.longitude = json.getDouble("lon")
        runCatching { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).parse(json.getString("utc"))?.time }.getOrNull()?.let { location.time = it }
        json.optDouble("groundSpeed").takeIf { !it.isNaN() }?.let {
            location.speed = (it * 0.5144).toFloat()
            LocationCompat.setSpeedAccuracyMetersPerSecond(location, location.speed * 0.1f)
        }
        json.optDouble("altitude").takeIf { !it.isNaN() }?.let { location.altitude = it * 0.3048 }
        json.optDouble("heading").takeIf { !it.isNaN() }?.let {
            location.bearing = it.toFloat()
            LocationCompat.setBearingAccuracyDegrees(location, 90f)
        }
        return location
    }

    private fun parseInput(ssid: String, data: ByteArray): Location {
        val location = Location(ssid)
        return when (ssid) {
            "WIFIonICE" -> parseWifiOnIce(location, data)
            "FlixBus" -> parseFlixbus(location, data)
            "FlixBus Wi-Fi" -> parseFlixbus(location, data)
            "FlixTrain Wi-Fi" -> parseFlixbus(location, data)
            "MAVSTART-WIFI" -> parsePassengera(location, data)
            "AegeanWiFi" -> parseDisplayUgo(location, data)
            "Telekom_FlyNet" -> parsePanasonic(location, data)
            "FlyNet" -> parseBoardConnect(location, data)
            else -> throw UnsupportedOperationException()
        }
    }

    fun isLocallyRetrievable(wifi: WifiDetails): Boolean =
        MOVING_WIFI_HOTSPOTS_LOCALLY_RETRIEVABLE.containsKey(wifi.ssid)

    companion object {
        private val MOVING_WIFI_HOTSPOTS_LOCALLY_RETRIEVABLE = mapOf(
            "WIFIonICE" to "https://iceportal.de/api1/rs/status",
            "FlixBus" to "https://media.flixbus.com/services/pis/v1/position",
            "FlixBus Wi-Fi" to "https://media.flixbus.com/services/pis/v1/position",
            "FlixTrain Wi-Fi" to "https://media.flixbus.com/services/pis/v1/position",
            "MAVSTART-WIFI" to "http://portal.mav.hu/portal/api/vehicle/realtime",
            "AegeanWiFi" to "https://api.ife.ugo.aero/navigation/positions",
            "Telekom_FlyNet" to "https://services.inflightpanasonic.aero/inflight/services/flightdata/v2/flightdata",
            "Cathay Pacific" to "https://services.inflightpanasonic.aero/inflight/services/flightdata/v2/flightdata",
            "FlyNet" to "https://ww2.lufthansa-flynet.com/map/api/flightData",
        )
    }
}

