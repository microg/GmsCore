/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.maps.internal;

import android.os.Bundle;

import com.google.android.gms.maps.internal.IOnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanoramaOptions;
import com.google.android.gms.dynamic.IObjectWrapper;

interface IStreetViewPanoramaFragmentDelegate {
    IObjectWrapper initStreetView();
    void onInflate(IObjectWrapper activity, in StreetViewPanoramaOptions options, in Bundle savedInstanceState);
    void onCreate(in Bundle savedInstanceState);
    IObjectWrapper onCreateView(IObjectWrapper layoutInflater, IObjectWrapper container, in Bundle savedInstanceState);
    void onResume();
    void onPause();
    void onDestroyView();
    void onDestroy();
    void onLowMemory();
    void onSaveInstanceState(inout Bundle outState);
    boolean isReady();
    void getStreetViewAsync(IOnStreetViewPanoramaReadyCallback callback);
    void onStart();
    void onStop();
}
