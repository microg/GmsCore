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
 * Receives changes to the recommended low light boost strength.
 */
@PublicApi
public interface SceneDetectorCallback {
    void onSceneBrightnessChanged(@NonNull LowLightBoostSession session, float boostStrength);
}
