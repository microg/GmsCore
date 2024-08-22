/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.maps.internal;

import android.os.Bundle;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.StreetViewPanoramaOptions;
import com.google.android.gms.maps.internal.IOnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.internal.IStreetViewPanoramaDelegate;

interface IStreetViewPanoramaFragmentDelegate {
    IStreetViewPanoramaDelegate getStreetViewPanorama() = 0;
    void onInflate(IObjectWrapper activity, in StreetViewPanoramaOptions options, in Bundle savedInstanceState) = 1;
    void onCreate(in Bundle savedInstanceState) = 2;
    IObjectWrapper onCreateView(IObjectWrapper layoutInflater, IObjectWrapper container, in Bundle savedInstanceState) = 3;
    void onResume() = 4;
    void onPause() = 5;
    void onDestroyView() = 6;
    void onDestroy() = 7;
    void onLowMemory() = 8;
    void onSaveInstanceState(inout Bundle outState) = 9;
    boolean isReady() = 10;
    void getStreetViewPanoramaAsync(IOnStreetViewPanoramaReadyCallback callback) = 11;
    void onStart() = 12;
    void onStop() = 13;
}
