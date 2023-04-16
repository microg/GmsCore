package com.google.android.gms.maps.internal;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.VisibleRegion;

interface IProjectionDelegate {
    LatLng fromScreenLocation(IObjectWrapper obj);
    IObjectWrapper toScreenLocation(in LatLng latLng);
    VisibleRegion getVisibleRegion();
}
