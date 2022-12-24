package com.google.android.gms.maps.model.internal;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.StyleSpan;

interface IPolylineDelegate {
	void remove() = 0;
	String getId() = 1;
	void setPoints(in List<LatLng> points) = 2;
	List<LatLng> getPoints() = 3;
	void setWidth(float width) = 4;
	float getWidth() = 5;
	void setColor(int color) = 6;
	int getColor() = 7;
	void setZIndex(float zIndex) = 8;
	float getZIndex() = 9;
	void setVisible(boolean visible) = 10;
	boolean isVisible() = 11;
	void setGeodesic(boolean geod) = 12;
	boolean isGeodesic() = 13;
	boolean equalsRemote(IPolylineDelegate other) = 14;
	int hashCodeRemote() = 15;
	void setClickable(boolean clickable) = 16;
    boolean isClickable() = 17;
    //void setStartCap(Cap startCap) = 18;
	//Cap getStartCap() = 19;
	//void setEndCap(Cap endCap) = 20;
	//Cap getEndCap() = 21;
	void setJointType(int jointType) = 22;
	int getJointType() = 23;
	void setPattern(in List<PatternItem> pattern) = 24;
	List<PatternItem> getPattern() = 25;
	void setTag(IObjectWrapper tag) = 26;
	IObjectWrapper getTag() = 27;
	//void setSpans(in List<StyleSpan> spans) = 28;
	//List<StyleSpan> getSpans() = 29
}
