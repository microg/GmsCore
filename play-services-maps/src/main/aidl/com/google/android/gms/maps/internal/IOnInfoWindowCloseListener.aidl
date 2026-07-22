package com.google.android.gms.maps.internal;

import com.google.android.gms.maps.model.internal.IMarkerDelegate;

interface IOnInfoWindowCloseListener {
    void onInfoWindowClose(IMarkerDelegate marker);
}
