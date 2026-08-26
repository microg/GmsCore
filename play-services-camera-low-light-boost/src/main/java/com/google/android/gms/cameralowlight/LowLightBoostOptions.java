/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.cameralowlight;

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.microg.gms.common.PublicApi;

import java.util.Objects;

/**
 * Immutable options used when creating a Camera Low Light Boost session.
 */
@PublicApi
@RequiresApi(30)
public class LowLightBoostOptions {
    private final Surface target;
    private final String cameraId;
    private final int captureWidth;
    private final int captureHeight;
    private final boolean lowLightBoostEnabled;

    public LowLightBoostOptions(
            @NonNull Surface target,
            @NonNull String cameraId,
            int captureWidth,
            int captureHeight,
            boolean lowLightBoostEnabled
    ) {
        this.target = Objects.requireNonNull(target, "target");
        this.cameraId = Objects.requireNonNull(cameraId, "cameraId");
        this.captureWidth = captureWidth;
        this.captureHeight = captureHeight;
        this.lowLightBoostEnabled = lowLightBoostEnabled;
    }

    public LowLightBoostOptions(
            @NonNull Surface target,
            @NonNull String cameraId,
            int captureWidth,
            int captureHeight
    ) {
        this(target, cameraId, captureWidth, captureHeight, false);
    }

    @NonNull
    public final String getCameraId() {
        return cameraId;
    }

    public final int getCaptureHeight() {
        return captureHeight;
    }

    public final int getCaptureWidth() {
        return captureWidth;
    }

    @NonNull
    public final Surface getTarget() {
        return target;
    }

    public final boolean getEnableLowLightBoost() {
        return lowLightBoostEnabled;
    }
}
