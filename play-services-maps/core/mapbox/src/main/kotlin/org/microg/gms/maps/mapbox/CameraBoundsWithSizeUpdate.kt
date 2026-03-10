/*
 * Copyright (C) 2019 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.maps.mapbox

import android.util.Log
import com.google.android.gms.maps.internal.IGoogleMapDelegate
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import java.util.*

internal class CameraBoundsWithSizeUpdate(val bounds: LatLngBounds, val width: Int, val height: Int, val padding: IntArray) : LiteModeCameraUpdate, CameraUpdate {

    constructor(bounds: LatLngBounds, width: Int, height: Int, paddingLeft: Int, paddingTop: Int = paddingLeft, paddingRight: Int = paddingLeft, paddingBottom: Int = paddingTop) : this(bounds, width, height, intArrayOf(paddingLeft, paddingTop, paddingRight, paddingBottom)) {}

    override fun getLiteModeCameraPosition(map: IGoogleMapDelegate) = null

    override fun getLiteModeCameraBounds() = bounds

    override fun getCameraPosition(map: MapboxMap): CameraPosition? {
        val padding = this.padding.clone()

        val mapPadding = map.cameraPosition.padding
        mapPadding?.let {
            for (i in 0..3) {
                padding[i] += it[i].toInt()
            }
        }

        val widthPadding = ((map.width - width) / 2).toInt()
        val heightPadding = ((map.height - height) / 2).toInt()
        padding[0] += widthPadding
        padding[1] += heightPadding
        padding[2] += widthPadding
        padding[3] += heightPadding

        Log.d(TAG, "map ${map.width} ${map.height}, set $width $height -> ${Arrays.toString(padding)}")
        return map.getCameraForLatLngBounds(bounds, padding)?.let {
            CameraPosition.Builder(it)
                .apply {
                    mapPadding?.let {
                        padding(it)
                    }
                }.build()
        }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || other !is CameraBoundsWithSizeUpdate?) {
            return false
        }

        val that = other as CameraBoundsWithSizeUpdate? ?: return false

        if (bounds != that.bounds) {
            return false
        }

        if (Arrays.equals(padding, that.padding)) {
            return false
        }

        if (height != that.height || width != that.width) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = bounds.hashCode()
        result = 31 * result + Arrays.hashCode(padding)
        result = 31 * result + height.hashCode()
        result = 31 * result + width.hashCode()
        return result
    }

    override fun toString(): String {
        return ("CameraBoundsWithSizeUpdate{"
                + "bounds=" + bounds
                + ", padding=" + Arrays.toString(padding)
                + '}'.toString())
    }

    companion object {
        const val TAG = "GmsMapCameraBounds"
    }
}