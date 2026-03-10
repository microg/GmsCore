/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.microg.gms.maps.StreetViewPanoramaViewLifecycleHelper;

/**
 * A View which displays a Street View panorama (with data obtained from the Google Maps service). When focused, it captures keypresses
 * and touch gestures to move the panorama.
 * <p>
 * Users of this class must forward all the life cycle methods from the {@link Activity} or {@link Fragment} containing this view to the corresponding ones
 * in this class.
 * <p>
 * A StreetViewPanorama must be acquired using {@link #getStreetViewPanoramaAsync(OnStreetViewPanoramaReadyCallback)}. The
 * {@link StreetViewPanoramaView} automatically initializes the Street View system and the view.
 */
public class StreetViewPanoramaView extends FrameLayout {
    private final StreetViewPanoramaViewLifecycleHelper helper;

    public StreetViewPanoramaView(@NonNull Context context) {
        super(context);
        this.helper = new StreetViewPanoramaViewLifecycleHelper(this, context, null);
    }

    public StreetViewPanoramaView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.helper = new StreetViewPanoramaViewLifecycleHelper(this, context, null);
    }

    public StreetViewPanoramaView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.helper = new StreetViewPanoramaViewLifecycleHelper(this, context, null);
    }

    /**
     * @param context The context that will show the view. Must not be {@code null}.
     * @param options Configuration options for the new view. The view will be configured with default values if {@code options} is {@code null} or any option field is left {@code null}.
     */
    public StreetViewPanoramaView(@NonNull Context context, StreetViewPanoramaOptions options) {
        super(context);
        this.helper = new StreetViewPanoramaViewLifecycleHelper(this, context, options);
    }

    /**
     * Sets a callback object which will be triggered when the {@link StreetViewPanorama} instance is ready to be used.
     *
     * @param callback The callback object that will be triggered when the panorama is ready to be used. Must not be {@code null}.
     */
    public void getStreetViewPanoramaAsync(OnStreetViewPanoramaReadyCallback callback) {
        helper.getStreetViewPanoramaAsync(callback);
    }

    /**
     * You must call this method from the parent Activity/Fragment's corresponding method.
     */
    public final void onCreate(Bundle savedInstanceState) {
        helper.onCreate(savedInstanceState);
    }

    /**
     * You must call this method from the parent Activity/Fragment's corresponding method.
     */
    public void onDestroy() {
        helper.onDestroy();
    }

    /**
     * You must call this method from the parent Activity/Fragment's corresponding method.
     */
    public final void onLowMemory() {
        helper.onLowMemory();
    }

    /**
     * You must call this method from the parent Activity/Fragment's corresponding method.
     */
    public final void onPause() {
        helper.onPause();
    }

    /**
     * You must call this method from the parent Activity/Fragment's corresponding method.
     */
    public void onResume() {
        helper.onResume();
    }

    /**
     * You must call this method from the parent Activity/Fragment's corresponding method.
     */
    public final void onSaveInstanceState(Bundle outState) {
        helper.onSaveInstanceState(outState);
    }

    /**
     * You must call this method from the parent Activity/Fragment's corresponding method.
     */
    public void onStart() {
        helper.onStart();
    }

    /**
     * You must call this method from the parent Activity/Fragment's corresponding method.
     */
    public void onStop() {
        helper.onStop();
    }
}
