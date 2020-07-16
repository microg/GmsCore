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

import android.app.Activity
import android.os.Bundle
import android.os.Parcel
import android.util.Base64
import android.util.Log
import android.view.View
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

    override fun onInflate(activity: IObjectWrapper, options: GoogleMapOptions, savedInstanceState: Bundle?) {
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
        map = GoogleMapImpl(activity, options ?: GoogleMapOptions())
    }

    override fun onCreateView(layoutInflater: IObjectWrapper, container: IObjectWrapper, savedInstanceState: Bundle?): IObjectWrapper {
        if (options == null) {
            options = savedInstanceState?.getParcelable("MapOptions")
        }
        Log.d(TAG, "onCreateView: ${options?.camera?.target}")
        if (map == null) {
            map = GoogleMapImpl(activity, options ?: GoogleMapOptions())
        }
        map!!.onCreate(savedInstanceState)
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
    override fun getMapAsync(callback: IOnMapReadyCallback) = map?.getMapAsync(callback) ?: Unit

    override fun onDestroyView() {
        map?.onDestroy()
    }

    override fun onDestroy() {
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
