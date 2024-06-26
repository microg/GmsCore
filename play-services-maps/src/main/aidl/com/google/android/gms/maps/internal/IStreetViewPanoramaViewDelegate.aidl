/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.maps.internal;

import android.os.Bundle;

import com.google.android.gms.maps.internal.IOnStreetViewPanoramaReadyCallback;
import com.google.android.gms.dynamic.IObjectWrapper;

interface IStreetViewPanoramaViewDelegate {
    IObjectWrapper init();
    void onCreate(in Bundle savedInstanceState);
    void onResume();
    void onPause();
    void onDestroy();
    void onLowMemory();
    void onSaveInstanceState(inout Bundle outState);
    IObjectWrapper getView();
    void getStreetViewAsync(IOnStreetViewPanoramaReadyCallback callback);
    void onStart();
    void onStop();
}
