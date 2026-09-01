/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps;

import android.content.Context;
import android.os.Parcel;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.android.gms.maps.model.StreetViewSource;
import org.microg.gms.utils.ToStringHelper;

/**
 * Defines configuration PanoramaOptions for a {@link StreetViewPanorama}. These options can be used when adding a panorama to your
 * application programmatically. If you are using a {@link StreetViewPanoramaFragment}, you can pass these options in using the static factory
 * method {@link StreetViewPanoramaFragment#newInstance(StreetViewPanoramaOptions)}. If you are using a {@link StreetViewPanoramaView},
 * you can pass these options in using the constructor {@link StreetViewPanoramaView#StreetViewPanoramaView(Context, StreetViewPanoramaOptions)}.
 */
@SafeParcelable.Class
public class StreetViewPanoramaOptions extends AbstractSafeParcelable {
    @Nullable
    @Field(value = 2, getterName = "getStreetViewPanoramaCamera")
    private StreetViewPanoramaCamera panoramaCamera;
    @Nullable
    @Field(value = 3, getterName = "getPanoramaId")
    private String panoramaId;
    @Nullable
    @Field(value = 4, getterName = "getPosition")
    private LatLng position;
    @Nullable
    @Field(value = 5, getterName = "getRadius")
    private Integer radius;
    @Nullable
    @Field(value = 6, getterName = "getUserNavigationEnabled")
    private Boolean userNavigationEnabled = true;
    @Nullable
    @Field(value = 7, getterName = "getZoomGesturesEnabled")
    private Boolean zoomGesturesEnabled = true;
    @Nullable
    @Field(value = 8, getterName = "getPanningGesturesEnabled")
    private Boolean panningGesturesEnabled = true;
    @Nullable
    @Field(value = 9, getterName = "getStreetNamesEnabled")
    private Boolean streetNamesEnabled = true;
    @Nullable
    @Field(value = 10, getterName = "getUseViewLifecycleInFragment")
    private Boolean useViewLifecycleInFragment = false;
    @Field(value = 11, getterName = "getSource")
    private StreetViewSource source = StreetViewSource.DEFAULT;

    /**
     * Creates a new StreetViewPanoramaOptions object.
     */
    public StreetViewPanoramaOptions() {
    }

    @Constructor
    StreetViewPanoramaOptions(@Nullable @Param(2) StreetViewPanoramaCamera panoramaCamera, @Nullable @Param(3) String panoramaId, @Nullable @Param(4) LatLng position, @Nullable @Param(5) Integer radius, @Nullable @Param(6) Boolean userNavigationEnabled, @Nullable @Param(7) Boolean zoomGesturesEnabled, @Nullable @Param(8) Boolean panningGesturesEnabled, @Nullable @Param(9) Boolean streetNamesEnabled, @Nullable @Param(10) Boolean useViewLifecycleInFragment, @Param(11) StreetViewSource source) {
        this.panoramaCamera = panoramaCamera;
        this.panoramaId = panoramaId;
        this.position = position;
        this.radius = radius;
        this.userNavigationEnabled = userNavigationEnabled;
        this.zoomGesturesEnabled = zoomGesturesEnabled;
        this.panningGesturesEnabled = panningGesturesEnabled;
        this.streetNamesEnabled = streetNamesEnabled;
        this.useViewLifecycleInFragment = useViewLifecycleInFragment;
        this.source = source;
    }

    /**
     * Returns {@code true} if users are initially able to pan via gestures on Street View panoramas.
     */
    @Nullable
    public Boolean getPanningGesturesEnabled() {
        return panningGesturesEnabled;
    }

    /**
     * Returns the initial panorama ID for the Street View panorama, or {@code null} if unspecified.
     */
    @Nullable
    public String getPanoramaId() {
        return panoramaId;
    }

    /**
     * Returns the initial position for the Street View panorama, or {@code null} if unspecified.
     */
    @Nullable
    public LatLng getPosition() {
        return position;
    }

    /**
     * Returns the initial radius used to search for a Street View panorama, or {@code null} if unspecified.
     */
    @Nullable
    public Integer getRadius() {
        return radius;
    }

    /**
     * Returns the source filter used to search for a Street View panorama, or {@link StreetViewSource#DEFAULT} if unspecified.
     */
    @NonNull
    public StreetViewSource getSource() {
        return source;
    }

    /**
     * Returns {@code true} if users are initially able to see street names on Street View panoramas.
     */
    @Nullable
    public Boolean getStreetNamesEnabled() {
        return streetNamesEnabled;
    }

    /**
     * Returns the initial camera for the Street View panorama, or {@code null} if unspecified.
     */
    @Nullable
    public StreetViewPanoramaCamera getStreetViewPanoramaCamera() {
        return panoramaCamera;
    }

    /**
     * Returns the useViewLifecycleInFragment option, or {@code null} if unspecified.
     */
    @Nullable
    public Boolean getUseViewLifecycleInFragment() {
        return useViewLifecycleInFragment;
    }

    /**
     * Returns {@code true} if users are initially able to move to different Street View panoramas.
     */
    @Nullable
    public Boolean getUserNavigationEnabled() {
        return userNavigationEnabled;
    }

