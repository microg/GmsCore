package com.google.android.gms.maps.model.internal;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

interface IGroundOverlayDelegate {
    void remove() = 0;
    String getId() = 1;
    void setPosition(in LatLng pos) = 2;
    LatLng getPosition() = 3;
    void setDimension(float dimension) = 4;
    void setDimensions(float width, float height) = 5;
    float getWidth() = 6;
    float getHeight() = 7;
    void setPositionFromBounds(in LatLngBounds bounds) = 8;
    LatLngBounds getBounds() = 9;
    void setBearing(float bearing) = 10;
    float getBearing() = 11;
    void setZIndex(float zIndex) = 12;
    float getZIndex() = 13;
    void setVisible(boolean visible) = 14;
    boolean isVisible() = 15;
    void setTransparency(float transparency) = 16;
    float getTransparency() = 17;
    boolean equalsRemote(IGroundOverlayDelegate other) = 18;
    int hashCodeRemote() = 19;
    void setImage(IObjectWrapper img) = 20;
    void setClickable(boolean clickable) = 21;
    boolean isClickable() = 22;
    void setTag(IObjectWrapper obj) = 23;
    IObjectWrapper getTag() = 24;
}
