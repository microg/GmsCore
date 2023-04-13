/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.maps.model;

import android.graphics.Color;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines options for a polyline.
 * TODO
 */
@PublicApi
public class PolylineOptions extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 1;
    @Field(value = 2, subClass = LatLng.class)
    private List<LatLng> points = new ArrayList<LatLng>();
    @Field(3)
    private float width = 10;
    @Field(4)
    private int color = Color.BLACK;
    @Field(5)
    private float zIndex = 0;
    @Field(6)
    private boolean visible = true;
    @Field(7)
    private boolean geodesic = false;
    @Field(8)
    private boolean clickable = false;
    @Field(9)
    private Cap startCap;
    @Field(10)
    private Cap endCap;
    @Field(11)
    private int jointType = JointType.DEFAULT;
    @Field(value = 12, subClass = PatternItem.class)
    private List<PatternItem> pattern = null;
    @Field(value = 13, subClass = StyleSpan.class)
    private List<StyleSpan> spans = null;

    public PolylineOptions() {
    }

    public PolylineOptions add(LatLng point) {
        points.add(point);
        return this;
    }

    public PolylineOptions add(LatLng... points) {
        for (LatLng point : points) {
            this.points.add(point);
        }
        return this;
    }

    public PolylineOptions addAll(Iterable<LatLng> points) {
        for (LatLng point : points) {
            this.points.add(point);
        }
        return this;
    }

    public PolylineOptions clickable(boolean clickable) {
        this.clickable = clickable;
        return this;
    }

    public PolylineOptions color(int color) {
        this.color = color;
        return this;
    }

    public PolylineOptions geodesic(boolean geodesic) {
        this.geodesic = geodesic;
        return this;
    }

    public int getColor() {
        return color;
    }

    public int getJointType() {
        return jointType;
    }

    public List<PatternItem> getPattern() {
        return pattern;
    }

    public List<LatLng> getPoints() {
        return points;
    }

    public float getWidth() {
        return width;
    }

    public float getZIndex() {
        return zIndex;
    }

    public boolean isGeodesic() {
        return geodesic;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isClickable() {
        return clickable;
    }

    public PolylineOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public PolylineOptions width(float width) {
        this.width = width;
        return this;
    }

    public PolylineOptions zIndex(float zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    public static Creator<PolylineOptions> CREATOR = new AutoCreator<PolylineOptions>(PolylineOptions.class);
}