    /**
     * Returns {@code true} if users are initially able to zoom via gestures on Street View panoramas.
     */
    @Nullable
    public Boolean getZoomGesturesEnabled() {
        return zoomGesturesEnabled;
    }

    /**
     * Toggles the ability for users to use pan around on panoramas using gestures. See
     * {@link StreetViewPanorama#setPanningGesturesEnabled(boolean)} for more details. The default is {@code true}
     */
    @NonNull
    public StreetViewPanoramaOptions panningGesturesEnabled(boolean enabled) {
        this.panningGesturesEnabled = enabled;
        return this;
    }

    /**
     * Specifies the initial camera for the Street View panorama.
     */
    @NonNull
    public StreetViewPanoramaOptions panoramaCamera(StreetViewPanoramaCamera camera) {
        this.panoramaCamera = camera;
        return this;
    }

    /**
     * Specifies the initial position for the Street View panorama based on a panorama id. The position set by the panoramaID takes precedence
     * over a position set by a LatLng.
     */
    @NonNull
    public StreetViewPanoramaOptions panoramaId(String panoId) {
        panoramaId = panoId;
        return this;
    }

    /**
     * Specifies the initial position for the Street View panorama based upon location. The position set by the panoramaID, if set, takes precedence
     * over a position set by a LatLng.
     */
    @NonNull
    public StreetViewPanoramaOptions position(LatLng position) {
        this.position = position;
        return this;
    }

    /**
     * Specifies the initial position for the Street View panorama based upon location, radius and source. The position set by the panoramaID, if
     * set, takes precedence over a position set by a LatLng.
     */
    @NonNull
    public StreetViewPanoramaOptions position(LatLng position, Integer radius, StreetViewSource source) {
        this.position = position;
        this.radius = radius;
        this.source = source;
        return this;
    }

    /**
     * Specifies the initial position for the Street View panorama based upon location and radius. The position set by the panoramaID, if set, takes
     * precedence over a position set by a LatLng.
     */
    @NonNull
    public StreetViewPanoramaOptions position(LatLng position, Integer radius) {
        this.position = position;
        this.radius = radius;
        return this;
    }

    /**
     * Specifies the initial position for the Street View panorama based upon location and source. The position set by the panoramaID, if set, takes
     * precedence over a position set by a LatLng.
     */
    @NonNull
    public StreetViewPanoramaOptions position(LatLng position, StreetViewSource source) {
        this.position = position;
        this.source = source;
        return this;
    }

    /**
     * Toggles the ability for users to see street names on panoramas. See {@link StreetViewPanorama#setStreetNamesEnabled(boolean)} for more
     * details. The default is {@code true}
     */
    @NonNull
    public StreetViewPanoramaOptions streetNamesEnabled(boolean enabled) {
        this.streetNamesEnabled = enabled;
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("StreetViewPanoramaOptions")
                .field("PanoramaId", panoramaId)
                .field("Position", position)
                .field("Radius", radius)
                .field("Source", source)
                .field("StreetViewPanoramaCamera", panoramaCamera)
                .field("UserNavigationEnabled", userNavigationEnabled)
                .field("ZoomGesturesEnabled", zoomGesturesEnabled)
                .field("PanningGesturesEnabled", panningGesturesEnabled)
                .field("StreetNamesEnabled", streetNamesEnabled)
                .field("UseViewLifecycleInFragment", useViewLifecycleInFragment)
                .end();
    }

    /**
     * When using a {@link StreetViewPanoramaFragment}, this flag specifies whether the lifecycle of the Street View panorama should be tied to the
     * fragment's view or the fragment itself. The default value is {@code false}, tying the lifecycle of the Street View panorama to the fragment.
     * <p>
     * Using the lifecycle of the fragment allows faster rendering of the Street View panorama when the fragment is detached and reattached,
     * because the underlying GL context is preserved. This has the cost that detaching the fragment, but not destroying it, will not release memory
     * used by the panorama.
     * <p>
     * Using the lifecycle of a fragment's view means that a Street View panorama is not reused when the fragment is detached and reattached.
     * This will cause the map to re-render from scratch, which can take a few seconds. It also means that while a fragment is detached, and
     * therefore has no view, all {@link StreetViewPanorama} methods will throw {@link NullPointerException}.
     */
    @NonNull
    public StreetViewPanoramaOptions useViewLifecycleInFragment(boolean useViewLifecycleInFragment) {
        this.useViewLifecycleInFragment = useViewLifecycleInFragment;
        return this;
    }

    /**
     * Toggles the ability for users to move between panoramas. See {@link StreetViewPanorama#setUserNavigationEnabled(boolean)} for more
     * details. The default is {@code true}
     */
    @NonNull
    public StreetViewPanoramaOptions userNavigationEnabled(boolean enabled) {
        this.userNavigationEnabled = enabled;
        return this;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    /**
     * Toggles the ability for users to zoom on panoramas using gestures. See {@link StreetViewPanorama#setZoomGesturesEnabled(boolean)} for
     * more details. The default is {@code true}
     */
    @NonNull
    public StreetViewPanoramaOptions zoomGesturesEnabled(boolean enabled) {
        this.zoomGesturesEnabled = enabled;
        return this;
    }

    public static final SafeParcelableCreatorAndWriter<StreetViewPanoramaOptions> CREATOR = findCreator(StreetViewPanoramaOptions.class);

}
