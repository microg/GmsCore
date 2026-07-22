package com.google.android.gms.maps.internal;

import com.google.android.gms.maps.model.internal.IMarkerDelegate;

interface IOnMarkerDragListener {
    void onMarkerDragStart(IMarkerDelegate marker) = 0;
    void onMarkerDrag(IMarkerDelegate marker) = 1;
    void onMarkerDragEnd(IMarkerDelegate marker) = 2;
}
