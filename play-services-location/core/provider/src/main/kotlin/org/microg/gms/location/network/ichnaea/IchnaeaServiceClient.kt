/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.ichnaea

import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.android.volley.Request.Method
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import org.microg.gms.location.LocationSettings
import org.microg.gms.location.network.cell.CellDetails
import org.microg.gms.location.network.precision
import org.microg.gms.location.network.wifi.WifiDetails
import org.microg.gms.location.network.wifi.isMoving
import org.microg.gms.location.provider.BuildConfig
import org.microg.gms.utils.singleInstanceOf
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class IchnaeaServiceClient(private val context: Context) {
    private val queue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }
    private val settings = LocationSettings(context)

    suspend fun retrieveMultiWifiLocation(wifis: List<WifiDetails>): Location = geoLocate(
        GeolocateRequest(
            considerIp = false,
            wifiAccessPoints = wifis.filter { it.ssid?.endsWith("_nomap") != true && !it.isMoving }.map(WifiDetails::toWifiAccessPoint),
            fallbacks = Fallback(lacf = false, ipf = false)
        )
    ).apply {
        precision = wifis.size.toDouble() / WIFI_BASE_PRECISION_COUNT
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
            return Location("ichnaea").apply {
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
            } else if (response.location?.lat != null){
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
        val url = Uri.parse(settings.ichneaeEndpoint).buildUpon().appendPath("v1").appendPath("geolocate").build().toString()
        queue.add(object : JsonObjectRequest(Method.POST, url, request.toJson(), {
            continuation.resume(it.toGeolocateResponse())
        }, {
            continueError(continuation, it)
        }) {
            override fun getHeaders(): Map<String, String> = getRequestHeaders()
        })
    }

    private suspend fun rawGeoSubmit(request: GeosubmitRequest): Unit = suspendCoroutine { continuation ->
        val url = Uri.parse(settings.ichneaeEndpoint).buildUpon().appendPath("v2").appendPath("geosubmit").build().toString()
        queue.add(object : JsonObjectRequest(Method.POST, url, request.toJson(), {
            continuation.resume(Unit)
        }, {
            continuation.resumeWithException(it)
        }) {
            override fun getHeaders(): Map<String, String> = getRequestHeaders()
        })
    }

    companion object {
        private const val TAG = "IchnaeaLocation"
        private const val WIFI_BASE_PRECISION_COUNT = 4.0
        private const val CELL_DEFAULT_PRECISION = 1.0
        private const val CELL_FALLBACK_PRECISION = 0.5
        const val LOCATION_EXTRA_FALLBACK = "fallback"
    }

}