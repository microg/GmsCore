package org.microg.gms.maps.mapbox.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import com.google.android.gms.maps.model.TileProvider
import com.mapbox.mapboxsdk.ModuleProviderImpl
import com.mapbox.mapboxsdk.http.HttpRequest
import com.mapbox.mapboxsdk.http.HttpResponder
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern

class TileOverlayRequestInterceptor(val actualHttpClient: HttpRequest) :
    HttpRequest by actualHttpClient {

    companion object {
        private const val TAG = "TileOvHttpReq"
        private const val CACHE_SIZE = 100
        val TILEJSON_URL_REGEX: Pattern =
            Pattern.compile("^https://maplibre-tile-overlay-provider\\.invalid-domain\\.microg\\.org/provider/(\\d+)/tiles.json(?:[?].*)?$")
        val TILES_URL_REGEX: Pattern =
            Pattern.compile("^https://maplibre-tile-overlay-provider\\.invalid-domain\\.microg\\.org/provider/(\\d+)/tiles/(\\d+)/(\\d+)/(\\d+)\\.(\\w+)(?:[?].*)?$")
    }

    private val tileCaches = mutableListOf<LruCache<String, ByteArray>>()
    val tileProviders = mutableListOf<TileProvider>()

    fun registerProvider(tileProvider: TileProvider): Int {
        synchronized(tileProviders) {
            val id = tileProviders.size
            tileProviders.add(tileProvider)
            tileCaches.add(LruCache(CACHE_SIZE))
            return id
        }
    }

    fun clearTileCache(providerId: Int) {
        synchronized(tileProviders) {
            tileCaches.getOrNull(providerId)?.evictAll()
        }
    }

    fun getProviderUrl(providerId: Int): String {
        return "https://maplibre-tile-overlay-provider.invalid-domain.microg.org/provider/$providerId/tiles.json?nocache=${System.currentTimeMillis()}"
    }

    // minzoom and maxzoom set to extremely high values should ensure the zoom level is capped only
    // by the map's global min/max zoom level
    private fun getTileJson(providerId: Int) = """
        {
          "tilejson": "3.0.0",
          "tiles": [
            "https://maplibre-tile-overlay-provider.invalid-domain.microg.org/provider/${providerId}/tiles/{z}/{x}/{y}.png?nocache=${System.currentTimeMillis()}"
          ],
          "minzoom": 0,
          "maxzoom": 100,
          "name": "gmaps-overlay-${providerId}",
          "scheme": "xyz"
        }
    """.trimIndent().trim().toByteArray()

    private fun isPng(data: ByteArray): Boolean =
        data.size >= 8 &&
                data[0] == 0x89.toByte() && data[1] == 0x50.toByte() &&
                data[2] == 0x4E.toByte() && data[3] == 0x47.toByte()

    override fun executeRequest(
        httpRequest: HttpResponder,
        nativePtr: Long,
        resourceUrl: String,
        dataRange: String,
        etag: String,
        offlineUsage: Boolean
    ) {
        TILEJSON_URL_REGEX.matcher(resourceUrl).apply {
            if (matches()) {
                Log.d(TAG, "Intercepting TileJSON request: $resourceUrl")

                val providerId = group(1)?.toIntOrNull()
                if (providerId == null) {
                    httpRequest.handleFailure(400, "Invalid tile URL")
                    return
                }
                if (providerId >= tileProviders.size) {
                    httpRequest.handleFailure(404, "Tile provider not found")
                    return
                }
                httpRequest.onResponse(
                    200,
                    null,
                    null,
                    "no-store", // prevent MapLibre from caching TileJSON in SQLite
                    null,
                    null,
                    null,
                    getTileJson(providerId)
                )
                return
            }
        }

        TILES_URL_REGEX.matcher(resourceUrl).apply {
            if (matches()) {
                Log.d(TAG, "Intercepting tile request: $resourceUrl")
                val providerId = group(1)?.toIntOrNull()
                val z = group(2)?.toIntOrNull()
                val x = group(3)?.toIntOrNull()
                val y = group(4)?.toIntOrNull()
                val ext = group(5)

                if (z == null || y == null || x == null || providerId == null || ext != "png") {
                    httpRequest.handleFailure(400, "Invalid tile URL")
                    return
                }

                // Check in-wrapper cache first
                val cacheKey = "$z/$x/$y"
                val cached: ByteArray?
                synchronized(tileProviders) {
                    cached = tileCaches.getOrNull(providerId)?.get(cacheKey)
                }
                if (cached != null) {
                    Log.d(TAG, "Serving tile from cache: $cacheKey (provider $providerId)")
                    httpRequest.onResponse(200, null, null, "no-store", null, null, null, cached)
                    return
                }

                val tileProvider: TileProvider
                synchronized(tileProviders) {
                    if (providerId >= tileProviders.size) {
                        httpRequest.handleFailure(404, "Tile provider not found")
                        return
                    }
                    tileProvider = tileProviders[providerId]
                }

                val tile = tileProvider.getTile(x, y, z)
                if (tile == TileProvider.NO_TILE) {
                    httpRequest.handleFailure(404, "Tile not found")
                    return
                }
                if (tile == null || tile.data == null || tile.data.size == 0) {
                    httpRequest.handleFailure(502, "App provided no tile data")
                    Log.w(
                        TAG,
                        "Tile provider returned a tile with null data for ($x, $y) (zoom: $z)"
                    )
                    return
                }

                val image = if (isPng(tile.data)) {
                    tile.data
                } else {
                    ByteArrayOutputStream().apply {
                        BitmapFactory.decodeByteArray(tile.data, 0, tile.data.size)
                            .compress(Bitmap.CompressFormat.PNG, 100, this)
                    }.toByteArray()
                }

                synchronized(tileProviders) {
                    tileCaches.getOrNull(providerId)?.put(cacheKey, image)
                    null
                }

                httpRequest.onResponse(
                    200,
                    null,
                    null,
                    "no-store", // prevent MapLibre from caching tile in SQLite
                    null,
                    null,
                    null,
                    image
                )
                return
            } else {
                Log.d(TAG, "Forwarding regular HTTP request: $resourceUrl")
                return actualHttpClient.executeRequest(
                    httpRequest,
                    nativePtr,
                    resourceUrl,
                    dataRange,
                    etag,
                    offlineUsage
                )
            }
        }
    }
}

object TileOverlayRequestInterceptorModuleProvider : ModuleProviderImpl() {
    val interceptor by lazy {
        TileOverlayRequestInterceptor(super.createHttpRequest())
    }

    override fun createHttpRequest(): HttpRequest = interceptor
}
