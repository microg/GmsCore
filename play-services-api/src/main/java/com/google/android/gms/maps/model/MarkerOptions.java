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

import android.os.IBinder;
import com.google.android.gms.dynamic.ObjectWrapper;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

@PublicApi
public class MarkerOptions extends AutoSafeParcelable {

    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    private LatLng position;
    @SafeParceled(3)
    private String title;
    @SafeParceled(4)
    private String snippet;
    /**
     * This is a IBinder to the remote BitmapDescriptor created using BitmapDescriptorFactory
     */
    @SafeParceled(5)
    private IBinder iconBinder;
    private BitmapDescriptor icon;
    @SafeParceled(6)
    private float anchorU = 0.5F;
    @SafeParceled(7)
    private float anchorV = 1F;
    @SafeParceled(8)
    private boolean draggable;
    @SafeParceled(9)
    private boolean visible;
    @SafeParceled(10)
    private boolean flat;
    @SafeParceled(11)
    private float rotation = 0F;
    @SafeParceled(12)
    private float infoWindowAnchorU = 0F;
    @SafeParceled(13)
    private float infoWindowAnchorV = 1F;
    @SafeParceled(14)
    private float alpha = 1F;
    @SafeParceled(15)
    private float zIndex = 0F;

    /**
     * Creates a new set of marker options.
     */
    public MarkerOptions() {
    }

    /**
     * Sets the alpha (opacity) of the marker. This is a value from 0 to 1, where 0 means the
     * marker is completely transparent and 1 means the marker is completely opaque.
     *
     * @return the object for which the method was called, with the new alpha set.
     */
    public MarkerOptions alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    /**
     * Specifies the anchor to be at a particular point in the marker image.
     * <p/>
     * The anchor specifies the point in the icon image that is anchored to the marker's position
     * on the Earth's surface.
     * <p/>
     * The anchor point is specified in the continuous space [0.0, 1.0] x [0.0, 1.0], where (0, 0)
     * is the top-left corner of the image, and (1, 1) is the bottom-right corner. The anchoring
     * point in a W x H image is the nearest discrete grid point in a (W + 1) x (H + 1) grid,
     * obtained by scaling the then rounding. For example, in a 4 x 2 image, the anchor point
     * (0.7, 0.6) resolves to the grid point at (3, 1).
     *
     * @param u u-coordinate of the anchor, as a ratio of the image width (in the range [0, 1])
     * @param v v-coordinate of the anchor, as a ratio of the image height (in the range [0, 1])
     * @return the object for which the method was called, with the new anchor set.
     */
    public MarkerOptions anchor(float u, float v) {
        this.anchorU = u;
        this.anchorV = v;
        return this;
    }

    /**
     * Sets the draggability for the marker.
     *
     * @return the object for which the method was called, with the new draggable state set.
     */
    public MarkerOptions draggable(boolean draggable) {
        this.draggable = draggable;
        return this;
    }

    /**
     * Sets whether this marker should be flat against the map true or a billboard facing the
     * camera false. If the marker is flat against the map, it will remain stuck to the map as the
     * camera rotates and tilts but will still remain the same size as the camera zooms, unlike a
     * GroundOverlay. If the marker is a billboard, it will always be drawn facing the camera
     * and will rotate and tilt with the camera. The default value is false.
     *
     * @return the object for which the method was called, with the new flat state set.
     */
    public MarkerOptions flat(boolean flat) {
        this.flat = flat;
        return this;
    }

