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

package org.microg.gms.maps.mapbox.model

import android.graphics.Color
import android.graphics.PointF
import android.util.Log
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_TOP_LEFT
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.utils.ColorUtils

open class BitmapDescriptorImpl(private val id: String, internal val size: FloatArray) {
    open fun applyTo(options: SymbolOptions, anchor: FloatArray, dpiFactor: Float): SymbolOptions {
        return options.withIconImage(id).withIconAnchor(ICON_ANCHOR_TOP_LEFT).withIconOffset(arrayOf(-anchor[0] * size[0] / dpiFactor, -anchor[1] * size[1] / dpiFactor))
    }

    open fun applyTo(symbol: Symbol, anchor: FloatArray, dpiFactor: Float) {
        symbol.iconAnchor = ICON_ANCHOR_TOP_LEFT
        symbol.iconOffset = PointF(-anchor[0] * size[0] / dpiFactor, -anchor[1] * size[1] / dpiFactor)
        symbol.iconImage = id
    }

    open fun applyTo(symbolLayer: SymbolLayer, anchor: FloatArray, dpiFactor: Float) {
        symbolLayer.withProperties(
            PropertyFactory.iconAnchor(ICON_ANCHOR_TOP_LEFT),
            PropertyFactory.iconOffset(arrayOf(-anchor[0] * size[0] / dpiFactor, -anchor[1] * size[1] / dpiFactor)),
            PropertyFactory.iconImage(id)
        )
    }
}

class ColorBitmapDescriptorImpl(id: String, size: FloatArray, val hue: Float) : BitmapDescriptorImpl(id, size) {
    override fun applyTo(options: SymbolOptions, anchor: FloatArray, dpiFactor: Float): SymbolOptions = super.applyTo(options, anchor, dpiFactor).withIconColor(ColorUtils.colorToRgbaString(Color.HSVToColor(floatArrayOf(hue, 1.0f, 0.5f))))
    override fun applyTo(symbol: Symbol, anchor: FloatArray, dpiFactor: Float) {
        super.applyTo(symbol, anchor, dpiFactor)
        symbol.setIconColor(Color.HSVToColor(floatArrayOf(hue, 1.0f, 0.5f)))
    }
}