/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps.model;

import android.graphics.Color;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines options for a polygon.
 * <p>
 * For more information, read the <a href="https://developers.google.com/maps/documentation/android-sdk/shapes">Shapes</a> developer guide.
 */
@PublicApi
public class PolygonOptions extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 1;
    @Field(value = 2, subClass = LatLng.class)
    private List<LatLng> points = new ArrayList<LatLng>();
    @Field(value = 3, subClass = LatLng.class, useValueParcel = true)
    private List<List<LatLng>> holes = new ArrayList<List<LatLng>>();
    @Field(4)
    private float strokeWidth = 10;
    @Field(5)
    private int strokeColor = Color.BLACK;
    @Field(6)
    private int fillColor = Color.BLACK;
    @Field(7)
    private float zIndex = 0;
    @Field(8)
    private boolean visible = true;
    @Field(9)
    private boolean geodesic = false;
    @Field(10)
    private boolean clickable = false;
    @Field(11)
    private int strokeJointType = JointType.DEFAULT;
    @Field(value = 12, subClass = PatternItem.class)
    private List<PatternItem> strokePattern = null;

    /**
     * Creates polygon options.
     */
    public PolygonOptions() {
    }

    /**
     * Adds vertices to the outline of the polygon being built.
     *
     * @param points an array of {@link LatLng}s that are added to the outline of the polygon. Must not be {@code null}.
     * @return this {@link PolygonOptions} object with the given points added to the outline.
     */
    public PolygonOptions add(LatLng... points) {
        for (LatLng point : points) {
            this.points.add(point);
        }
        return this;
    }

    /**
     * Adds a vertex to the outline of the polygon being built.
     *
     * @param point a {@link LatLng} that is added to the outline of the polygon. Must not be {@code null}.
     * @return this {@link PolygonOptions} object with the given point added to the outline.
     */
    public PolygonOptions add(LatLng point) {
        points.add(point);
        return this;
    }

    /**
     * Adds vertices to the outline of the polygon being built.
     *
     * @param points a list of {@link LatLng}s that are added to the outline of the polygon. Must not be {@code null}.
     * @return this {@link PolygonOptions} object with the given points added to the outline.
     */
    public PolygonOptions add(Iterable<LatLng> points) {
        for (LatLng point : points) {
            this.points.add(point);
        }
        return this;
    }

    /**
     * Adds a hole to the polygon being built.
     *
     * @param points an iterable of {@link LatLng}s that represents a hole. Must not be {@code null}.
     * @return this {@link PolygonOptions} object with the given hole added.
     */
    public PolygonOptions addHole(Iterable<LatLng> points) {
        ArrayList<LatLng> hole = new ArrayList<LatLng>();
        for (LatLng point : points) {
            hole.add(point);
        }
        holes.add(hole);
        return this;
    }

    /**
     * Specifies whether this polygon is clickable. The default setting is {@code false}
     *
     * @return this {@link PolygonOptions} object with a new clickability setting.
     */
    public PolygonOptions clickable(boolean clickable) {
        this.clickable = clickable;
        return this;
    }

    /**
     * Specifies the polygon's fill color, as 32-bit ARGB. The default color is black ({@code 0xff000000}).
     *
     * @return this {@link PolygonOptions} object with a new fill color set.
     */
    public PolygonOptions fillColor(int color) {
        this.fillColor = color;
        return this;
    }

    /**
     * Specifies whether to draw each segment of this polygon as a geodesic. The default setting is {@code false}
     *
     * @return this {@link PolygonOptions} object with a new geodesic setting.
     */
    public PolygonOptions geodesic(boolean geodesic) {
        this.geodesic = geodesic;
        return this;
    }

    /**
     * Gets the fill color set for this {@link PolygonOptions} object.
     *
     * @return the fill color of the polygon in screen pixels.
     */
    public int getFillColor() {
        return fillColor;
    }

    /**
     * Gets the holes set for this {@link PolygonOptions} object.
     *
     * @return the list of {@code List<LatLng>}s specifying the holes of the polygon.
     */
    public List<List<LatLng>> getHoles() {
        return holes;
    }

    /**
     * Gets the outline set for this {@link PolygonOptions} object.
     *
     * @return the list of {@link LatLng}s specifying the vertices of the outline of the polygon.
     */
    public List<LatLng> getPoints() {
        return points;
    }

    /**
     * Gets the stroke color set for this {@link PolygonOptions} object.
     *
     * @return the stroke color of the polygon in screen pixels.
     */
    public int getStrokeColor() {
        return strokeColor;
    }

    /**
     * Gets the stroke joint type set in this {@link PolygonOptions} object for all vertices of the polygon's outline. See {@link JointType} for possible values.
     *
     * @return the stroke joint type of the polygon's outline.
     */
    public int getStrokeJointType() {
        return strokeJointType;
    }

    /**
     * Gets the stroke pattern set in this {@link PolygonOptions} object for the polygon's outline.
     *
     * @return the stroke pattern of the polygon's outline.
     */
    public List<PatternItem> getStrokePattern() {
        return strokePattern;
    }

    /**
     * Gets the stroke width set for this {@link PolygonOptions} object.
     *
     * @return the stroke width of the polygon in screen pixels.
     */
    public float getStrokeWidth() {
        return strokeWidth;
    }

    /**
     * Gets the zIndex set for this {@link PolygonOptions} object.
     *
     * @return the zIndex of the polygon.
     */
    public float getZIndex() {
        return zIndex;
    }

    /**
     * Gets the clickability setting for this {@link PolygonOptions} object.
     *
     * @return {@code true} if the polygon is clickable; {@code false} if it is not.
     */
    public boolean isClickable() {
        return clickable;
    }

    /**
     * Gets the geodesic setting for this {@link PolygonOptions} object.
     *
     * @return {@code true} if the polygon segments should be geodesics; {@code false} if they should not be.
     */
    public boolean isGeodesic() {
        return geodesic;
    }

    /**
     * Gets the visibility setting for this {@link PolygonOptions} object.
     *
     * @return {@code true} if the polygon is to be visible; {@code false} if it is not.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Specifies the polygon's stroke color, as 32-bit ARGB. The default color is black ({@code 0xff000000}).
     *
     * @return this {@link PolygonOptions} object with a new stroke color set.
     */
    public PolygonOptions strokeColor(int color) {
        this.strokeColor = color;
        return this;
    }

    /**
     * Specifies the joint type for all vertices of the polygon's outline.
     * <p>
     * See {@link JointType} for allowed values. The default value {@link JointType#DEFAULT} will be used if joint type
     * is undefined or is not one of the allowed values.
     *
     * @return this {@link PolygonOptions} object with a new stroke joint type set.
     */
    public PolygonOptions strokeJointType(int jointType) {
        this.strokeJointType = jointType;
        return this;
    }

    /**
     * Specifies a stroke pattern for the polygon's outline. The default stroke pattern is solid, represented by {@code null}.
     *
     * @return this {@link PolygonOptions} object with a new stroke pattern set.
     */
    public PolygonOptions strokePattern(List<PatternItem> pattern) {
        this.strokePattern = pattern;
        return this;
    }

    /**
     * Specifies the polygon's stroke width, in display pixels. The default width is 10.
     *
     * @return this {@link PolygonOptions} object with a new stroke width set.
     */
    public PolygonOptions strokeWidth(float width) {
        this.strokeWidth = width;
        return this;
    }

    /**
     * Specifies the visibility for the polygon. The default visibility is true.
     *
     * @return this {@link PolygonOptions} object with a new visibility setting.
     */
    public PolygonOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Specifies the polygon's zIndex, i.e., the order in which it will be drawn. See the documentation at the top of this class for more information about zIndex.
     *
     * @return this {@link PolygonOptions} object with a new zIndex set.
     */
    public PolygonOptions zIndex(float zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    public static Creator<PolygonOptions> CREATOR = new AutoCreator<PolygonOptions>(PolygonOptions.class);
}
