/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.maps.model;

import android.graphics.Color;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines options for a polyline.
 * TODO
 */
@PublicApi
public class PolylineOptions extends AutoSafeParcelable {
    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(value = 2, subClass = LatLng.class)
    private List<LatLng> points = new ArrayList<LatLng>();
    @SafeParceled(3)
    private float width = 10;
    @SafeParceled(4)
    private int color = Color.BLACK;
    @SafeParceled(5)
    private float zIndex = 0;
    @SafeParceled(6)
    private boolean visible = true;
    @SafeParceled(7)
    private boolean geodesic = false;

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
