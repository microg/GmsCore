/*
 * SPDX-FileCopyrightText: 2020 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.mapbox.model

import android.os.Parcel
import android.util.Log
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.maps.model.internal.ITileOverlayDelegate
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.RasterLayer
import com.mapbox.mapboxsdk.style.sources.RasterSource
import com.mapbox.mapboxsdk.style.sources.TileSet
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.utils.warnOnTransactionIssues

private const val TILE_SIZE = 256

class TileOverlayImpl(private val map: GoogleMapImpl, private val id: String, options: TileOverlayOptions) : ITileOverlayDelegate.Stub() {
    private val sourceId = "tileoverlay-source-$id"
    private val layerId = "tileoverlay-layer-$id"
    private val tileProvider: TileProvider? = try {
        options.tileProvider
    } catch (e: Exception) {
        Log.w(TAG, "No tile provider for overlay $id", e)
        null
    }

    private var zIndex = options.zIndex
    private var visible = options.isVisible
    private var fadeIn = options.fadeIn
    private var transparency = options.transparency
    private var added = false
    private var serverToken: String? = null
    private var cacheEpoch = 0L

    /** Called on the map thread once the style is ready. Adds the raster source + layer. */
    fun update(style: Style) {
        val provider = tileProvider ?: return
        try {
            val token = serverToken ?: TileProviderServer.register(provider).also { serverToken = it }
            if (style.getSource(sourceId) == null) {
                val tileSet = TileSet("2.2.0", TileProviderServer.tileUrl(token, cacheEpoch)).apply {
                    minZoom = 0f
                    maxZoom = 22f
                }
                style.addSource(RasterSource(sourceId, tileSet, TILE_SIZE))
            }
            if (style.getLayer(layerId) == null) {
                val layer = RasterLayer(layerId, sourceId)
                layer.setProperties(
                    PropertyFactory.rasterOpacity(currentOpacity()),
                    PropertyFactory.rasterFadeDuration(if (fadeIn) 300f else 0f)
                )
                style.addLayer(layer)
            }
            added = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to add tile overlay $id", e)
        }
    }

    private fun currentOpacity(): Float = if (visible) (1f - transparency).coerceIn(0f, 1f) else 0f

    private fun applyOpacity() {
        if (!added) return
        map.map?.getStyle { style ->
            try {
                style.getLayerAs<RasterLayer>(layerId)?.setProperties(PropertyFactory.rasterOpacity(currentOpacity()))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update tile overlay $id opacity", e)
            }
        }
    }

    override fun remove() {
        serverToken?.let { TileProviderServer.unregister(it) }
        serverToken = null
        map.map?.getStyle { style ->
            try {
                style.removeLayer(layerId)
                style.removeSource(sourceId)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove tile overlay $id", e)
            }
        }
        added = false
    }

    override fun clearTileCache() {
        map.map?.getStyle { style ->
            try {
                if (style.getLayer(layerId) != null) style.removeLayer(layerId)
                if (style.getSource(sourceId) != null) style.removeSource(sourceId)
                added = false
                // Bump the epoch so the re-added source uses a fresh URL; otherwise MapLibre
                // serves the previously cached tiles and never re-invokes the provider.
                cacheEpoch++
                update(style)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear tile cache for overlay $id", e)
            }
        }
    }

    override fun getId(): String = id

    override fun setZIndex(zIndex: Float) {
        this.zIndex = zIndex
    }

    override fun getZIndex(): Float = zIndex

    override fun setVisible(visible: Boolean) {
        this.visible = visible
        applyOpacity()
    }

    override fun isVisible(): Boolean = visible

    override fun equalsRemote(other: ITileOverlayDelegate?): Boolean = this == other

    override fun hashCodeRemote(): Int = hashCode()

    override fun setFadeIn(fadeIn: Boolean) {
        this.fadeIn = fadeIn
    }

    override fun getFadeIn(): Boolean = fadeIn

    override fun setTransparency(transparency: Float) {
        this.transparency = transparency
        applyOpacity()
    }

    override fun getTransparency(): Float = transparency

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }

    companion object {
        private const val TAG = "TileOverlay"
    }
}
