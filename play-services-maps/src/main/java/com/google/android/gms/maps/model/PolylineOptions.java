/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps.model;

import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Defines options for a polyline.
 */
@PublicApi
public class PolylineOptions extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 1;
    @Field(value = 2, subClass = LatLng.class)
    @NonNull
    private List<LatLng> points = new ArrayList<>();
    @Field(3)
    private float width = 10.0f;
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
    @NonNull
    private Cap startCap = new ButtCap();
    @Field(10)
    @NonNull
    private Cap endCap = new ButtCap();
    @Field(11)
    private int jointType = JointType.DEFAULT;
    @Field(value = 12, subClass = PatternItem.class)
    @Nullable
    private List<PatternItem> pattern = null;
    @Field(value = 13, subClass = StyleSpan.class)
    @NonNull
    private List<StyleSpan> spans = new ArrayList<>();

    /**
     * Creates polyline options.
     */
    public PolylineOptions() {
    }

    /**
     * Adds vertices to the end of the polyline being built.
     *
     * @param points an array of {@link LatLng}s that are added to the end of the polyline. Must not be {@code null}.
     * @return this {@link PolylineOptions} object with the given points on the end.
     */
    public PolylineOptions add(LatLng... points) {
        this.points.addAll(Arrays.asList(points));
        return this;
    }

    /**
     * Adds a vertex to the end of the polyline being built.
     *
     * @param point a {@link LatLng} that is added to the end of the polyline. Must not be {@code null}.
     * @return this {@link PolylineOptions} object with the given point on the end.
     */
    public PolylineOptions add(LatLng point) {
        points.add(point);
        return this;
    }

    /**
     * Adds vertices to the end of the polyline being built.
     *
     * @param points a list of {@link LatLng}s that are added to the end of the polyline. Must not be {@code null}.
     * @return this {@link PolylineOptions} object with the given points on the end.
     */
    public PolylineOptions addAll(Iterable<LatLng> points) {
        for (LatLng point : points) {
            this.points.add(point);
        }
        return this;
    }

    /**
     * Adds new style spans to the polyline being built.
     *
     * @param spans the style spans that will be added to the polyline.
     * @return this {@link PolylineOptions} object with new style spans added.
     */
    public PolylineOptions addAllSpans(Iterable<StyleSpan> spans) {
        for (StyleSpan span : spans) {
            this.spans.add(span);
        }
        return this;
    }

    /**
     * Adds a new style span to the polyline being built.
     *
     * @param span the style span that will be added to the polyline.
     * @return this {@link PolylineOptions} object with new style span added.
     */
    public PolylineOptions addSpan(StyleSpan span) {
        this.spans.add(span);
        return this;
    }

    /**
     * Adds new style spans to the polyline being built.
     *
     * @param spans the style spans that will be added to the polyline.
     * @return this {@link PolylineOptions} object with new style spans added.
     */
    public PolylineOptions addSpan(StyleSpan... spans) {
        this.spans.addAll(Arrays.asList(spans));
        return this;
    }

    /**
     * Specifies whether this polyline is clickable. The default setting is {@code false}
     *
     * @return this {@link PolylineOptions} object with a new clickability setting.
     */
    public PolylineOptions clickable(boolean clickable) {
        this.clickable = clickable;
        return this;
    }

    /**
     * Sets the color of the polyline as a 32-bit ARGB color. The default color is black ({@code 0xff000000}).
     *
     * @return this {@link PolylineOptions} object with a new color set.
     */
    public PolylineOptions color(int color) {
        this.color = color;
        return this;
    }

    /**
     * Sets the cap at the end vertex of the polyline. The default end cap is {@link ButtCap}.
     *
     * @return this {@link PolylineOptions} object with a new end cap set.
     */
    public PolylineOptions endCap(@NonNull Cap endCap) {
        this.endCap = endCap;
        return this;
    }

    /**
     * Specifies whether to draw each segment of this polyline as a geodesic. The default setting is {@code false}
     *
     * @return this {@link PolylineOptions} object with a new geodesic setting.
     */
    public PolylineOptions geodesic(boolean geodesic) {
        this.geodesic = geodesic;
        return this;
    }

    /**
     * Gets the color set for this {@link PolylineOptions} object.
     *
     * @return the color of the polyline in ARGB format.
     */
    public int getColor() {
        return color;
    }

    /**
     * Gets the cap set for the end vertex in this {@link PolylineOptions} object.
     *
     * @return the end cap of the polyline.
     */
    public Cap getEndCap() {
        return endCap;
    }

    /**
     * Gets the joint type set in this {@link PolylineOptions} object for all vertices except the start and end vertices.
     * See {@link JointType} for possible values.
     *
     * @return the joint type of the polyline.
     */
    public int getJointType() {
        return jointType;
    }

    /**
     * Gets the stroke pattern set in this {@link PolylineOptions} object for the polyline.
     *
     * @return the stroke pattern of the polyline.
     */
    public List<PatternItem> getPattern() {
        return pattern;
    }

    /**
     * Gets the points set for this {@link PolylineOptions} object.
     *
     * @return the list of {@link LatLng}s specifying the vertices of the polyline.
     */
    public List<LatLng> getPoints() {
        return points;
    }

    @Hide
    public List<StyleSpan> getSpans() {
        return spans;
    }

    /**
     * Gets the cap set for the start vertex in this {@link PolylineOptions} object.
     *
     * @return the start cap of the polyline.
     */
    public Cap getStartCap() {
        return startCap;
    }

    /**
     * Gets the width set for this {@link PolylineOptions} object.
     *
     * @return the width of the polyline in screen pixels.
     */
    public float getWidth() {
        return width;
    }

    /**
     * Gets the zIndex set for this {@link PolylineOptions} object.
     *
     * @return the zIndex of the polyline.
     */
    public float getZIndex() {
        return zIndex;
    }

    /**
     * Gets the clickability setting for this {@link PolylineOptions} object.
     *
     * @return {@code true} if the polyline is clickable; {@code false} if it is not.
     */
    public boolean isClickable() {
        return clickable;
    }

    /**
     * Gets the geodesic setting for this {@link PolylineOptions} object.
     *
     * @return {@code true} if the polyline segments should be geodesics; {@code false} they should not be.
     */
    public boolean isGeodesic() {
        return geodesic;
    }

    /**
     * Gets the visibility setting for this {@link PolylineOptions} object.
     *
     * @return {@code true} if the polyline is visible; {@code false} if it is not.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets the joint type for all vertices of the polyline except the start and end vertices.
     * <p>
     * See {@link JointType} for allowed values. The default value {@link JointType#DEFAULT} will be used if joint type is undefined or is
     * not one of the allowed values.
     *
     * @return this {@link PolylineOptions} object with a new joint type set.
     */
    public PolylineOptions jointType(int jointType) {
        this.jointType = jointType;
        return this;
    }

    /**
     * Sets the stroke pattern for the polyline. The default stroke pattern is solid, represented by {@code null}.
     *
     * @return this {@link PolylineOptions} object with a new stroke pattern set.
     */
    public PolylineOptions pattern(@Nullable List<PatternItem> pattern) {
        this.pattern = pattern;
        return this;
    }

    /**
     * Sets the cap at the start vertex of the polyline. The default start cap is {@link ButtCap}.
     *
     * @return this {@link PolylineOptions} object with a new start cap set.
     */
    public PolylineOptions startCap(@NonNull Cap startCap) {
        this.startCap = startCap;
        return this;
    }

    /**
     * Specifies the visibility for the polyline. The default visibility is {@code true}.
     *
     * @return this {@link PolylineOptions} object with a new visibility setting.
     */
    public PolylineOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Sets the width of the polyline in screen pixels. The default is {@code 10}.
     *
     * @return this {@link PolylineOptions} object with a new width set.
     */
    public PolylineOptions width(float width) {
        this.width = width;
        return this;
    }

    /**
     * Specifies the polyline's zIndex, i.e., the order in which it will be drawn.
     *
     * @return this {@link PolylineOptions} object with a new zIndex set.
     */
    public PolylineOptions zIndex(float zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    public static Creator<PolylineOptions> CREATOR = new AutoCreator<PolylineOptions>(PolylineOptions.class);
}