    /**
     * Gets the alpha set for this MarkerOptions object.
     *
     * @return the alpha of the marker in the range [0, 1].
     */
    public float getAlpha() {
        return alpha;
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
     * Gets the custom icon set for this MarkerOptions object.
     *
     * @return An {@link BitmapDescriptor} representing the custom icon, or {@code null} if no
     * custom icon is set.
     */
    public BitmapDescriptor getIcon() {
        if (icon == null && iconBinder != null) {
            icon = new BitmapDescriptor(ObjectWrapper.asInterface(iconBinder));
        }
        return icon;
    }

    /**
     * Horizontal distance, normalized to [0, 1], of the info window anchor from the left edge.
     *
     * @return the u value of the info window anchor.
     */
    public float getInfoWindowAnchorU() {
        return infoWindowAnchorU;
    }

    /**
     * Vertical distance, normalized to [0, 1], of the info window anchor from the top edge.
     *
     * @return the v value of the info window anchor.
     */
    public float getInfoWindowAnchorV() {
        return infoWindowAnchorV;
    }

    /**
     * Returns the position set for this MarkerOptions object.
     *
     * @return A {@link LatLng} object specifying the marker's current position.
     */
    public LatLng getPosition() {
        return position;
    }

    /**
     * Gets the rotation set for this MarkerOptions object.
     *
     * @return the rotation of the marker in degrees clockwise from the default position.
     */
    public float getRotation() {
        return rotation;
    }

    /**
     * Gets the snippet set for this MarkerOptions object.
     *
     * @return A string containing the marker's snippet.
     */
    public String getSnippet() {
        return snippet;
    }

    /**
     * Gets the title set for this MarkerOptions object.
     *
     * @return A string containing the marker's title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the icon for the marker.
     *
     * @param icon if null, the default marker is used.
     * @return the object for which the method was called, with the new icon set.
     */
    public MarkerOptions icon(BitmapDescriptor icon) {
        this.icon = icon;
        this.iconBinder = icon == null ? null : icon.getRemoteObject().asBinder();
        return this;
    }

    /**
     * Specifies the anchor point of the info window on the marker image. This is specified in the
     * same coordinate system as the anchor. See {@link MarkerOptions#anchor(float, float)} for
     * more details. The default is the top middle of the image.
     *
     * @param u u-coordinate of the info window anchor, as a ratio of the image width (in the range [0, 1])
     * @param v v-coordinate of the info window anchor, as a ratio of the image height (in the range [0, 1])
     * @return the object for which the method was called, with the new info window anchor set.
     */
    public MarkerOptions infoWindowAnchor(float u, float v) {
        this.infoWindowAnchorU = u;
        this.infoWindowAnchorV = v;
        return this;
    }

    /**
     * Gets the draggability setting for this MarkerOptions object.
     *
     * @return {@code true} if the marker is draggable; otherwise, returns {@code false}.
     */
    public boolean isDraggable() {
        return draggable;
    }

    /**
     * Gets the flat setting for this MarkerOptions object.
     *
     * @return {@code true} if the marker is flat against the map; {@code false} if the marker
     * should face the camera.
     */
    public boolean isFlat() {
        return flat;
    }

    /**
     * Gets the visibility setting for this MarkerOptions object.
     *
     * @return {@code true} if the marker is visible; otherwise, returns {@code false}.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets the location for the marker.
     *
     * @return the object for which the method was called, with the new position set.
     */
    public MarkerOptions position(LatLng position) {
        this.position = position;
        return this;
    }

    /**
     * Sets the rotation of the marker in degrees clockwise about the marker's anchor point. The
     * axis of rotation is perpendicular to the marker. A rotation of 0 corresponds to the default
     * position of the marker. When the marker is flat on the map, the default position is North
     * aligned and the rotation is such that the marker always remains flat on the map. When the
     * marker is a billboard, the default position is pointing up and the rotation is such that
     * the marker is always facing the camera. The default value is 0.
     *
     * @return the object for which the method was called, with the new rotation set.
     */
    public MarkerOptions rotation(float rotation) {
        this.rotation = rotation;
        return this;
    }

    /**
     * Sets the snippet for the marker.
     *
     * @return the object for which the method was called, with the new snippet set.
     */
    public MarkerOptions snippet(String snippet) {
        this.snippet = snippet;
        return this;
    }

    /**
     * Sets the title for the marker.
     *
     * @return the object for which the method was called, with the new title set.
     */
    public MarkerOptions title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the visibility for the marker.
     *
     * @return the object for which the method was called, with the new visibility state set.
     */
    public MarkerOptions visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public MarkerOptions zIndex(float zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    public float getZIndex() {
        return this.zIndex;
    }

    public static Creator<MarkerOptions> CREATOR = new AutoCreator<MarkerOptions>(MarkerOptions.class);
}
