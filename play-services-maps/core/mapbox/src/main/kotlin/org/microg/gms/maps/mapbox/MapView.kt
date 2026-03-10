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

    private val options: GoogleMapOptions = options ?: GoogleMapOptions()
    private var map: IGoogleMapDelegate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: ${options?.camera?.target}")
        map = if (options.liteMode) {
            LiteGoogleMapImpl(context, options)
        } else {
            GoogleMapImpl(context, options)
        }.apply {
            this.onCreate(savedInstanceState)
        }
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
    override fun getView(): IObjectWrapper = ObjectWrapper.wrap(
        map?.let {
            when (it) {
                is GoogleMapImpl -> it.view
                is LiteGoogleMapImpl -> it.view
                else -> null
            }
        }
    )

    override fun getMapAsync(callback: IOnMapReadyCallback) = map?.let {
        when (it) {
            is GoogleMapImpl -> it.getMapAsync(callback)
            is LiteGoogleMapImpl -> it.getMapAsync(callback)
            else -> null
        }
    } ?: Unit

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
