/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.mozilla

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.util.Log
import com.android.volley.Request.Method
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.microg.gms.location.network.NetworkLocationService
import org.microg.gms.location.network.cell.CellDetails
import org.microg.gms.location.network.precision
import org.microg.gms.location.network.wifi.WifiDetails
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.log10
import kotlin.math.log2

class MozillaLocationServiceClient(context: Context) {
    private val queue = Volley.newRequestQueue(context)

    suspend fun retrieveMultiWifiLocation(wifis: List<WifiDetails>): Location = geoLocate(
        GeolocateRequest(
            considerIp = false,
            wifiAccessPoints = wifis.filter { it.ssid?.endsWith("_nomap") != true }.map(WifiDetails::toWifiAccessPoint),
            fallbacks = Fallback(lacf = false, ipf = false)
        )
    ).apply {
        precision = WIFI_BASE_PRECISION_COUNT/wifis.size.toDouble()
    }

    suspend fun retrieveSingleCellLocation(cell: CellDetails): Location = geoLocate(
        GeolocateRequest(
            considerIp = false,
            cellTowers = listOf(cell.toCellTower()),
            fallbacks = Fallback(
                lacf = true,
                ipf = false
            )
        )
    ).apply {
        precision = if (extras?.getString(LOCATION_EXTRA_FALLBACK) != null) CELL_FALLBACK_PRECISION else CELL_DEFAULT_PRECISION
    }

    private suspend fun geoLocate(request: GeolocateRequest): Location {
        val response = rawGeoLocate(request)
        Log.d(TAG, "$request -> $response")
        if (response.location != null) {
            return Location("mozilla").apply {
                latitude = response.location.lat
                longitude = response.location.lng
                if (response.accuracy != null) accuracy = response.accuracy.toFloat()
                if (response.fallback != null) extras = Bundle().apply { putString(LOCATION_EXTRA_FALLBACK, response.fallback) }
            }
        } else if (response.error != null) {
            throw ServiceException(response.error)
        } else {
            throw RuntimeException("Invalid response JSON")
        }
    }

    private suspend fun rawGeoLocate(request: GeolocateRequest): GeolocateResponse = suspendCoroutine { continuation ->
        queue.add(JsonObjectRequest(Method.POST, GEOLOCATE_URL.format(API_KEY), request.toJson(), {
            continuation.resume(it.toGeolocateResponse())
        }, {
            continuation.resumeWithException(it)
        }))
    }

    companion object {
        private const val TAG = "MozillaLocation"
        private const val GEOLOCATE_URL = "https://location.services.mozilla.com/v1/geolocate?key=%s"
        private const val API_KEY = "068ab754-c06b-473d-a1e5-60e7b1a2eb77"
        private const val WIFI_BASE_PRECISION_COUNT = 8.0
        private const val CELL_DEFAULT_PRECISION = 1.0
        private const val CELL_FALLBACK_PRECISION = 0.5
        const val LOCATION_EXTRA_FALLBACK = "fallback"
    }

}