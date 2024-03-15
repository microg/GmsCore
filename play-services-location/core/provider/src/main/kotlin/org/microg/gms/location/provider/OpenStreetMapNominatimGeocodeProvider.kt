/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.provider

import android.content.Context
import android.location.Address
import android.location.GeocoderParams
import android.net.Uri
import android.util.Log
import android.util.LruCache
import com.android.location.provider.GeocodeProvider
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.internal.ClientIdentity
import org.json.JSONObject
import org.microg.address.Formatter
import org.microg.gms.location.LocationSettings
import org.microg.gms.utils.singleInstanceOf
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


class OpenStreetMapNominatimGeocodeProvider(private val context: Context) : GeocodeProvider() {
    private val queue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }
    private val formatter = runCatching { Formatter() }.getOrNull()
    private val addressCache = LruCache<CacheKey, Address>(CACHE_SIZE)
    private val settings by lazy { LocationSettings(context) }

    override fun onGetFromLocation(latitude: Double, longitude: Double, maxResults: Int, params: GeocoderParams, addresses: MutableList<Address>): String? {
        val clientIdentity = params.clientIdentity ?: return "null client package"
        val locale = params.locale ?: return "null locale"
        if (!settings.geocoderNominatim) return "disabled"
        val cacheKey = CacheKey(clientIdentity, locale, latitude, longitude)
        addressCache[cacheKey]?.let {address ->
            addresses.add(address)
            return null
        }
        val uri = Uri.Builder()
            .scheme("https").authority(NOMINATIM_SERVER).path("/reverse")
            .appendQueryParameter("format", "json")
            .appendQueryParameter("accept-language", locale.language)
            .appendQueryParameter("addressdetails", "1")
            .appendQueryParameter("lat", latitude.toString())
            .appendQueryParameter("lon", longitude.toString())
        val result = AtomicReference<String?>("timeout reached")
        val returnedAddress = AtomicReference<Address?>(null)
        val latch = CountDownLatch(1)
        queue.add(object : JsonObjectRequest(uri.build().toString(), {
            parseResponse(locale, it)?.let(returnedAddress::set)
            result.set(null)
            latch.countDown()
        }, {
            result.set(it.message)
            latch.countDown()
        }) {
            override fun getHeaders(): Map<String, String> = mapOf("User-Agent" to "microG/${context.versionName}")
        })
        latch.await(5, TimeUnit.SECONDS)
        val address = returnedAddress.get()
        if (address != null) {
            Log.d(TAG, "Returned $address for $latitude,$longitude")
            addresses.add(address)
            addressCache.put(cacheKey, address)
        }
        return result.get()
    }

    override fun onGetFromLocationName(
        locationName: String,
        lowerLeftLatitude: Double,
        lowerLeftLongitude: Double,
        upperRightLatitude: Double,
        upperRightLongitude: Double,
        maxResults: Int,
        params: GeocoderParams,
        addresses: MutableList<Address>
    ): String? {
        val clientIdentity = params.clientIdentity ?: return "null client package"
        val locale = params.locale ?: return "null locale"
        if (!settings.geocoderNominatim) return "disabled"
        val uri = Uri.Builder()
            .scheme("https").authority(NOMINATIM_SERVER).path("/search")
            .appendQueryParameter("format", "json")
            .appendQueryParameter("accept-language", locale.language)
            .appendQueryParameter("addressdetails", "1")
            .appendQueryParameter("bounded", "1")
            .appendQueryParameter("q", locationName)
            .appendQueryParameter("limit", maxResults.toString())
        if (lowerLeftLatitude != upperRightLatitude && lowerLeftLongitude != upperRightLongitude) {
            uri.appendQueryParameter("viewbox", "$lowerLeftLongitude,$upperRightLatitude,$upperRightLongitude,$lowerLeftLatitude")
        }
        val result = AtomicReference<String?>("timeout reached")
        val latch = CountDownLatch(1)
        queue.add(object : JsonArrayRequest(uri.build().toString(), {
            for (i in 0 until it.length()) {
                parseResponse(locale, it.getJSONObject(i))?.let(addresses::add)
            }
            result.set(null)
            latch.countDown()
        }, {
            result.set(it.message)
            latch.countDown()
        }) {
            override fun getHeaders(): Map<String, String> = mapOf("User-Agent" to "microG/${context.versionName}")
        })
        latch.await(5, TimeUnit.SECONDS)
        return result.get()
    }

    private fun parseResponse(locale: Locale, result: JSONObject): Address? {
        if (!result.has(WIRE_LATITUDE) || !result.has(WIRE_LONGITUDE) ||
            !result.has(WIRE_ADDRESS)
        ) {
            return null
        }
        Log.d(TAG, "Result: $result")
        val address = Address(locale)
        address.latitude = result.getDouble(WIRE_LATITUDE)
        address.longitude = result.getDouble(WIRE_LONGITUDE)
        val a = result.getJSONObject(WIRE_ADDRESS)
        address.thoroughfare = a.optString(WIRE_THOROUGHFARE)
        address.subLocality = a.optString(WIRE_SUBLOCALITY)
        address.postalCode = a.optString(WIRE_POSTALCODE)
        address.subAdminArea = a.optString(WIRE_SUBADMINAREA)
        address.adminArea = a.optString(WIRE_ADMINAREA)
        address.countryName = a.optString(WIRE_COUNTRYNAME)
        address.countryCode = a.optString(WIRE_COUNTRYCODE)
        if (a.has(WIRE_LOCALITY_CITY)) {
            address.locality = a.getString(WIRE_LOCALITY_CITY)
        } else if (a.has(WIRE_LOCALITY_TOWN)) {
            address.locality = a.getString(WIRE_LOCALITY_TOWN)
        } else if (a.has(WIRE_LOCALITY_VILLAGE)) {
            address.locality = a.getString(WIRE_LOCALITY_VILLAGE)
        }
        if (formatter != null) {
            val components = mutableMapOf<String, String>()
            for (s in a.keys()) {
                if (s !in WIRE_IGNORED) {
                    components[s] = a[s].toString()
                }
            }
            val split = formatter.formatAddress(components).split("\n")
            for (i in split.indices) {
                address.setAddressLine(i, split[i])
            }
            address.featureName = formatter.guessName(components)
        }
        return address
    }

    fun dump(writer: PrintWriter?) {
        writer?.println("Enabled: ${settings.geocoderNominatim}")
        writer?.println("Address cache: size=${addressCache.size()} hits=${addressCache.hitCount()} miss=${addressCache.missCount()} puts=${addressCache.putCount()} evicts=${addressCache.evictionCount()}")
    }

    companion object {
        private const val CACHE_SIZE = 200

        private const val NOMINATIM_SERVER = "nominatim.openstreetmap.org"

        private const val WIRE_LATITUDE = "lat"
        private const val WIRE_LONGITUDE = "lon"
        private const val WIRE_ADDRESS = "address"
        private const val WIRE_THOROUGHFARE = "road"
        private const val WIRE_SUBLOCALITY = "suburb"
        private const val WIRE_POSTALCODE = "postcode"
        private const val WIRE_LOCALITY_CITY = "city"
        private const val WIRE_LOCALITY_TOWN = "town"
        private const val WIRE_LOCALITY_VILLAGE = "village"
        private const val WIRE_SUBADMINAREA = "county"
        private const val WIRE_ADMINAREA = "state"
        private const val WIRE_COUNTRYNAME = "country"
        private const val WIRE_COUNTRYCODE = "country_code"

        private val WIRE_IGNORED = setOf<String>("ISO3166-2-lvl4")

        private data class CacheKey(val uid: Int, val packageName: String?, val locale: Locale, val latitude: Int, val longitude: Int) {
            constructor(clientIdentity: ClientIdentity, locale: Locale, latitude: Double, longitude: Double) : this(clientIdentity.uid, clientIdentity.packageName.takeIf { clientIdentity.uid != 0 }, locale, (latitude * 100000.0).toInt(), (longitude * 100000.0).toInt())
        }
    }
}