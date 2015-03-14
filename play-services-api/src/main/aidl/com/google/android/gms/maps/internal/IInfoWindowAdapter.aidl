package com.google.android.gms.maps.internal;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.model.internal.IMarkerDelegate;

interface IInfoWindowAdapter {
    IObjectWrapper getInfoWindow(IMarkerDelegate marker);
    IObjectWrapper getInfoContents(IMarkerDelegate marker);
}
