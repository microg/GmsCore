package com.google.android.gms.maps.model.internal;

import com.google.android.gms.maps.model.LatLng;

interface IPolygonDelegate {
    void remove();
    String getId();
	void setPoints(in List<LatLng> points);
	List<LatLng> getPoints();
	void setHoles(in List holes);
	List getHoles();
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
	void setGeodesic(boolean geod);
	boolean isGeodesic();
	boolean equalsRemote(IPolygonDelegate other);
	int hashCodeRemote();

}
