/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.mapbox

import android.util.Log
import com.google.android.gms.maps.model.TileProvider
import com.mapbox.mapboxsdk.module.http.HttpRequestUtil
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.ConcurrentHashMap

object TileProviderHttpAdapter {

    private const val TAG = "GmsMapTileAdapter"
    private const val SENTINEL_HOST = "tileoverlay.microg.invalid"
    private const val TILE_PATH_SEGMENTS = 4
    private const val HTTP_OK = 200
    private const val HTTP_NOT_FOUND = 404

    private val providersByOverlayKey = ConcurrentHashMap<String, TileProvider>()
    private val pngMediaType = "image/png".toMediaTypeOrNull()

    @Volatile
    private var interceptorInstalled = false

    fun tileUrlTemplate(overlayKey: String): String = "https://$SENTINEL_HOST/$overlayKey/{z}/{x}/{y}"

    @Synchronized
    fun register(overlayKey: String, provider: TileProvider) {
        ensureInterceptorInstalled()
        providersByOverlayKey[overlayKey] = provider
    }

    fun unregister(overlayKey: String) {
        providersByOverlayKey.remove(overlayKey)
    }

    private fun ensureInterceptorInstalled() {
        if (interceptorInstalled) {
            return
        }

        interceptorInstalled = true
        HttpRequestUtil.setOkHttpClient(
            OkHttpClient.Builder()
                .addInterceptor(SentinelTileInterceptor())
                .build()
        )
    }

    private fun fetchTileBytes(overlayKey: String, x: Int, y: Int, zoom: Int): ByteArray? {
        val provider = providersByOverlayKey[overlayKey] ?: return null

        return try {
            provider.getTile(x, y, zoom)?.data
        } catch (e: Exception) {
            Log.w(TAG, "getTile failed for $overlayKey/$zoom/$x/$y", e)
            null
        }
    }

    private class SentinelTileInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            if (request.url.host != SENTINEL_HOST) {
                return chain.proceed(request)
            }

            val tileBytes = resolveTileBytes(request.url.pathSegments)
            return tileResponse(request, tileBytes)
        }

        private fun resolveTileBytes(pathSegments: List<String>): ByteArray? {
            if (pathSegments.size != TILE_PATH_SEGMENTS) {
                return null
            }

            val overlayKey = pathSegments[0]
            val zoom = pathSegments[1].toIntOrNull() ?: return null
            val x = pathSegments[2].toIntOrNull() ?: return null
            val y = pathSegments[3].toIntOrNull() ?: return null

            return fetchTileBytes(overlayKey, x, y, zoom)
        }

        private fun tileResponse(request: Request, tileBytes: ByteArray?): Response {
            val builder = Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)

            return if (tileBytes != null) {
                builder.code(HTTP_OK).message("OK").body(tileBytes.toResponseBody(pngMediaType)).build()
            } else {
                builder.code(HTTP_NOT_FOUND).message("No tile").body(ByteArray(0).toResponseBody(pngMediaType)).build()
            }
        }
    }
}
