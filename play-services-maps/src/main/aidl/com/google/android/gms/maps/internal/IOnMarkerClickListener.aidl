package com.google.android.gms.maps.internal;

import com.google.android.gms.maps.model.internal.IMarkerDelegate;

interface IOnMarkerClickListener {
    boolean onMarkerClick(IMarkerDelegate marker);
}
