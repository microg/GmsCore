/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.mozilla

import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.android.volley.Request.Method
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.microg.gms.location.provider.BuildConfig
import org.microg.gms.location.network.precision
import org.microg.gms.location.network.wifi.WifiDetails
import org.microg.gms.location.network.wifi.isMoving
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MozillaLocationServiceClient(context: Context) {
    private val queue = Volley.newRequestQueue(context)

    suspend fun retrieveMultiWifiLocation(wifis: List<org.microg.gms.location.network.wifi.WifiDetails>): Location = geoLocate(
        GeolocateRequest(
            considerIp = false,
            wifiAccessPoints = wifis.filter { it.ssid?.endsWith("_nomap") != true && !it.isMoving }.map(WifiDetails::toWifiAccessPoint),
            fallbacks = Fallback(lacf = false, ipf = false)
        )
    ).apply {
        precision = wifis.size.toDouble()/ WIFI_BASE_PRECISION_COUNT
    }

    suspend fun retrieveSingleCellLocation(cell: org.microg.gms.location.network.cell.CellDetails): Location = geoLocate(
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
        queue.add(JsonObjectRequest(Method.POST, Uri.parse(GEOLOCATE_URL).buildUpon().apply {
            if (API_KEY != null) appendQueryParameter("key", API_KEY)
        }.build().toString(), request.toJson(), {
            continuation.resume(it.toGeolocateResponse())
        }, {
            continuation.resumeWithException(it)
        }))
    }

    companion object {
        private const val TAG = "MozillaLocation"
        private const val GEOLOCATE_URL = "https://location.services.mozilla.com/v1/geolocate"
        private const val WIFI_BASE_PRECISION_COUNT = 4.0
        private const val CELL_DEFAULT_PRECISION = 1.0
        private const val CELL_FALLBACK_PRECISION = 0.5
        @JvmField
        val API_KEY: String? = BuildConfig.ICHNAEA_KEY.takeIf { it.isNotBlank() }
        const val LOCATION_EXTRA_FALLBACK = "fallback"
    }

}