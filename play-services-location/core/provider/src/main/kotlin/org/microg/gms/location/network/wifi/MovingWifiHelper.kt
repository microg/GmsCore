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
import org.microg.gms.location.network.TAG
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.security.KeyStore
import java.security.cert.*
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.CertPathTrustManagerParameters
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


private val MOVING_WIFI_HOTSPOTS = setOf(
    // Austria
    "OEBB",
    "Austrian FlyNet",
    "svciob", // OEBB Service WIFI
    // Belgium
    "THALYSNET",
    // Canada
    "Air Canada",
    "ACWiFi",
    "ACWiFi.com",
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
    "agilis-Wifi",
    "freeWIFIahead!",
    // Greece
    "AegeanWiFi",
    // Hong Kong
    "Cathay Pacific",
    // Hungary
    "MAVSTART-WIFI",
    // Netherlands
    "KEOLIS Nederland",
    // New Zealand
    "AirNZ_InflightWiFi",
    "Bluebridge WiFi",
    // Singapore
    "KrisWorld",
    // Sweden
    "SJ",
    "saswifi",
    // Switzerland
    "SBB-Free",
    "SBB-FREE",
    "SWISS Connect",
    "Edelweiss Entertainment",
    // United Kingdom
    "Avanti_Free_WiFi",
    "CrossCountryWiFi",
    "GWR WiFi",
    "LNR On Board Wi-Fi",
    "LOOP on train WiFi",
    "WMR On Board Wi-Fi",
    "EurostarTrainsWiFi",
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
        if (MOVING_WIFI_HOTSPOTS.contains(ssid)) {
            return true
        }
        if (PHONE_HOTSPOT_KEYWORDS.any { ssid?.contains(it) == true }) {
            return true
        }
        return false
    }

const val FEET_TO_METERS = 0.3048
const val KNOTS_TO_METERS_PER_SECOND = 0.5144
const val MILES_PER_HOUR_TO_METERS_PER_SECOND = 0.447

