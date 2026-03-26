/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms

import android.app.Activity
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import android.view.ViewGroup
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.internal.IGoogleMapDelegate
import com.google.android.gms.maps.internal.IMapFragmentDelegate
import com.google.android.gms.maps.internal.IOnMapReadyCallback

class MapFragmentImpl(private val activity: Activity) : IMapFragmentDelegate.Stub() {

    private var map: GoogleMapImpl? = null
    private var options: GoogleMapOptions? = null
    private var readyCallbackList: MutableList<IOnMapReadyCallback> = mutableListOf()

    override fun onInflate(activity: IObjectWrapper, options: GoogleMapOptions, savedInstanceState: Bundle?) {
        Log.d(TAG, "onInflate: $options")
        this.options = options
        map?.options = options
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (options == null) {
            options = savedInstanceState?.getParcelable("MapOptions")
        }
        if (options == null) {
            options = GoogleMapOptions()
        }
        Log.d(TAG, "onCreate $this : $options ")
        if (map == null) {
            map = GoogleMapImpl(activity, options ?: GoogleMapOptions())
        }
    }

    override fun onCreateView(layoutInflater: IObjectWrapper, container: IObjectWrapper, savedInstanceState: Bundle?): IObjectWrapper {
        if (options == null) {
            options = savedInstanceState?.getParcelable("MapOptions")
        }
        Log.d(TAG, "onCreateView: ${options?.camera?.target}")
        if (map == null) {
            map = GoogleMapImpl(activity, options ?: GoogleMapOptions())
        }
        Log.d(TAG, "onCreateView $this : $options")
        map!!.onCreate(savedInstanceState)
        readyCallbackList.forEach { map!!.getMapAsync(it) }
        readyCallbackList.clear()
        val view = map!!.view
        val parent = view.parent as ViewGroup?
        parent?.removeView(view)
        return ObjectWrapper.wrap(view)
    }

    override fun getMap(): IGoogleMapDelegate? = map
    override fun onEnterAmbient(bundle: Bundle?) = map?.onEnterAmbient(bundle) ?: Unit
    override fun onExitAmbient() = map?.onExitAmbient() ?: Unit
    override fun onStart() = map?.onStart() ?: Unit
    override fun onStop() = map?.onStop() ?: Unit
    override fun onResume() = map?.onResume() ?: Unit
    override fun onPause() = map?.onPause() ?: Unit
    override fun onLowMemory() = map?.onLowMemory() ?: Unit
    override fun isReady(): Boolean = this.map != null
    override fun getMapAsync(callback: IOnMapReadyCallback) {
        Log.d(TAG, "getMapAsync: map: $map")
        if (map == null) {
            readyCallbackList.add(callback)
            return
        }
        map?.getMapAsync(callback)
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView: $this : $options")
        if (options?.useViewLifecycleInFragment == true) {
            map?.onDestroy()
            map = null
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: $this")
        map?.onDestroy()
        map = null
        options = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (options != null) {
            outState.putParcelable("MapOptions", options)
        }
        map?.onSaveInstanceState(outState)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (super.onTransact(code, data, reply, flags)) {
            return true
        } else {
            Log.d(TAG, "onTransact [unknown]: $code, $data, $flags")
            return false
        }
    }

    companion object {
        private val TAG = "GmsMapFragment"
    }
}
