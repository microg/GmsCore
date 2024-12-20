/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.maps.internal;

import android.os.Bundle;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.internal.IOnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.internal.IStreetViewPanoramaDelegate;

interface IStreetViewPanoramaViewDelegate {
    IStreetViewPanoramaDelegate getStreetViewPanorama() = 0;
    void onCreate(in Bundle savedInstanceState) = 1;
    void onResume() = 2;
    void onPause() = 3;
    void onDestroy() = 4;
    void onLowMemory() = 5;
    void onSaveInstanceState(inout Bundle outState) = 6;
    IObjectWrapper getView() = 7;
    void getStreetViewPanoramaAsync(IOnStreetViewPanoramaReadyCallback callback) = 8;
    void onStart() = 9;
    void onStop() = 10;
}
