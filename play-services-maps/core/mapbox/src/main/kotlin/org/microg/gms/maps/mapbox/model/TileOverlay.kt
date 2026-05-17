/*
 * SPDX-FileCopyrightText: 2020 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.mapbox.model

import android.os.Parcel
import android.util.Log
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.internal.ITileOverlayDelegate
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.rasterFadeDuration
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.rasterOpacity
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility
import com.mapbox.mapboxsdk.style.layers.RasterLayer
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.utils.warnOnTransactionIssues

class TileOverlayImpl(
    private val map: GoogleMapImpl,
    private val id: String,
    options: TileOverlayOptions,
    private val providerId: Int
) : ITileOverlayDelegate.Stub() {
    private var zIndex = options.zIndex
    private var visible = options.isVisible
    private var fadeIn = options.fadeIn
    private var transparency = options.transparency

    override fun remove() {
        Log.d(TAG, "remove")
        visible = false
        map.removeRasterLayer(zIndex, providerId)
    }

    override fun clearTileCache() {
        Log.d(TAG, "clearTileCache")
        map.removeRasterLayer(zIndex, providerId)
        map.clearTileOverlayProviderCache(providerId)
        update()
    }

    override fun getId(): String = id

    override fun setZIndex(zIndex: Float) {
        Log.d(TAG, "setZIndex: $zIndex")
        if (zIndex == this.zIndex) return

        map.removeRasterLayer(this.zIndex, providerId)
        this.zIndex = zIndex
        update()
    }

    override fun getZIndex(): Float = zIndex

    override fun setVisible(visible: Boolean) {
        Log.d(TAG, "setVisible: $visible")
        this.visible = visible
        update()
    }

    override fun isVisible(): Boolean = visible

    override fun equalsRemote(other: ITileOverlayDelegate?): Boolean = this == other

    override fun hashCodeRemote(): Int = hashCode()

    override fun setFadeIn(fadeIn: Boolean) {
        this.fadeIn = fadeIn
        update()
    }

    override fun getFadeIn(): Boolean = fadeIn

    override fun setTransparency(transparency: Float) {
        Log.d(TAG, "setTransparency: $transparency")
        this.transparency = transparency
        update()
    }

    override fun getTransparency(): Float = transparency

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }

    fun update() {
        map.getOrCreateRasterLayerForZIndex(zIndex, providerId)?.let { update(it) }
    }

    fun update(layer: RasterLayer) {
        layer.setProperties(
            visibility(if (visible) Property.VISIBLE else Property.NONE),
            rasterOpacity(1 - transparency),
            rasterFadeDuration(if (fadeIn) 300f else 0f)
        )
    }

    companion object {
        private const val TAG = "TileOverlay"
    }
}
