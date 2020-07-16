package com.google.android.gms.maps.model.internal;

import com.google.android.gms.maps.model.LatLng;

interface IPolylineDelegate {
	void remove();
	String getId();
	void setPoints(in List<LatLng> points);
	List<LatLng> getPoints();
	void setWidth(float width);
	float getWidth();
	void setColor(int color);
	int getColor();
	void setZIndex(float zIndex);
	float getZIndex();
	void setVisible(boolean visible);
	boolean isVisible();
	void setGeodesic(boolean geod);
	boolean isGeodesic();
	boolean equalsRemote(IPolylineDelegate other);
	int hashCodeRemote();
}
