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

import android.content.res.Resources
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.maps.model.internal.IBitmapDescriptorFactoryDelegate
import com.mapbox.mapboxsdk.maps.MapboxMap
import org.microg.gms.maps.mapbox.R
import org.microg.gms.maps.mapbox.runOnMainLooper


object BitmapDescriptorFactoryImpl : IBitmapDescriptorFactoryDelegate.Stub() {
    private val TAG = "GmsMapBitmap"
    private var resources: Resources? = null
    private var mapResources: Resources? = null
    private val maps = hashSetOf<MapboxMap>()
    private val bitmaps = hashMapOf<String, Bitmap>()

    fun initialize(mapResources: Resources?, resources: Resources?) {
        BitmapDescriptorFactoryImpl.mapResources = mapResources ?: resources
        BitmapDescriptorFactoryImpl.resources = resources ?: mapResources
    }

    fun registerMap(map: MapboxMap) {
        Log.d(TAG, "registerMap")
        map.getStyle {
            synchronized(bitmaps) {
                it.addImages(bitmaps)
            }
        }
        maps.add(map)
    }

    fun unregisterMap(map: MapboxMap?) {
        maps.remove(map)
        // TODO: cleanup bitmaps?
    }

    fun bitmapSize(id: String): FloatArray =
            bitmaps[id]?.let { floatArrayOf(it.width.toFloat(), it.height.toFloat()) }
                    ?: floatArrayOf(0f, 0f)

    private fun registerBitmap(id: String, bitmapCreator: () -> Bitmap?) {
        val bitmap: Bitmap = synchronized(bitmaps) {
            if (bitmaps.contains(id)) return
            val bitmap = bitmapCreator()
            if (bitmap == null) {
                Log.w(TAG, "Failed to register bitmap $id, creator returned null")
                return
            }
            bitmaps[id] = bitmap
            bitmap
        }
        for (map in maps) {
            map.getStyle {
                runOnMainLooper {
                    it.addImage(id, bitmap)
                }
            }
        }
    }

    override fun fromResource(resourceId: Int): IObjectWrapper? {
        val id = "resource-$resourceId"
        registerBitmap(id) {
            val bitmap = BitmapFactory.decodeResource(resources, resourceId)
            if (bitmap == null) {
                try {
                    Log.d(TAG, "Resource $resourceId not found in $resources (${resources?.getResourceName(resourceId)})")
                } catch (e: Resources.NotFoundException) {
                    Log.d(TAG, "Resource $resourceId not found in $resources")
                }
            }
            bitmap
        }
        return ObjectWrapper.wrap(BitmapDescriptorImpl(id, bitmapSize(id)))
    }

    override fun fromAsset(assetName: String): IObjectWrapper? {
        val id = "asset-$assetName"
        registerBitmap(id) { resources?.assets?.open(assetName)?.let { BitmapFactory.decodeStream(it) } }
        return ObjectWrapper.wrap(BitmapDescriptorImpl(id, bitmapSize(id)))
    }

    override fun fromFile(fileName: String): IObjectWrapper? {
        val id = "file-$fileName"
        registerBitmap(id) { BitmapFactory.decodeFile(fileName) }
        return ObjectWrapper.wrap(BitmapDescriptorImpl(id, bitmapSize(id)))
    }

    override fun defaultMarker(): IObjectWrapper? {
        val id = "marker"
        registerBitmap(id) { BitmapFactory.decodeResource(mapResources, R.drawable.maps_default_marker) }
        return ObjectWrapper.wrap(BitmapDescriptorImpl(id, bitmapSize(id)))
    }

    private fun adjustHue(cm: ColorMatrix, value: Float) {
        var value = value
        value = cleanValue(value, 180f) / 180f * Math.PI.toFloat()
        if (value == 0f) {
            return
        }
        val cosVal = Math.cos(value.toDouble()).toFloat()
        val sinVal = Math.sin(value.toDouble()).toFloat()
        val lumR = 0.213f
        val lumG = 0.715f
        val lumB = 0.072f
        val mat = floatArrayOf(lumR + cosVal * (1 - lumR) + sinVal * -lumR, lumG + cosVal * -lumG + sinVal * -lumG, lumB + cosVal * -lumB + sinVal * (1 - lumB), 0f, 0f, lumR + cosVal * -lumR + sinVal * 0.143f, lumG + cosVal * (1 - lumG) + sinVal * 0.140f, lumB + cosVal * -lumB + sinVal * -0.283f, 0f, 0f, lumR + cosVal * -lumR + sinVal * -(1 - lumR), lumG + cosVal * -lumG + sinVal * lumG, lumB + cosVal * (1 - lumB) + sinVal * lumB, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f)
        cm.postConcat(ColorMatrix(mat))
    }

    private fun cleanValue(p_val: Float, p_limit: Float): Float {
        return Math.min(p_limit, Math.max(-p_limit, p_val))
    }

    override fun defaultMarkerWithHue(hue: Float): IObjectWrapper? {
        val id = "marker-${hue.toInt()}"
        registerBitmap(id) {
            val bitmap = BitmapFactory.decodeResource(mapResources, R.drawable.maps_default_marker).copy(Bitmap.Config.ARGB_8888, true)
            val paint = Paint()
            val matrix = ColorMatrix()
            val huex = hue % 360f
            adjustHue(matrix, if (huex > 180f) huex - 360f else huex)
            paint.setColorFilter(ColorMatrixColorFilter(matrix))

            val canvas = Canvas(bitmap)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            bitmap
        }
        return ObjectWrapper.wrap(ColorBitmapDescriptorImpl(id, bitmapSize(id), hue))
    }

    override fun fromBitmap(bitmap: Bitmap): IObjectWrapper? {
        val id = "bitmap-${bitmap.hashCode()}"
        registerBitmap(id) { bitmap }
        return ObjectWrapper.wrap(BitmapDescriptorImpl(id, bitmapSize(id)))
    }

    override fun fromPath(absolutePath: String): IObjectWrapper? {
        val id = "path-$absolutePath"
        registerBitmap(id) { BitmapFactory.decodeFile(absolutePath) }
        return ObjectWrapper.wrap(BitmapDescriptorImpl(id, bitmapSize(id)))
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
            if (super.onTransact(code, data, reply, flags)) {
                true
            } else {
                Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
            }
}
