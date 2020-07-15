package com.google.android.gms.maps.internal;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

interface ICameraUpdateFactoryDelegate {
    IObjectWrapper zoomIn();
    IObjectWrapper zoomOut();
    IObjectWrapper scrollBy(float x, float y);
    IObjectWrapper zoomTo(float zoom);
    IObjectWrapper zoomBy(float zoomDelta);
    IObjectWrapper zoomByWithFocus(float zoomDelta, int x, int y);
    IObjectWrapper newCameraPosition(in CameraPosition cameraPosition);
    IObjectWrapper newLatLng(in LatLng latLng);
    IObjectWrapper newLatLngZoom(in LatLng latLng, float zoom);
    IObjectWrapper newLatLngBounds(in LatLngBounds bounds, int padding);
    IObjectWrapper newLatLngBoundsWithSize(in LatLngBounds bounds, int width, int height, int padding);
}
