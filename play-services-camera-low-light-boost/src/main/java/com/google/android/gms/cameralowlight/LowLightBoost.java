/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.cameralowlight;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Low Light Boost API.
 * <p>
 * Low Light Boost automatically adjusts the camera surface brightness to adapt to low light scenes. The capability can apply
 * to the preview stream, still captures, and video recordings.
 * <p>
 * To use this API, you must get an instance of {@link com.google.android.gms.cameralowlight.LowLightBoostClient} and then
 * check that the device supports the feature.
 */
public class LowLightBoost {
    @NonNull
    public static final LowLightBoost INSTANCE = new LowLightBoost();

    private LowLightBoost() {
    }

    /**
     * Creates a new instance of {@link LowLightBoostClient}.
     *
     * @param context the context that is using this client
     */
    @NonNull
    @RequiresApi(30)
    public static LowLightBoostClient getClient(@NonNull Context context) {
        throw new UnsupportedOperationException();
    }
}
