package com.google.android.gms.maps.model.internal;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

interface IGroundOverlayDelegate {
    void remove();
    String getId();
    void setPosition(in LatLng pos);
    LatLng getPosition();
    void setDimension(float dimension);
    void setDimensions(float width, float height);
    float getWidth();
    float getHeight();
    void setPositionFromBounds(in LatLngBounds bounds);
    LatLngBounds getBounds();
    void setBearing(float bearing);
    float getBearing();
    void setZIndex(float zIndex);
    float getZIndex();
    void setVisible(boolean visible);
    boolean isVisible();
    void setTransparency(float transparency);
    float getTransparency();
	boolean equalsRemote(IGroundOverlayDelegate other);
	int hashCodeRemote();
	void todo(IObjectWrapper obj);
}
