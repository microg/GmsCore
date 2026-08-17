/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.internal.IGoogleMapDelegate
import com.google.android.gms.maps.internal.IMapViewDelegate
import com.google.android.gms.maps.internal.IOnMapReadyCallback

class MapViewImpl(private val context: Context, options: GoogleMapOptions?) : IMapViewDelegate.Stub() {

    private var options: GoogleMapOptions = options ?: GoogleMapOptions()
    private var map: GoogleMapImpl? = null
    private var readyCallbackList: MutableList<IOnMapReadyCallback> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: $options")
        if (map == null) {
            map = GoogleMapImpl(context, options)
        }
        map!!.onCreate(savedInstanceState)
        readyCallbackList.forEach { map!!.getMapAsync(it) }
        readyCallbackList.clear()
    }

    override fun getMap(): IGoogleMapDelegate? = map
    override fun onEnterAmbient(bundle: Bundle?) = map?.onEnterAmbient(bundle) ?: Unit
    override fun onExitAmbient() = map?.onExitAmbient() ?: Unit
    override fun onStart() = map?.onStart() ?: Unit
    override fun onStop() = map?.onStop() ?: Unit
    override fun onResume() = map?.onResume() ?: Unit
    override fun onPause() = map?.onPause() ?: Unit
    override fun onDestroy() {
        map?.onDestroy()
        map = null
    }

    override fun onLowMemory() = map?.onLowMemory() ?: Unit
    override fun onSaveInstanceState(outState: Bundle) = map?.onSaveInstanceState(outState) ?: Unit
    override fun getView(): IObjectWrapper = ObjectWrapper.wrap(map?.view)
    override fun getMapAsync(callback: IOnMapReadyCallback) {
        Log.d(TAG, "getMapAsync: map: $map")
        if (map == null) {
            readyCallbackList.add(callback)
            return
        }
        map?.getMapAsync(callback)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
            if (super.onTransact(code, data, reply, flags)) {
                true
            } else {
                Log.d(TAG, "onTransact [unknown]: $code, $data, $flags"); false
            }

    companion object {
        private val TAG = "GmsMapView"
    }
}
