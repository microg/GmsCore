package com.google.android.gms.maps.internal;

import com.google.android.gms.maps.model.internal.IPolylineDelegate;

interface IOnPolylineClickListener {
    void onPolylineClick(IPolylineDelegate polyline);
}