/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms.model

import android.os.Parcel
import android.util.Log
import com.google.android.gms.maps.model.internal.ITileOverlayDelegate
import com.huawei.hms.maps.model.TileOverlay

class TileOverlayImpl(private val tileOverlay: TileOverlay) : ITileOverlayDelegate.Stub() {

    override fun clearTileCache() {
        tileOverlay.clearTileCache()
    }

    override fun equals(other: Any?): Boolean {
        return tileOverlay == other
    }

    override fun getFadeIn(): Boolean {
        return tileOverlay.fadeIn
    }

    override fun getId(): String {
        return tileOverlay.id
    }

    override fun getTransparency(): Float {
        return tileOverlay.transparency
    }

    override fun getZIndex(): Float {
        return tileOverlay.zIndex
    }

    override fun hashCode(): Int {
        return tileOverlay.hashCode()
    }

    override fun isVisible(): Boolean {
        return tileOverlay.isVisible
    }

    override fun remove() {
        return tileOverlay.remove()
    }

    override fun setFadeIn(fadeIn: Boolean) {
        tileOverlay.fadeIn = fadeIn
    }

    override fun setTransparency(transparency: Float) {
        tileOverlay.transparency = transparency
    }

    override fun setVisible(visible: Boolean) {
        tileOverlay.isVisible = visible
    }

    override fun setZIndex(zIndex: Float) {
        tileOverlay.zIndex = zIndex
    }

    override fun equalsRemote(other: ITileOverlayDelegate): Boolean = tileOverlay == (other as? TileOverlayImpl)?.tileOverlay
    override fun hashCodeRemote(): Int = tileOverlay.hashCode()

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        if (super.onTransact(code, data, reply, flags)) {
            Log.e(TAG, "onTransact [known]: $code, $data, $flags")
            true
        } else {
            Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
        }

    companion object {
        private val TAG = "GmsMapTileOverlay"
    }
}
