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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcel
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.maps.model.internal.IBitmapDescriptorFactoryDelegate
import com.mapbox.mapboxsdk.maps.MapboxMap

object BitmapDescriptorFactoryImpl : IBitmapDescriptorFactoryDelegate.Stub() {
    private val TAG = "GmsMapBitmap"
    private var resources: Resources? = null
    private val maps = hashSetOf<MapboxMap>()
    private val bitmaps = hashMapOf<String, Bitmap>()

    fun initialize(resources: Resources) {
        BitmapDescriptorFactoryImpl.resources = resources
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
        val bitmap = synchronized(bitmaps) {
            if (bitmaps.contains(id)) return
            val bitmap = bitmapCreator() ?: return
            bitmaps[id] = bitmap
            bitmap
        }
        for (map in maps) {
            map.getStyle { it.addImage(id, bitmap) }
        }
    }

    override fun fromResource(resourceId: Int): IObjectWrapper? {
        val id = "resource-$resourceId"
        registerBitmap(id) { BitmapFactory.decodeResource(resources, resourceId) }
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
        Log.d(TAG, "unimplemented Method: defaultMarker")
        val id = "marker"
        return ObjectWrapper.wrap(BitmapDescriptorImpl("marker", bitmapSize(id)))
    }

    override fun defaultMarkerWithHue(hue: Float): IObjectWrapper? {
        val id = "marker"
        Log.d(TAG, "unimplemented Method: defaultMarkerWithHue")
        return ObjectWrapper.wrap(ColorBitmapDescriptorImpl("marker", bitmapSize(id), hue))
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

    override fun onTransact(code: Int, data: Parcel?, reply: Parcel?, flags: Int): Boolean =
            if (super.onTransact(code, data, reply, flags)) {
                true
            } else {
                Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
            }
}
