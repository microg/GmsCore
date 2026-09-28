/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.cameralowlight;

import androidx.annotation.NonNull;

import org.microg.gms.common.PublicApi;

/**
 * Handles callbacks notifying changes in scene lighting conditions.
 */
public interface SceneDetectorCallback {
    /**
     * Called when the scene brightness changes.
     *
     * @param session       the session associated with the brightness change.
     * @param boostStrength the current boost value, in the range 0.0, 1.0. When the value exceeds 0.5, the scene is considered low light.
     */
    void onSceneBrightnessChanged(@NonNull LowLightBoostSession session, float boostStrength);
}
