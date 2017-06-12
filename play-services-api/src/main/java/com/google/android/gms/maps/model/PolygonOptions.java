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
 * Defines options for a polygon.
 * TODO: Docs
 */
@PublicApi
public class PolygonOptions extends AutoSafeParcelable {
    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(value = 2, subClass = LatLng.class)
    private List<LatLng> points = new ArrayList<LatLng>();
    @SafeParceled(value = 3, subClass = LatLng.class, useClassLoader = true)
    private List<List<LatLng>> holes = new ArrayList<List<LatLng>>();
    @SafeParceled(4)
    private float strokeWidth = 10;
    @SafeParceled(5)
    private int strokeColor = Color.BLACK;
    @SafeParceled(6)
    private int fillColor = Color.TRANSPARENT;
    @SafeParceled(7)
    private float zIndex = 0;
    @SafeParceled(8)
    private boolean visible = true;
    @SafeParceled(9)
    private boolean geodesic = false;

    /**
     * Creates polygon options.
     */
    public PolygonOptions() {
    }

    public PolygonOptions add(LatLng point) {
        points.add(point);
        return this;
    }

    public PolygonOptions add(LatLng... points) {
        for (LatLng point : points) {
            this.points.add(point);
        }
        return this;
    }

    public PolygonOptions add(Iterable<LatLng> points) {
        for (LatLng point : points) {
            this.points.add(point);
        }
        return this;
    }

    public PolygonOptions addHole(Iterable<LatLng> points) {
        ArrayList<LatLng> hole = new ArrayList<LatLng>();
        for (LatLng point : points) {
            hole.add(point);
        }
        holes.add(hole);
        return this;
    }

    public PolygonOptions fillColor(int color) {
        this.fillColor = color;
        return this;
    }

    public PolygonOptions geodesic(boolean geodesic) {
        this.geodesic = geodesic;
        return this;
    }

    public int getFillColor() {
        return fillColor;
    }

    public List<List<LatLng>> getHoles() {
        return holes;
    }

    public List<LatLng> getPoints() {
        return points;
    }

    public int getStrokeColor() {
        return strokeColor;
    }

    public float getStrokeWidth() {
        return strokeWidth;
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

    public PolygonOptions strokeColor(int color) {
        this.strokeColor = color;
        return this;
    }

    public PolygonOptions strokeWidth(float width) {
        this.strokeWidth = width;
        return this;
    }

    public PolygonOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public PolygonOptions zIndex(float zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    public static Creator<PolygonOptions> CREATOR = new AutoCreator<PolygonOptions>(PolygonOptions.class);
}
