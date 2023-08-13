/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.SurfaceView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLngBounds;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Defines configuration GoogleMapOptions for a {@link GoogleMap}. These options can be used when adding a map to your application programmatically
 * (as opposed to via XML). If you are using a {@link MapFragment}, you can pass these options in using the static factory method
 * {@link MapFragment#newInstance(GoogleMapOptions)}. If you are using a {@link MapView}, you can pass these options in using the constructor
 * {@link MapView#MapView(Context, GoogleMapOptions)}.
 * <p>
 * If you add a map using XML, then you can apply these options using custom XML tags.
 */
public final class GoogleMapOptions extends AutoSafeParcelable {
    @Field(1)
    private int versionCode;
    @Field(2)
    private int zOrderOnTop;
    @Field(3)
    private boolean useViewLifecycleInFragment;
    @Field(4)
    private int mapType;
    @Field(5)
    @Nullable
    private CameraPosition camera;
    @Field(6)
    private boolean zoomControlsEnabled;
    @Field(7)
    private boolean compassEnabled;
    @Field(8)
    private boolean scrollGesturesEnabled = true;
    @Field(9)
    private boolean zoomGesturesEnabled = true;
    @Field(10)
    private boolean tiltGesturesEnabled = true;
    @Field(11)
    private boolean rotateGesturesEnabled = true;
    @Field(12)
    private int liteMode = 0;
    @Field(14)
    private boolean mapToobarEnabled = false;
    @Field(15)
    private boolean ambientEnabled = false;
    @Field(16)
    private float minZoom;
    @Field(17)
    private float maxZoom;
    @Field(18)
    @Nullable
    private LatLngBounds boundsForCamera;
    @Field(19)
    private boolean scrollGesturesEnabledDuringRotateOrZoom = true;
    @Field(20)
    @ColorInt
    @Nullable
    private Integer backgroundColor;
    @Field(21)
    @Nullable
    private String mapId;

    /**
     * Creates a new GoogleMapOptions object.
     */
    public GoogleMapOptions() {
    }

    /**
     * Creates a {@code GoogleMapsOptions} from the {@link AttributeSet}.
     */
    public static @Nullable GoogleMapOptions createFromAttributes(@Nullable Context context, @Nullable AttributeSet attrs) {
        if (context == null || attrs == null) return null;
        GoogleMapOptions options = new GoogleMapOptions();
        TypedArray obtainAttributes = context.getResources().obtainAttributes(attrs, R.styleable.MapAttrs);
        // TODO: Handle attributes
        if (obtainAttributes.hasValue(R.styleable.MapAttrs_mapType)) {
//            options.mapType(obtainAttributes.getInt(R.styleable.MapAttrs_mapType, -1));
        }
        obtainAttributes.recycle();
        return options;
    }

    /**
     * Specifies whether ambient-mode styling should be enabled. The default value is {@code false}.
     * When enabled, ambient-styled maps can be displayed when an Ambiactive device enters ambient mode.
     */
    @NonNull
    public GoogleMapOptions ambientEnabled(boolean enabled) {
        this.ambientEnabled = enabled;
        return this;
    }

    /**
     * Sets the map background color. This is the color that shows underneath map tiles and displays whenever the renderer does not have a tile available for
     * a portion of the viewport.
     *
     * @param backgroundColor the color to show in the background of the map. If {@code null} is supplied then the map uses the default renderer background color.
     */
    @NonNull
    public GoogleMapOptions backgroundColor(@Nullable @ColorInt Integer backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    /**
     * Specifies the initial camera position for the map (specify null to use the default camera position).
     */
    @NonNull
    public GoogleMapOptions camera(@Nullable CameraPosition camera) {
        this.camera = camera;
        return this;
    }

    /**
     * Specifies whether the compass should be enabled. See {@link UiSettings#setCompassEnabled(boolean)} for more details. The default value is {@code true}.
     */
    @NonNull
    public GoogleMapOptions compassEnabled(boolean enabled) {
        this.compassEnabled = enabled;
        return this;
    }

    /**
     * Specifies a LatLngBounds to constrain the camera target, so that when users scroll and pan the map, the camera target does not move outside these bounds.
     * <p>
     * See {@link GoogleMap#setLatLngBoundsForCameraTarget(LatLngBounds)} for details.
     */
    @NonNull
    public GoogleMapOptions latLngBoundsForCameraTarget(@Nullable LatLngBounds llbounds) {
        this.boundsForCamera = llbounds;
        return this;
    }

    /**
     * Specifies whether the map should be created in lite mode. The default value is {@code false}. If lite mode is enabled, maps will load as static images.
     * This improves performance in the case where a lot of maps need to be displayed at the same time, for example in a scrolling list, however lite-mode maps
     * cannot be panned or zoomed by the user, or tilted or rotated at all.
     */
    @NonNull
    public GoogleMapOptions liteMode(boolean enabled) {
        this.liteMode = enabled ? 1 : 0;
        return this;
    }

    /**
     * Specifies the map's ID.
     */
    @NonNull
    public GoogleMapOptions mapId(@NonNull String mapId) {
        this.mapId = mapId;
        return this;
    }

    /**
     * Specifies whether the mapToolbar should be enabled. See {@link UiSettings#setMapToolbarEnabled(boolean)} for more details.
     * The default value is {@code true}.
     */
    @NonNull
    public GoogleMapOptions mapToolbarEnabled(boolean enabled) {
        this.mapToobarEnabled = enabled;
        return this;
    }

    /**
     * Specifies a change to the initial map type.
     */
    @NonNull
    public GoogleMapOptions mapType(int mapType) {
        this.mapType = mapType;
        return this;
    }

    /**
     * Specifies a preferred upper bound for camera zoom.
     * <p>
     * See {@link GoogleMap#setMaxZoomPreference(float)} for details.
     */
    @NonNull
    public GoogleMapOptions maxZoomPreference(float maxZoomPreference) {
        this.maxZoom = maxZoomPreference;
        return this;
    }

    /**
     * Specifies a preferred lower bound for camera zoom.
     * <p>
     * See {@link GoogleMap#setMinZoomPreference(float)} for details.
     */
    @NonNull
    public GoogleMapOptions minZoomPreference(float minZoomPreference) {
        this.minZoom = minZoomPreference;
        return this;
    }

    /**
     * Specifies whether rotate gestures should be enabled. See {@link UiSettings#setRotateGesturesEnabled(boolean)} for more details.
     * The default value is {@code true}.
     */
    @NonNull
    public GoogleMapOptions rotateGesturesEnabled(boolean enabled) {
        this.rotateGesturesEnabled = enabled;
        return this;
    }

    /**
     * Specifies whether scroll gestures should be enabled. See {@link UiSettings#setScrollGesturesEnabled(boolean)} for more details.
     * The default value is {@code true}.
     */
    @NonNull
    public GoogleMapOptions scrollGesturesEnabled(boolean enabled) {
        this.scrollGesturesEnabled = enabled;
        return this;
    }

    /**
     * Specifies whether scroll gestures should be enabled during rotate and zoom gestures.
     * See {@link UiSettings#setScrollGesturesEnabledDuringRotateOrZoom(boolean)} for more details. The default value is {@code true}.
     */
    @NonNull
    public GoogleMapOptions scrollGesturesEnabledDuringRotateOrZoom(boolean enabled) {
        this.scrollGesturesEnabledDuringRotateOrZoom = enabled;
        return this;
    }

    /**
     * Specifies whether tilt gestures should be enabled. See {@link UiSettings#setTiltGesturesEnabled(boolean)} for more details.
     * The default value is {@code true}.
     */
    @NonNull
    public GoogleMapOptions tiltGesturesEnabled(boolean enabled) {
        this.tiltGesturesEnabled = enabled;
        return this;
    }

    /**
     * When using a {@link MapFragment}, this flag specifies whether the lifecycle of the map should be tied to the fragment's view or the fragment itself.
     * The default value is {@code false}, tying the lifecycle of the map to the fragment.
     * <p>
     * Using the lifecycle of the fragment allows faster rendering of the map when the fragment is detached and reattached, because the underlying GL context
     * is preserved. This has the cost that detaching the fragment, but not destroying it, will not release memory used by the map.
     * <p>
     * Using the lifecycle of a fragment's view means that a map is not reused when the fragment is detached and reattached. This will cause the map to
     * re-render from scratch, which can take a few seconds. It also means that while a fragment is detached, and therefore has no view, all {@link GoogleMap}
     * methods will throw {@link NullPointerException}.
     */
    @NonNull
    public GoogleMapOptions useViewLifecycleInFragment(boolean useViewLifecycleInFragment) {
        this.useViewLifecycleInFragment = useViewLifecycleInFragment;
        return this;
    }

    /**
     * Control whether the map view's surface is placed on top of its window. See {@link SurfaceView#setZOrderOnTop(boolean)} for more details.
     * Note that this will cover all other views that could appear on the map (e.g., the zoom controls, the my location button).
     */
    @NonNull
    public GoogleMapOptions zOrderOnTop(boolean zOrderOnTop) {
        this.zOrderOnTop = zOrderOnTop ? 1 : 0;
        return this;
    }

    /**
     * Specifies whether the zoom controls should be enabled. See {@link UiSettings#setZoomControlsEnabled(boolean)} for more details.
     * The default value is {@code true}.
     */
    @NonNull
    public GoogleMapOptions zoomControlsEnabled(boolean enabled) {
        this.zoomControlsEnabled = enabled;
        return this;
    }

    /**
     * Specifies whether zoom gestures should be enabled. See {@link UiSettings#setZoomGesturesEnabled(boolean)} for more details.
     * The default value is {@code true}.
     */
    @NonNull
    public GoogleMapOptions zoomGesturesEnabled(boolean enabled) {
        this.zoomGesturesEnabled = enabled;
        return this;
    }

    /**
     * @return the {@code ambientEnabled} option, or {@code null} if unspecified.
     */
    public Boolean getAmbientEnabled() {
        return ambientEnabled;
    }

    /**
     * @return the current backgroundColor for the map, or null if unspecified.
     */
    public Integer getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * @return the camera option, or {@code null} if unspecified.
     */
    @Nullable
    public CameraPosition getCamera() {
        return camera;
    }

    /**
     * @return the {@code compassEnabled} option, or {@code null} if unspecified.
     */
    @Nullable
    public Boolean getCompassEnabled() {
        return compassEnabled;
    }

    /**
     * @return the {@code LatLngBounds} used to constrain the camera target, or {@code null} if unspecified.
     */
    @Nullable
    public LatLngBounds getLatLngBoundsForCameraTarget() {
        return boundsForCamera;
    }

    /**
     * @return the {@code liteMode} option, or {@code null} if unspecified.
     */
    @Nullable
    public boolean getLiteMode() {
        // Is encoded as `-1` if null, `0` if false, `1` if true. The default is false.
        return liteMode == 1;
    }

    /**
     * @return the {@code mapId}, or {@code null} if unspecified.
     */
    @Nullable
    public String getMapId() {
        return mapId;
    }

    /**
     * @return the {@code mapToolbarEnabled} option, or {@code null} if unspecified.
     */
    @Nullable
    public Boolean getMapToolbarEnabled() {
        return mapToobarEnabled;
    }

    /**
     * @return the {@code mapType} option, or -1 if unspecified.
     */
    public int getMapType() {
        return mapType;
    }

    /**
     * @return the maximum zoom level preference, or {@code null} if unspecified.
     */
    @Nullable
    public Float getMaxZoomPreference() {
        return maxZoom;
    }

    /**
     * @return the minimum zoom level preference, or {@code null} if unspecified.
     */
    @Nullable
    public Float getMinZoomPreference() {
        return minZoom;
    }

    /**
     * @return the {@code rotateGesturesEnabled} option, or {@code null} if unspecified.
     */
    @Nullable
    public Boolean getRotateGesturesEnabled() {
        return rotateGesturesEnabled;
    }

    /**
     * @return the {@code scrollGesturesEnabled} option, or {@code null} if unspecified.
     */
    @Nullable
    public Boolean getScrollGesturesEnabled() {
        return scrollGesturesEnabled;
    }

    /**
     * @return the {@code scrollGesturesEnabledDuringRotateOrZoom} option, or {@code null} if unspecified.
     */
    @Nullable
    public Boolean getScrollGesturesEnabledDuringRotateOrZoom() {
        return scrollGesturesEnabledDuringRotateOrZoom;
    }

    /**
     * @return the {@code tiltGesturesEnabled} option, or {@code null} if unspecified.
     */
    @Nullable
    public Boolean getTiltGesturesEnabled() {
        return tiltGesturesEnabled;
    }

    /**
     * @return the {@code useViewLifecycleInFragment} option, or {@code null} if unspecified.
     */
    @Nullable
    public Boolean getUseViewLifecycleInFragment() {
        return useViewLifecycleInFragment;
    }

    /**
     * @return the {@code zOrderOnTop} option, or {@code null} if unspecified.
     */
    @Nullable
    public Boolean getZOrderOnTop() {
        return zOrderOnTop == 1; // TODO
    }

    /**
     * @return the {@code zoomControlsEnabled} option, or {@code null} if unspecified.
     */
    @Nullable
    public Boolean getZoomControlsEnabled() {
        return zoomControlsEnabled;
    }

    /**
     * @return the {@code zoomGesturesEnabled} option, or {@code null} if unspecified.
     */
    @Nullable
    public Boolean getZoomGesturesEnabled() {
        return zoomGesturesEnabled;
    }

    @Deprecated
    public boolean isCompassEnabled() {
        return compassEnabled;
    }

    @Deprecated
    public boolean isZoomControlsEnabled() {
        return zoomControlsEnabled;
    }

    @Deprecated
    public boolean isScrollGesturesEnabled() {
        return scrollGesturesEnabled;
    }

    @Deprecated
    public boolean isZoomGesturesEnabled() {
        return zoomGesturesEnabled;
    }

    @Deprecated
    public boolean isTiltGesturesEnabled() {
        return tiltGesturesEnabled;
    }

    @Deprecated
    public boolean isRotateGesturesEnabled() {
        return rotateGesturesEnabled;
    }


    public static Creator<GoogleMapOptions> CREATOR = new AutoCreator<GoogleMapOptions>(GoogleMapOptions.class);
}