class MovingWifiHelper(private val context: Context) {
    suspend fun retrieveMovingLocation(current: WifiDetails): Location {
        if (!isLocallyRetrievable(current)) throw IllegalArgumentException()
        val connectivityManager = context.getSystemService<ConnectivityManager>() ?: throw IllegalStateException()
        val sources = MOVING_WIFI_HOTSPOTS_LOCALLY_RETRIEVABLE[current.ssid]!!
        val exceptions = mutableListOf<Exception>()
        for (source in sources) {
            try {
                val url = URL(source.url)
                return withContext(Dispatchers.IO) {
                    val network = if (isLocallyRetrievable(current) && SDK_INT >= 23) {
                        @Suppress("DEPRECATION")
                        (connectivityManager.allNetworks.singleOrNull {
                            val networkInfo = connectivityManager.getNetworkInfo(it)
                            networkInfo?.type == TYPE_WIFI && networkInfo.isConnected
                        })
                    } else {
                        null
                    }
                    val connection = (if (SDK_INT >= 23) {
                        network?.openConnection(url, Proxy.NO_PROXY)
                    } else {
                        null
                    } ?: url.openConnection()) as HttpURLConnection
                    try {
                        connection.doInput = true
                        if (connection is HttpsURLConnection && SDK_INT >= 24) {
                            try {
                                val ctx = SSLContext.getInstance("TLS")
                                val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                                fun wrap(originalTrustManager: TrustManager): TrustManager {
                                    if (originalTrustManager is X509TrustManager) {
                                        return object : X509TrustManager {
                                            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                                                Log.d(TAG, "checkClientTrusted: $chain, $authType")
                                                originalTrustManager.checkClientTrusted(chain, authType)
                                            }

                                            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                                                Log.d(TAG, "checkServerTrusted: $chain, $authType")
                                                originalTrustManager.checkServerTrusted(chain, authType)
                                            }

                                            override fun getAcceptedIssuers(): Array<X509Certificate> {
                                                return originalTrustManager.acceptedIssuers
                                            }
                                        }
                                    } else {
                                        return originalTrustManager
                                    }
                                }
                                val ks = KeyStore.getInstance("AndroidCAStore")
                                ks.load(null, null)
                                tmf.init(ks)
                                ctx.init(null, tmf.trustManagers.map(::wrap).toTypedArray(), null)
                                connection.sslSocketFactory = ctx.socketFactory
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to disable revocation", e)
                            }
                        }
                        if (connection.responseCode != 200) throw RuntimeException("Got error")
                        val location = Location(current.ssid ?: "wifi")
                        source.parse(location, connection.inputStream.readBytes())
                    } finally {
                        connection.inputStream.close()
                        connection.disconnect()
                    }
                }
            } catch (e: Exception) {
                exceptions.add(e)
            }
        }
        if (exceptions.size == 1) throw exceptions.single()
        throw RuntimeException(exceptions.joinToString("\n"))
    }

    fun isLocallyRetrievable(wifi: WifiDetails): Boolean =
        MOVING_WIFI_HOTSPOTS_LOCALLY_RETRIEVABLE.containsKey(wifi.ssid)

    companion object {
        abstract class MovingWifiLocationSource(val url: String) {
            abstract fun parse(location: Location, data: ByteArray): Location
        }

        private val SOURCE_WIFI_ON_ICE = object : MovingWifiLocationSource("https://iceportal.de/api1/rs/status") {
            override fun parse(location: Location, data: ByteArray): Location {
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
        }

        private val SOURCE_OEBB_1 = object : MovingWifiLocationSource("https://railnet.oebb.at/assets/modules/fis/combined.json") {
            override fun parse(location: Location, data: ByteArray): Location {
                val json = JSONObject(data.decodeToString()).getJSONObject("latestStatus")
                location.accuracy = 100f
                runCatching { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).parse(json.getString("dateTime"))?.time }.getOrNull()?.let { location.time = it }
                location.latitude = json.getJSONObject("gpsPosition").getDouble("latitude")
                location.longitude = json.getJSONObject("gpsPosition").getDouble("longitude")
                json.getJSONObject("gpsPosition").optDouble("orientation").takeIf { !it.isNaN() }?.let {
                    location.bearing = it.toFloat()
                    LocationCompat.setBearingAccuracyDegrees(location, 90f)
                }
                json.optDouble("speed").takeIf { !it.isNaN() }?.let {
                    location.speed = (it / 3.6).toFloat()
                    LocationCompat.setSpeedAccuracyMetersPerSecond(location, location.speed * 0.1f)
                }
                return location
            }
        }

        private val SOURCE_OEBB_2 = object : MovingWifiLocationSource("https://railnet.oebb.at/api/gps") {
            override fun parse(location: Location, data: ByteArray): Location {
                val root = JSONObject(data.decodeToString())
                if (root.has("JSON")) {
                    val json = root.getJSONObject("JSON")
                    if (!json.isNull("error")) throw RuntimeException("Error: ${json.get("error")}");
                    location.accuracy = 100f
                    location.latitude = json.getDouble("lat")
                    location.longitude = json.getDouble("lon")
                    json.optDouble("speed").takeIf { !it.isNaN() }?.let {
                        location.speed = (it / 3.6).toFloat()
                        LocationCompat.setSpeedAccuracyMetersPerSecond(location, location.speed * 0.1f)
                    }
                } else if (root.optDouble("Latitude").let { !it.isNaN() && it.isFinite() && it > 0.1 }) {
                    location.accuracy = 100f
                    location.latitude = root.getDouble("Latitude")
                    location.longitude = root.getDouble("Longitude")
                } else {
                    throw RuntimeException("Unsupported: $root")
                }
                return location
            }
        }

        private val SOURCE_FLIXBUS = object : MovingWifiLocationSource("https://media.flixbus.com/services/pis/v1/position") {
            override fun parse(location: Location, data: ByteArray): Location {
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
        }

        class PassengeraLocationSource(base: String) : MovingWifiLocationSource("$base/portal/api/vehicle/realtime") {
            override fun parse(location: Location, data: ByteArray): Location {
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
        }
        private val SOURCE_PASSENGERA_MAV = PassengeraLocationSource("http://portal.mav.hu")
        private val SOURCE_PASSENGERA_CD = PassengeraLocationSource("http://cdwifi.cz")

        private val SOURCE_DISPLAY_UGO = object : MovingWifiLocationSource("https://api.ife.ugo.aero/navigation/positions") {
            override fun parse(location: Location, data: ByteArray): Location {
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
        }

        private val SOURCE_INFLIGHT_PANASONIC = object : MovingWifiLocationSource("https://services.inflightpanasonic.aero/inflight/services/flightdata/v2/flightdata") {
            override fun parse(location: Location, data: ByteArray): Location {
                val json = JSONObject(data.decodeToString())
                location.accuracy = 100f
                location.latitude = json.getJSONObject("current_coordinates").getDouble("latitude")
                location.longitude = json.getJSONObject("current_coordinates").getDouble("longitude")
                json.optDouble("ground_speed_knots").takeIf { !it.isNaN() }?.let {
                    location.speed = (it * KNOTS_TO_METERS_PER_SECOND).toFloat()
                    LocationCompat.setSpeedAccuracyMetersPerSecond(location, location.speed * 0.1f)
                }
                json.optDouble("altitude_feet").takeIf { !it.isNaN() }?.let { location.altitude = it * FEET_TO_METERS }
                json.optDouble("true_heading_degree").takeIf { !it.isNaN() }?.let {
                    location.bearing = it.toFloat()
                    LocationCompat.setBearingAccuracyDegrees(location, 90f)
                }
                return location
            }
        }

        class BoardConnectLocationSource(base: String) : MovingWifiLocationSource("$base/map/api/flightData") {
            override fun parse(location: Location, data: ByteArray): Location {
                val json = JSONObject(data.decodeToString())
                location.accuracy = 100f
                location.latitude = json.getDouble("lat")
                location.longitude = json.getDouble("lon")
                runCatching { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).parse(json.getString("utc"))?.time }.getOrNull()?.let { location.time = it }
                json.optDouble("groundSpeed").takeIf { !it.isNaN() }?.let {
                    location.speed = (it * KNOTS_TO_METERS_PER_SECOND).toFloat()
                    LocationCompat.setSpeedAccuracyMetersPerSecond(location, location.speed * 0.1f)
                }
                json.optDouble("altitude").takeIf { !it.isNaN() }?.let { location.altitude = it * FEET_TO_METERS }
                json.optDouble("heading").takeIf { !it.isNaN() }?.let {
                    location.bearing = it.toFloat()
                    LocationCompat.setBearingAccuracyDegrees(location, 90f)
                }
                return location
            }
        }
        private val SOURCE_LUFTHANSA_FLYNET_EUROPE = BoardConnectLocationSource("https://www.lufthansa-flynet.com")
        private val SOURCE_LUFTHANSA_FLYNET_EUROPE_2 = BoardConnectLocationSource("https://ww2.lufthansa-flynet.com")
        private val SOURCE_AUSTRIAN_FLYNET_EUROPE = BoardConnectLocationSource("https://www.austrian-flynet.com")

        class SncfLocationSource(base: String) : MovingWifiLocationSource("$base/router/api/train/gps") {
            override fun parse(location: Location, data: ByteArray): Location {
                val json = JSONObject(data.decodeToString())
                if(json.has("fix") && json.getInt("fix") == -1) throw RuntimeException("GPS not valid")
                location.accuracy = 100f
                location.latitude = json.getDouble("latitude")
                location.longitude = json.getDouble("longitude")
                json.optDouble("speed").takeIf { !it.isNaN() }?.let {
                    location.speed = it.toFloat()
                    LocationCompat.setSpeedAccuracyMetersPerSecond(location, location.speed * 0.1f)
                }
                location.time = json.getLong("timestamp")
                json.optDouble("heading").takeIf { !it.isNaN() }?.let {
                    location.bearing = it.toFloat()
                    LocationCompat.setBearingAccuracyDegrees(location, 90f)
                }
                return location
            }
        }
        private val SOURCE_SNCF = SncfLocationSource("https://wifi.sncf")
        private val SOURCE_SNCF_INTERCITES = SncfLocationSource("https://wifi.intercites.sncf")
        private val SOURCE_LYRIA = SncfLocationSource("https://wifi.tgv-lyria.com")
        private val SOURCE_NORMANDIE = SncfLocationSource("https://wifi.normandie.fr")

        private val SOURCE_OUIFI = object : MovingWifiLocationSource("https://ouifi.ouigo.com:8084/api/gps") {
            override fun parse(location: Location, data: ByteArray): Location {
                val json = JSONObject(data.decodeToString())
                if(json.has("fix") && json.getInt("fix") == -1) throw RuntimeException("GPS not valid")
                location.accuracy = 100f
                location.latitude = json.getDouble("latitude")
                location.longitude = json.getDouble("longitude")
                json.optDouble("speed").takeIf { !it.isNaN() }?.let {
                    location.speed = it.toFloat()
                    LocationCompat.setSpeedAccuracyMetersPerSecond(location, location.speed * 0.1f)
                }
                location.time = json.getLong("timestamp")
                json.optDouble("heading").takeIf { !it.isNaN() }?.let {
                    location.bearing = it.toFloat()
                    LocationCompat.setBearingAccuracyDegrees(location, 90f)
                }
                return location
            }
        }

        private val SOURCE_OMBORD = object : MovingWifiLocationSource("https://www.ombord.info/api/jsonp/position/") {
            override fun parse(location: Location, data: ByteArray): Location {
                // The API endpoint returns a JSONP object (even when no ?callback= is supplied), so strip the surrounding function call.
                val json = JSONObject(data.decodeToString().trim().trim('(', ')', ';'))
                // TODO: what happens in the Channel Tunnel? Does "satellites" go to zero? Does "mode" change?
                if (json.has("satellites") && json.getInt("satellites") < 1) throw RuntimeException("Ombord has no GPS fix")
                location.accuracy = 100f
                location.latitude = json.getDouble("latitude")
                location.longitude = json.getDouble("longitude")
                json.optDouble("speed").takeIf { !it.isNaN() }?.let {
                    location.speed = it.toFloat()
                    LocationCompat.setSpeedAccuracyMetersPerSecond(location, location.speed * 0.1f)
                }
                location.time = json.getLong("time")
                // "cmg" means "course made good", i.e. the compass heading of the track over the ground.
                // Sometimes gets stuck for a few minutes, so use a generous accuracy value.
                json.optDouble("cmg").takeIf { !it.isNaN() }?.let {
                    location.bearing = it.toFloat()
                    LocationCompat.setBearingAccuracyDegrees(location, 90f)
                }
                json.optDouble("altitude").takeIf { !it.isNaN() }?.let {
                    location.altitude = it
                }
                return location
            }
        }

        private val SOURCE_AIR_CANADA = object : MovingWifiLocationSource("https://airbornemedia.inflightinternet.com/asp/api/flight/info") {
            override fun parse(location: Location, data: ByteArray): Location {
                val json = JSONObject(data.decodeToString()).getJSONObject("gpsData")
                location.accuracy = 100f
                location.latitude = json.getDouble("latitude")
                location.longitude = json.getDouble("longitude")
                json.optLong("utcTime").takeIf { it != 0L }?.let { location.time = it }
                json.optDouble("altitude").takeIf { !it.isNaN() }?.let { location.altitude = it * FEET_TO_METERS }
                json.optDouble("horizontalVelocity").takeIf { !it.isNaN() }?.let {
                    location.speed = (it * MILES_PER_HOUR_TO_METERS_PER_SECOND).toFloat()
                    LocationCompat.setSpeedAccuracyMetersPerSecond(location, location.speed * 0.1f)
                }
                return location
            }
        }

        private val SOURCE_HOTSPLOTS = object : MovingWifiLocationSource("http://hsp.hotsplots.net/status.json") {
            override fun parse(location: Location, data: ByteArray): Location {
                val json = JSONObject(data.decodeToString())
                location.accuracy = 100f
                location.latitude = json.getDouble("lat")
                location.longitude = json.getDouble("lng")
                json.optLong("ts").takeIf { it != 0L }?.let { location.time = it * 1000 }
                json.optDouble("speed").takeIf { !it.isNaN() }?.let {
                    location.speed = it.toFloat()
                    LocationCompat.setSpeedAccuracyMetersPerSecond(location, location.speed * 0.1f)
                }
                return location
            }
        }

        private val MOVING_WIFI_HOTSPOTS_LOCALLY_RETRIEVABLE: Map<String, List<MovingWifiLocationSource>> = mapOf(
            "WIFIonICE" to listOf(SOURCE_WIFI_ON_ICE),
            "OEBB" to listOf(SOURCE_OEBB_2, SOURCE_OEBB_1),
            "FlixBus" to listOf(SOURCE_FLIXBUS),
            "FlixBus Wi-Fi" to listOf(SOURCE_FLIXBUS),
            "FlixTrain Wi-Fi" to listOf(SOURCE_FLIXBUS),
            "MAVSTART-WIFI" to listOf(SOURCE_PASSENGERA_MAV),
            "AegeanWiFi" to listOf(SOURCE_DISPLAY_UGO),
            "Telekom_FlyNet" to listOf(SOURCE_INFLIGHT_PANASONIC),
            "Cathay Pacific" to listOf(SOURCE_INFLIGHT_PANASONIC),
            "KrisWorld" to listOf(SOURCE_INFLIGHT_PANASONIC),
            "SWISS Connect" to listOf(SOURCE_INFLIGHT_PANASONIC),
            "Edelweiss Entertainment" to listOf(SOURCE_INFLIGHT_PANASONIC),
            "FlyNet" to listOf(SOURCE_LUFTHANSA_FLYNET_EUROPE, SOURCE_LUFTHANSA_FLYNET_EUROPE_2),
            "CDWiFi" to listOf(SOURCE_PASSENGERA_CD),
            "Air Canada" to listOf(SOURCE_AIR_CANADA),
            "ACWiFi" to listOf(SOURCE_AIR_CANADA),
            "ACWiFi.com" to listOf(SOURCE_AIR_CANADA),
            "OUIFI" to listOf(SOURCE_OUIFI),
            "_SNCF_WIFI_INOUI" to listOf(SOURCE_SNCF),
            "_SNCF_WIFI_INTERCITES" to listOf(SOURCE_SNCF_INTERCITES),
            "_WIFI_LYRIA" to listOf(SOURCE_LYRIA),
            "NormandieTrainConnecte" to listOf(SOURCE_NORMANDIE),
            "agilis-Wifi" to listOf(SOURCE_HOTSPLOTS),
            "Austrian FlyNet" to listOf(SOURCE_AUSTRIAN_FLYNET_EUROPE),
            "EurostarTrainsWiFi" to listOf(SOURCE_OMBORD),
        )
    }
}

