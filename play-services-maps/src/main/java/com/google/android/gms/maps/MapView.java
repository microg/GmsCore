/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
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
import org.microg.gms.maps.MapViewLifecycleHelper;

/**
 * A View which displays a map. When focused, it will capture keypresses and touch gestures to move the map.
 * <p>
 * Users of this class must forward all the life cycle methods from the {@link Activity} or {@link Fragment} containing this view to the corresponding ones in
 * this class.
 * <p>
 * A {@link GoogleMap} must be acquired using {@link #getMapAsync(OnMapReadyCallback)}.
 * The {@link MapView} automatically initializes the maps system and the view.
 */
public class MapView extends FrameLayout {
    private final MapViewLifecycleHelper helper;

    public MapView(@NonNull Context context) {
        super(context);
        helper = new MapViewLifecycleHelper(this, context, null);
        setClickable(true);
    }

    public MapView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        helper = new MapViewLifecycleHelper(this, context, GoogleMapOptions.createFromAttributes(context, attrs));
        setClickable(true);
    }

    public MapView(@NonNull Context context, @NonNull AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        helper = new MapViewLifecycleHelper(this, context, GoogleMapOptions.createFromAttributes(context, attrs));
        setClickable(true);
    }

    /**
     * Constructs MapView with {@link GoogleMapOptions}.
     *
     * @param options configuration GoogleMapOptions for a {@link GoogleMap}, or {@code null} to use the default options.
     */
    public MapView(@NonNull Context context, @Nullable GoogleMapOptions options) {
        super(context);
        helper = new MapViewLifecycleHelper(this, context, options);
        setClickable(true);
    }

    /**
     * Returns a instance of the {@link GoogleMap} through the callback, ready to be used.
     * <p>
     * Note that:
     * <ul>
     * <li>This method must be called from the main thread.</li>
     * <li>The callback will be executed in the main thread.</li>
     * <li>In the case where Google Play services is not installed on the user's device, the callback will not be triggered until the user installs it.</li>
     * <li>The GoogleMap object provided by the callback is never null.</li>
     * </ul>
     *
     * @param callback The callback object that will be triggered when the map is ready to be used.
     */
    public void getMapAsync(OnMapReadyCallback callback) {
        helper.getMapAsync(callback);
    }

    /**
     * You must call this method from the parent Activity/Fragment's corresponding method.
     */
    public void onCreate(Bundle savedInstanceState) {
        helper.onCreate(savedInstanceState);
    }

    /**
     * You must call this method from the parent Activity/Fragment's corresponding method.
     */
    public void onDestroy() {
        helper.onDestroy();
    }

    /**
     * You must call this method from the parent WearableActivity's corresponding method.
     */
    public void onEnterAmbient(Bundle ambientDetails) {
        if (helper.getDelegate() != null) {
            helper.getDelegate().onEnterAmbient(ambientDetails);
        }
    }

    /**
     * You must call this method from the parent WearableActivity's corresponding method.
     */
    public void onExitAmbient() {
        if (helper.getDelegate() != null) {
            helper.getDelegate().onExitAmbient();
        }
    }

    /**
     * You must call this method from the parent Activity/Fragment's corresponding method.
     */
    public void onLowMemory() {
        helper.onLowMemory();
    }

    /**
     * You must call this method from the parent Activity/Fragment's corresponding method.
     */
    public void onPause() {
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
     * <p>
     * Provides a {@link Bundle} to store the state of the View before it gets destroyed.
     * It can later be retrieved when {@link #onCreate(Bundle)} is called again.
     */
    public void onSaveInstanceState(Bundle outState) {
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
