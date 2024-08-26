/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps;

/**
 * Callback interface for when the Street View panorama is ready to be used.
 * <p>
 * Once an instance of this interface is set on a {@link StreetViewPanoramaFragment} or {@link StreetViewPanoramaView} object, the
 * {@link #onStreetViewPanoramaReady(StreetViewPanorama)} method is triggered when the panorama is ready to be used and provides a non-null
 * instance of {@link StreetViewPanorama}.
 * <p>
 * If Google Play services is not installed on the device, the user is prompted to install it, and the
 * {@link #onStreetViewPanoramaReady(StreetViewPanorama)} method will only be triggered when the user has installed it and returned to the app.
 */
public interface OnStreetViewPanoramaReadyCallback {
    /**
     * Called when the Street View panorama is ready to be used.
     *
     * @param panorama A non-null instance of a StreetViewPanorama associated with the {@link StreetViewPanoramaFragment} or {@link StreetViewPanoramaView} that
     *                 defines the callback.
     */
    void onStreetViewPanoramaReady(StreetViewPanorama panorama);
}
