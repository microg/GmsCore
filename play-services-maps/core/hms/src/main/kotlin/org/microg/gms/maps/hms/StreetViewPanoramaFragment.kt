/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.maps.StreetViewPanoramaOptions
import com.google.android.gms.maps.internal.IOnStreetViewPanoramaReadyCallback
import com.google.android.gms.maps.internal.IStreetViewPanoramaDelegate
import com.google.android.gms.maps.internal.IStreetViewPanoramaFragmentDelegate

class StreetViewPanoramaFragmentImpl(private val activity: Activity) : IStreetViewPanoramaFragmentDelegate.Stub() {

    override fun getStreetViewPanorama(): IStreetViewPanoramaDelegate? {
        return null
    }

    override fun onInflate(activity: IObjectWrapper?, options: StreetViewPanoramaOptions?, savedInstanceState: Bundle?) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
    }

    override fun onCreateView(layoutInflater: IObjectWrapper?, container: IObjectWrapper?, savedInstanceState: Bundle?): IObjectWrapper {
        return ObjectWrapper.wrap(TextView(activity))
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onDestroyView() {
    }

    override fun onDestroy() {
    }

    override fun onLowMemory() {
    }

    override fun onSaveInstanceState(outState: Bundle?) {
    }

    override fun isReady(): Boolean = true

    override fun getStreetViewPanoramaAsync(callback: IOnStreetViewPanoramaReadyCallback?) {
    }

    override fun onStart() {
    }

    override fun onStop() {
    }

}