/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps;

import android.view.View;
import android.view.ViewTreeObserver;
import com.google.android.gms.maps.model.LatLngBounds;

/**
 * Callback interface for when the map is ready to be used.
 * <p>
 * Once an instance of this interface is set on a {@link MapFragment} or {@link MapView} object, the {@link #onMapReady(GoogleMap)} method is triggered when
 * the map is ready to be used and provides a non-null instance of {@link GoogleMap}.
 * <p>
 * If required services are not installed on the device, the user will be prompted to install it, and the {@link #onMapReady(GoogleMap)} method will only be
 * triggered when the user has installed it and returned to the app.
 */
public interface OnMapReadyCallback {
    /**
     * Called when the map is ready to be used.
     * <p>
     * Note that this does not guarantee that the map has undergone layout. Therefore, the map's size may not have been determined by the time the callback
     * method is called. If you need to know the dimensions or call a method in the API that needs to know the dimensions, get the map's {@link View} and
     * register an {@link ViewTreeObserver.OnGlobalLayoutListener} as well.
     * <p>
     * Do not chain the {@code OnMapReadyCallback} and {@code OnGlobalLayoutListener} listeners, but instead register and wait for both callbacks independently,
     * since the callbacks can be fired in any order.
     * <p>
     * As an example, if you want to update the map's camera using a {@link LatLngBounds} without dimensions, you should wait until both
     * {@code OnMapReadyCallback} and {@code OnGlobalLayoutListener} have completed. Otherwise there is a race condition that could trigger an
     * {@link IllegalStateException}.
     *
     * @param googleMap A non-null instance of a GoogleMap associated with the {@link MapFragment} or {@link MapView} that defines the callback.
     */
    void onMapReady(GoogleMap googleMap);
}
