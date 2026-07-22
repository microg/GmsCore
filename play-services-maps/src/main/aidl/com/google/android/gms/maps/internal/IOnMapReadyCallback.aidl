package com.google.android.gms.maps.internal;

import com.google.android.gms.maps.internal.IGoogleMapDelegate;

interface IOnMapReadyCallback {
    void onMapReady(IGoogleMapDelegate map);
}
