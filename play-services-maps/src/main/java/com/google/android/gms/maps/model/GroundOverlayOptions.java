/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps.model;

import android.os.IBinder;

import com.google.android.gms.dynamic.ObjectWrapper;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Defines options for a ground overlay.
 */
@PublicApi
public class GroundOverlayOptions extends AutoSafeParcelable {
    /**
     * Flag for when no dimension is specified for the height.
     */
    public static final float NO_DIMENSION = -1;

    @Field(1)
    private int versionCode;
    @Field(2)
    private IBinder image;
    private BitmapDescriptor imageDescriptor;
    @Field(3)
    private LatLng location;
    @Field(4)
    private float width;
    @Field(5)
    private float height = NO_DIMENSION;
    @Field(6)
    private LatLngBounds bounds;
    @Field(7)
    private float bearing;
    @Field(8)
    private float zIndex;
    @Field(9)
    private boolean visible = true;
    @Field(10)
    private float transparency = 0.0f;
    @Field(11)
    private float anchorU = 0.5f;
    @Field(12)
    private float anchorV = 0.5f;
    @Field(13)
    private boolean clickable = false;

    /**
     * Creates a new set of ground overlay options.
     */
    public GroundOverlayOptions() {
    }

    /**
     * Specifies the anchor to be at a particular point in the image.
     * <p/>
     * The anchor specifies the point in the image that aligns with the ground overlay's location.
     * <p/>
     * The anchor point is specified in the continuous space [0.0, 1.0] x [0.0, 1.0], where (0, 0)
     * is the top-left corner of the image, and (1, 1) is the bottom-right corner.
     *
     * @param u u-coordinate of the anchor, as a ratio of the image width (in the range [0, 1])
     * @param v v-coordinate of the anchor, as a ratio of the image height (in the range [0, 1])
     * @return this {@link GroundOverlayOptions} object with a new anchor set.
     */
    public GroundOverlayOptions anchor(float u, float v) {
        this.anchorU = u;
        this.anchorV = v;
        return this;
    }

    /**
     * Specifies the bearing of the ground overlay in degrees clockwise from north. The rotation is
     * performed about the anchor point. If not specified, the default is 0 (i.e., up on the image
     * points north).
     * <p/>
     * If a ground overlay with position set using {@link #positionFromBounds(LatLngBounds)} is
     * rotated, its size will preserved and it will no longer be guaranteed to fit inside the
     * bounds.
     *
     * @param bearing the bearing in degrees clockwise from north. Values outside the range
     *                [0, 360) will be normalized.
     * @return this {@link GroundOverlayOptions} object with a new bearing set.
     */
    public GroundOverlayOptions bearing(float bearing) {
        this.bearing = bearing;
        return this;
    }

    /**
     * Specifies whether the ground overlay is clickable. The default clickability is {@code false}.
     *
     * @param clickable The new clickability setting.
     * @return this {@link GroundOverlayOptions} object with a new clickability setting.
     */
    public GroundOverlayOptions clickable(boolean clickable) {
        this.clickable = clickable;
        return this;
    }

    /**
     * Horizontal distance, normalized to [0, 1], of the anchor from the left edge.
     *
     * @return the u value of the anchor.
     */
    public float getAnchorU() {
        return anchorU;
    }

    /**
     * Vertical distance, normalized to [0, 1], of the anchor from the top edge.
     *
     * @return the v value of the anchor.
     */
    public float getAnchorV() {
        return anchorV;
    }

    /**
     * Gets the bearing set for this options object.
     *
     * @return the bearing of the ground overlay.
     */
    public float getBearing() {
        return bearing;
    }

    /**
     * Gets the bounds set for this options object.
     *
     * @return the bounds of the ground overlay. This will be {@code null} if the position was set
     * using {@link #position(LatLng, float)} or {@link #position(LatLng, float, float)}
     */
    public LatLngBounds getBounds() {
        return bounds;
    }

    /**
     * Gets the height set for this options object.
     *
     * @return the height of the ground overlay.
     */
    public float getHeight() {
        return height;
    }

    /**
     * Gets the image set for this options object.
     *
     * @return the image of the ground overlay.
     */
    public BitmapDescriptor getImage() {
        if (imageDescriptor == null && image != null) {
            imageDescriptor = new BitmapDescriptor(ObjectWrapper.asInterface(image));
        }
        return imageDescriptor;
    }

    /**
     * Gets the location set for this options object.
     *
     * @return the location to place the anchor of the ground overlay. This will be {@code null}
     * if the position was set using {@link #positionFromBounds(LatLngBounds)}.
     */
    public LatLng getLocation() {
        return location;
    }

    /**
     * Gets the transparency set for this options object.
     *
     * @return the transparency of the ground overlay.
     */
    public float getTransparency() {
        return transparency;
    }

    /**
     * Gets the width set for this options object.
     *
     * @return the width of the ground overlay.
     */
    public float getWidth() {
        return width;
    }

    /**
     * Gets the zIndex set for this options object.
     *
     * @return the zIndex of the ground overlay.
     */
    public float getZIndex() {
        return zIndex;
    }

