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
import java.util.Collections;
import java.util.List;

/**
 * Defines options for a Circle.
 */
@PublicApi
public class CircleOptions extends AutoSafeParcelable {
    @SafeParceled(1)
    private int versionCode;
    @SafeParceled(2)
    private LatLng center;
    @SafeParceled(3)
    private double radius = 0;
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
    private boolean clickable = false;
    @SafeParceled(10)
    private List<PatternItem> strokePattern = null;

    /**
     * Creates circle options.
     */
    public CircleOptions() {
    }

    /**
     * Sets the center using a {@link LatLng}.
     * <p/>
     * The center must not be {@code null}.
     * <p/>
     * This method is mandatory because there is no default center.
     *
     * @param center The geographic center as a {@link LatLng}.
     * @return this {@link CircleOptions} object
     */
    public CircleOptions center(LatLng center) {
        this.center = center;
        return this;
    }

    /**
     * Sets the fill color.
     * <p/>
     * The fill color is the color inside the circle, in the integer format specified by
     * {@link Color}. If TRANSPARENT is used then no fill is drawn.
     * <p/>
     * By default the fill color is transparent ({@code 0x00000000}).
     *
     * @param color color in the {@link Color} format
     * @return this {@link CircleOptions} object
     */
    public CircleOptions fillColor(int color) {
        this.fillColor = color;
        return this;
    }

    /**
     * Returns the center as a {@link LatLng}.
     *
     * @return The geographic center as a {@link LatLng}.
     */
    public LatLng getCenter() {
        return center;
    }

    /**
     * Returns the fill color.
     *
     * @return The color in the {@link Color} format.
     */
    public int getFillColor() {
        return fillColor;
    }

    /**
     * Returns the circle's radius, in meters.
     *
     * @return The radius in meters.
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Returns the stroke color.
     *
     * @return The color in the {@link Color} format.
     */
    public int getStrokeColor() {
        return strokeColor;
    }

    /**
     * Returns the stroke width.
     *
     * @return The width in screen pixels.
     */
    public float getStrokeWidth() {
        return strokeWidth;
    }

    /**
     * Returns the zIndex.
     *
     * @return The zIndex value.
     */
    public float getZIndex() {
        return zIndex;
    }

    /**
     * Checks whether the circle is visible.
     *
     * @return {code true} if the circle is visible; {@code false} if it is invisible.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Gets the clickability setting for the circle.
     *
     * @return {@code true} if the circle is clickable; {@code false} if it is not.
     */
    public boolean isClickable() {
        return clickable;
    }

    /**
     * Sets the radius in meters.
     * <p/>
     * The radius must be zero or greater. The default radius is zero.
     *
     * @param radius radius in meters
     * @return this {@link CircleOptions} object
     */
    public CircleOptions radius(double radius) {
        this.radius = radius;
        return this;
    }

    /**
     * Sets the stroke color.
     * <p/>
     * The stroke color is the color of this circle's outline, in the integer format specified by
     * {@link Color}. If TRANSPARENT is used then no outline is drawn.
     * <p/>
     * By default the stroke color is black ({@code 0xff000000}).
     *
     * @param color color in the {@link Color} format
     * @return this {@link CircleOptions} object
     */
    public CircleOptions strokeColor(int color) {
        this.strokeColor = color;
        return this;
    }

    /**
     * Sets the stroke width.
     * <p/>
     * The stroke width is the width (in screen pixels) of the circle's outline. It must be zero or
     * greater. If it is zero then no outline is drawn.
     * <p/>
     * The default width is 10 pixels.
     *
     * @param width width in screen pixels
     * @return this {@link CircleOptions} object
     */
    public CircleOptions strokeWidth(float width) {
        this.strokeWidth = width;
        return this;
    }

    /**
     * Sets the visibility.
     * <p/>
     * If this circle is not visible then it is not drawn, but all other state is preserved.
     *
     * @param visible {@code false} to make this circle invisible
     * @return this {@link CircleOptions} object
     */
    public CircleOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Sets the zIndex.
     * <p/>
     * Overlays (such as circles) with higher zIndices are drawn above those with lower indices.
     * <p/>
     * By default the zIndex is {@code 0.0}.
     *
     * @param zIndex zIndex value
     * @return this {@link CircleOptions} object
     */
    public CircleOptions zIndex(float zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    /**
     * Specifies whether this circle is clickable. The default setting is {@code false}.
     *
     * @param clickable
     * @return this {@code CircleOptions} object with a new clickability setting.
     */
    public CircleOptions clickable(boolean clickable) {
        this.clickable = clickable;
        return this;
    }

    /**
     * Specifies a stroke pattern for the circle's outline. The default stroke pattern is solid, represented by {@code null}.
     *
     * @return this {@link CircleOptions} object with a new stroke pattern set.
     */
    public CircleOptions strokePattern(List<PatternItem> pattern) {
        this.strokePattern = pattern;
        return this;
    }

    /**
     * Gets the stroke pattern set in this {@link CircleOptions} object for the circle's outline.
     *
     * @return the stroke pattern of the circle's outline.
     */
    public List<PatternItem> getStrokePattern() {
        return strokePattern;
    }

    public static Creator<CircleOptions> CREATOR = new AutoCreator<CircleOptions>(CircleOptions.class);
}
