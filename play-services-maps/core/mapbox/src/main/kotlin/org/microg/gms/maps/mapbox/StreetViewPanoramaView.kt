/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.mapbox

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.maps.internal.IOnStreetViewPanoramaReadyCallback
import com.google.android.gms.maps.internal.IStreetViewPanoramaDelegate
import com.google.android.gms.maps.internal.IStreetViewPanoramaViewDelegate

class StreetViewPanoramaViewImpl(private val context: Context) : IStreetViewPanoramaViewDelegate.Stub() {

    override fun getStreetViewPanorama(): IStreetViewPanoramaDelegate? {
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onDestroy() {
    }

    override fun onLowMemory() {
    }

    override fun onSaveInstanceState(outState: Bundle?) {
    }

    override fun getView(): IObjectWrapper? {
        return ObjectWrapper.wrap(TextView(context))
    }

    override fun getStreetViewPanoramaAsync(callback: IOnStreetViewPanoramaReadyCallback?) {
    }

    override fun onStart() {
    }

    override fun onStop() {
    }

}