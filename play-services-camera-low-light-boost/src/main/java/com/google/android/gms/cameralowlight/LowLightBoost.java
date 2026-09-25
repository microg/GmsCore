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

import org.microg.gms.common.PublicApi;

/**
 * Entry point for the Camera Low Light Boost APIs.
 */
@PublicApi
@RequiresApi(30)
public class LowLightBoost {
    public static final LowLightBoost INSTANCE = new LowLightBoost();

    private LowLightBoost() {
    }

    /**
     * Creates a client scoped to a context.
     */
    @NonNull
    public static LowLightBoostClient getClient(@NonNull Context context) {
        throw new UnsupportedOperationException();
    }
}
