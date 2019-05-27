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

import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import java.util.*

internal class CameraBoundsWithSizeUpdate(val bounds: LatLngBounds, val width: Int, val height: Int, val padding: IntArray) : CameraUpdate {

    constructor(bounds: LatLngBounds, width: Int, height: Int, paddingLeft: Int, paddingTop: Int = paddingLeft, paddingRight: Int = paddingLeft, paddingBottom: Int = paddingTop) : this(bounds, width, height, intArrayOf(paddingLeft, paddingTop, paddingRight, paddingBottom)) {}

    override fun getCameraPosition(map: MapboxMap): CameraPosition? {
        val padding = this.padding.clone()
        val widthPad = (map.padding[0] + map.padding[2])/2
        val heightPad = (map.padding[1] + map.padding[3])/2
        padding[0] += widthPad
        padding[1] += heightPad
        padding[2] += widthPad
        padding[3] += heightPad
        return map.getCameraForLatLngBounds(bounds, padding)
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
        val TAG = "GmsCameraBounds"
    }
}