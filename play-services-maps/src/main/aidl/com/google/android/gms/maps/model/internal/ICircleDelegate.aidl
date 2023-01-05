package com.google.android.gms.maps.model.internal;

import com.google.android.gms.maps.model.LatLng;

interface ICircleDelegate {
    void remove();
    String getId();
    void setCenter(in LatLng center);
    LatLng getCenter();
    void setRadius(double radius);
    double getRadius();
	void setStrokeWidth(float width);
	float getStrokeWidth();
	void setStrokeColor(int color);
	int getStrokeColor();
	void setFillColor(int color);
    int getFillColor();
    void setZIndex(float zIndex);
    float getZIndex();
    void setVisible(boolean visible);
    boolean isVisible();
	boolean equalsRemote(ICircleDelegate other);
	int hashCodeRemote();
}
