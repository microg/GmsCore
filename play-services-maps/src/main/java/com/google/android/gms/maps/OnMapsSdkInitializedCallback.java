/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps;

/**
 * Callback interface used by the Maps SDK to inform you which maps {@link MapsInitializer.Renderer} type has been loaded for your application.
 */
public interface OnMapsSdkInitializedCallback {
    /**
     * The Maps SDK calls this method to inform you which maps {@link MapsInitializer.Renderer} has been loaded for your application.
     * <p>
     * You can implement this method to define configurations or operations that are specific to each {@link MapsInitializer.Renderer} type.
     *
     * @param renderer The actual maps {@link MapsInitializer.Renderer} the maps SDK has loaded for your application.
     */
    void onMapsSdkInitialized(MapsInitializer.Renderer renderer);
}
