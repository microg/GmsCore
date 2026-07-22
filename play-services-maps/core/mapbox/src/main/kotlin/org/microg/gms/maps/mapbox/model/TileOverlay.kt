/*
 * SPDX-FileCopyrightText: 2020 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.mapbox.model

import android.os.Parcel
import android.util.Log
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.internal.ITileOverlayDelegate
import org.microg.gms.maps.mapbox.GoogleMapImpl
import org.microg.gms.utils.warnOnTransactionIssues

class TileOverlayImpl(private val map: GoogleMapImpl, private val id: String, options: TileOverlayOptions) : ITileOverlayDelegate.Stub() {
    private var zIndex = options.zIndex
    private var visible = options.isVisible
    private var fadeIn = options.fadeIn
    private var transparency = options.transparency

    override fun remove() {
        Log.d(TAG, "Not yet implemented: remove")
    }

    override fun clearTileCache() {
        Log.d(TAG, "Not yet implemented: clearTileCache")
    }

    override fun getId(): String = id

    override fun setZIndex(zIndex: Float) {
        this.zIndex = zIndex
    }

    override fun getZIndex(): Float = zIndex

    override fun setVisible(visible: Boolean) {
        this.visible = visible
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
    }

    override fun getTransparency(): Float = transparency

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }

    companion object {
        private const val TAG = "TileOverlay"
    }
}
