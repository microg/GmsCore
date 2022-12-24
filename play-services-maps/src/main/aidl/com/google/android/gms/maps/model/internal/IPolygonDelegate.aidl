/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.maps.model.internal;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;

interface IPolygonDelegate {
    void remove() = 0;
    String getId() = 1;
	void setPoints(in List<LatLng> points) = 2;
	List<LatLng> getPoints() = 3;
	void setHoles(in List holes) = 4;
	List getHoles() = 5;
	void setStrokeWidth(float width) = 6;
	float getStrokeWidth() = 7;
	void setStrokeColor(int color) = 8;
	int getStrokeColor() = 9;
	void setFillColor(int color) = 10;
	int getFillColor() = 11;
	void setZIndex(float zIndex) = 12;
	float getZIndex() = 13;
	void setVisible(boolean visible) = 14;
	boolean isVisible() = 15;
	void setGeodesic(boolean geod) = 16;
	boolean isGeodesic() = 17;
	boolean equalsRemote(IPolygonDelegate other) = 18;
	int hashCodeRemote() = 19;
	void setClickable(boolean click) = 20;
	boolean isClickable() = 21;
	void setStrokeJointType(int type) = 22;
	int getStrokeJointType() = 23;
	void setStrokePattern(in List<PatternItem> items) = 24;
	List<PatternItem> getStrokePattern() = 25;
	void setTag(IObjectWrapper obj) = 26;
	IObjectWrapper getTag() = 27;
}
