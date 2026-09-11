/*
 * SPDX-FileCopyrightText: 2020 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.mapbox.model

import android.os.Parcel
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.maps.model.internal.ITileOverlayDelegate
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.RasterLayer
import com.mapbox.mapboxsdk.style.sources.RasterSource
import com.mapbox.mapboxsdk.style.sources.TileSet
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.maps.mapbox.TileProviderHttpAdapter
import org.microg.gms.utils.warnOnTransactionIssues

class TileOverlayImpl(private val map: GoogleMapImpl, private val id: String, options: TileOverlayOptions) : ITileOverlayDelegate.Stub() {

    private var zIndex = options.zIndex
    private var visible = options.isVisible
    private var fadeIn = options.fadeIn
    private var transparency = options.transparency
    private val tileProvider: TileProvider? = runCatching { options.tileProvider }.getOrNull()

    private val sourceId = "gms-tileoverlay-src-$id"
    private val layerId = "gms-tileoverlay-layer-$id"

    init {
        tileProvider?.let { TileProviderHttpAdapter.register(id, it) }
    }

    fun addToStyle(style: Style) {
        if (tileProvider == null || style.getLayer(layerId) != null) {
            return
        }

        if (style.getSource(sourceId) == null) {
            val tileSet = TileSet("2.1.0", TileProviderHttpAdapter.tileUrlTemplate(id))
            style.addSource(RasterSource(sourceId, tileSet, TILE_SIZE))
        }

        style.addLayer(
            RasterLayer(layerId, sourceId).withProperties(
                PropertyFactory.rasterOpacity(opacity()),
                PropertyFactory.visibility(visibilityValue()),
                PropertyFactory.rasterFadeDuration(if (fadeIn) FADE_DURATION_MS else 0f)
            )
        )
    }

    private fun opacity(): Float = 1f - transparency

    private fun visibilityValue(): String = if (visible) Property.VISIBLE else Property.NONE

    private fun updateLayer(block: (RasterLayer) -> Unit) {
        map.map?.getStyle { style ->
            (style.getLayer(layerId) as? RasterLayer)?.let(block)
        }
    }

    override fun remove() {
        TileProviderHttpAdapter.unregister(id)
        map.tileOverlays.remove(id)
        map.map?.getStyle { style ->
            style.removeLayer(layerId)
            style.removeSource(sourceId)
        }
    }

    override fun clearTileCache() {
        map.map?.getStyle { style ->
            style.removeLayer(layerId)
            style.removeSource(sourceId)
            addToStyle(style)
        }
    }

    override fun getId(): String = id

    override fun setZIndex(zIndex: Float) {
        this.zIndex = zIndex
    }

    override fun getZIndex(): Float = zIndex

    override fun setVisible(visible: Boolean) {
        this.visible = visible
        updateLayer { it.setProperties(PropertyFactory.visibility(visibilityValue())) }
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
        updateLayer { it.setProperties(PropertyFactory.rasterOpacity(opacity())) }
    }

    override fun getTransparency(): Float = transparency

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }

    companion object {
        private const val TAG = "TileOverlay"
        private const val TILE_SIZE = 256
        private const val FADE_DURATION_MS = 300f
    }
}