    /**
     * Specifies the image for this ground overlay.
     * <p/>
     * To load an image as a texture (which is used to draw the image on a map), it must be
     * converted into an image with sides that are powers of two. This is so that a mipmap can be
     * created in order to render the texture at various zoom levels - see
     * <a href="http://en.wikipedia.org/wiki/Mipmap">Mipmap (Wikipedia)</a> for details. Hence, to
     * conserve memory by avoiding this conversion, it is advised that the dimensions of the image
     * are powers of two.
     *
     * @param image the {@link BitmapDescriptor} to use for this ground overlay
     * @return this {@link GroundOverlayOptions} object with a new image set.
     */
    public GroundOverlayOptions image(BitmapDescriptor image) {
        this.imageDescriptor = image;
        this.image = imageDescriptor.getRemoteObject().asBinder();
        return this;
    }

    /**
     * Gets the clickability setting for this {@link GroundOverlayOptions} object.
     *
     * @return {@code true} if the ground overlay is clickable; {@code false} if it is not.
     */
    public boolean isClickable() {
        return clickable;
    }

    /**
     * Gets the visibility setting for this options object.
     *
     * @return {@code true} if the ground overlay is to be visible; {@code false} if it is not.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Specifies the position for this ground overlay using an anchor point (a {@link LatLng}),
     * width and height (both in meters). When rendered, the image will be scaled to fit the
     * dimensions specified (i.e., its proportions will not necessarily be preserved).
     *
     * @param location the location on the map {@code LatLng} to which the anchor point in the
     *                 given image will remain fixed. The anchor will remain fixed to the position
     *                 on the ground when transformations are applied (e.g., setDimensions,
     *                 setBearing, etc.).
     * @param width    the width of the overlay (in meters)
     * @param height   the height of the overlay (in meters)
     * @return this {@link GroundOverlayOptions} object with a new position set.
     * @throws IllegalArgumentException if anchor is null
     * @throws IllegalArgumentException if width or height are negative
     * @throws IllegalStateException    if the position was already set using
     *                                  {@link #positionFromBounds(LatLngBounds)}
     */
    public GroundOverlayOptions position(LatLng location, float width, float height)
            throws IllegalArgumentException, IllegalStateException {
        position(location, width);
        if (height < 0)
            throw new IllegalArgumentException("height must not be negative");
        this.height = height;
        return this;
    }

    /**
     * Specifies the position for this ground overlay using an anchor point (a {@link LatLng}) and
     * the width (in meters). When rendered, the image will retain its proportions from the bitmap,
     * i.e., the height will be calculated to preserve the original proportions of the image.
     *
     * @param location the location on the map {@link LatLng} to which the anchor point in the
     *                 given image will remain fixed. The anchor will remain fixed to the position
     *                 on the ground when transformations are applied (e.g., setDimensions,
     *                 setBearing, etc.).
     * @param width    the width of the overlay (in meters). The height will be determined
     *                 automatically based on the image proportions.
     * @return this {@link GroundOverlayOptions} object with a new position set.
     * @throws IllegalArgumentException if anchor is null
     * @throws IllegalArgumentException if width is negative
     * @throws IllegalStateException    if the position was already set using
     *                                  {@link #positionFromBounds(LatLngBounds)}
     */
    public GroundOverlayOptions position(LatLng location, float width)
            throws IllegalArgumentException, IllegalStateException {
        if (location == null)
            throw new IllegalArgumentException("location must not be null");
        if (width < 0)
            throw new IllegalArgumentException("width must not be negative");
        if (bounds != null)
            throw new IllegalStateException("Position already set using positionFromBounds()");
        this.location = location;
        this.width = width;
        return this;
    }

    /**
     * Specifies the position for this ground overlay. When rendered, the image will be scaled to
     * fit the bounds (i.e., its proportions will not necessarily be preserved).
     *
     * @param bounds a {@link LatLngBounds} in which to place the ground overlay
     * @return this {@link GroundOverlayOptions} object with a new position set.
     * @throws IllegalStateException if the position was already set using
     *                               {@link #position(LatLng, float)} or
     *                               {@link #position(LatLng, float, float)}
     */
    public GroundOverlayOptions positionFromBounds(LatLngBounds bounds)
            throws IllegalStateException {
        if (location != null)
            throw new IllegalStateException("Position already set using position()");
        this.bounds = bounds;
        return this;
    }

    /**
     * Specifies the transparency of the ground overlay. The default transparency is {code 0}
     * (opaque).
     *
     * @param transparency a float in the range {@code [0..1]} where {@code 0} means that the
     *                     ground overlay is opaque and {code 1} means that the ground overlay is
     *                     transparent
     * @return this {@link GroundOverlayOptions} object with a new visibility setting.
     * @throws IllegalArgumentException if the transparency is outside the range [0..1].
     */
    public GroundOverlayOptions transparency(float transparency) throws IllegalArgumentException {
        if (transparency < 0 || transparency > 1)
            throw new IllegalArgumentException("transparency must be in range [0..1]");
        this.transparency = transparency;
        return this;
    }

    /**
     * Specifies the visibility for the ground overlay. The default visibility is {@code true}.
     *
     * @return this {@link GroundOverlayOptions} object with a new visibility setting.
     */
    public GroundOverlayOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Specifies the ground overlay's zIndex, i.e., the order in which it will be drawn. See the
     * documentation at the top of this class for more information about zIndex.
     *
     * @return this {@link GroundOverlayOptions} object with a new zIndex set.
     */
    public GroundOverlayOptions zIndex(float zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    public static Creator<GroundOverlayOptions> CREATOR = new AutoCreator<GroundOverlayOptions>(GroundOverlayOptions.class);
}
