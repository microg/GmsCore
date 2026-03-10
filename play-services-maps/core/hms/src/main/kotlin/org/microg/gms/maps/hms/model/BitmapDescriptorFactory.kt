/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms.model

import android.content.res.Resources
import android.graphics.*
import android.os.Parcel
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.maps.model.internal.IBitmapDescriptorFactoryDelegate
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.BitmapDescriptorFactory


object BitmapDescriptorFactoryImpl : IBitmapDescriptorFactoryDelegate.Stub() {
    private val TAG = "GmsMapBitmap"
    private var resources: Resources? = null

    fun initialize(resources: Resources?) {
        BitmapDescriptorFactoryImpl.resources = resources ?: BitmapDescriptorFactoryImpl.resources
    }

    override fun fromResource(resourceId: Int): IObjectWrapper? {
        return BitmapFactory.decodeResource(resources, resourceId)?.let {
            ObjectWrapper.wrap(BitmapDescriptorFactory.fromBitmap(it))
        }
    }

    override fun fromAsset(assetName: String): IObjectWrapper? {
        return resources?.assets?.open(assetName)?.let {
            BitmapFactory.decodeStream(it)
                ?.let { ObjectWrapper.wrap(BitmapDescriptorFactory.fromBitmap(it)) }
        }
    }

    override fun fromFile(fileName: String): IObjectWrapper? {
        return BitmapFactory.decodeFile(fileName)
            ?.let { ObjectWrapper.wrap(BitmapDescriptorFactory.fromBitmap(it)) }
    }

    override fun defaultMarker(): IObjectWrapper? {
        return ObjectWrapper.wrap(BitmapDescriptorFactory.defaultMarker())
    }

    override fun defaultMarkerWithHue(hue: Float): IObjectWrapper? {
        return ObjectWrapper.wrap(BitmapDescriptorFactory.defaultMarker(hue))
    }

    override fun fromBitmap(bitmap: Bitmap): IObjectWrapper? {
        return ObjectWrapper.wrap(BitmapDescriptorFactory.fromBitmap(bitmap))
    }

    override fun fromPath(absolutePath: String): IObjectWrapper? {
        return BitmapFactory.decodeFile(absolutePath)
            ?.let { ObjectWrapper.wrap(BitmapDescriptorFactory.fromBitmap(it)) }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
            if (super.onTransact(code, data, reply, flags)) {
                true
            } else {
                Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
            }
}
