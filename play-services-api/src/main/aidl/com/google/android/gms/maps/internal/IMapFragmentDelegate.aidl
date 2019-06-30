package com.google.android.gms.maps.internal;

import android.os.Bundle;

import com.google.android.gms.maps.internal.IGoogleMapDelegate;
import com.google.android.gms.maps.internal.IOnMapReadyCallback;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.dynamic.IObjectWrapper;

interface IMapFragmentDelegate {
    IGoogleMapDelegate getMap();
    void onInflate(IObjectWrapper activity, in GoogleMapOptions options, in Bundle savedInstanceState);
    void onCreate(in Bundle savedInstanceState);
    IObjectWrapper onCreateView(IObjectWrapper layoutInflater, IObjectWrapper container, in Bundle savedInstanceState);
    void onResume();
    void onPause();
    void onDestroyView();
    void onDestroy();
    void onLowMemory();
    void onSaveInstanceState(inout Bundle outState);
    boolean isReady();
    void getMapAsync(IOnMapReadyCallback callback);
    void onEnterAmbient(in Bundle bundle);
    void onExitAmbient();
    void onStart();
    void onStop();
}
