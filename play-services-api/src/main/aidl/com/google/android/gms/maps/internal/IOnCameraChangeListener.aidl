package com.google.android.gms.maps.internal;

import com.google.android.gms.maps.model.CameraPosition;

interface IOnCameraChangeListener {
    void onCameraChange(in CameraPosition update);
}
