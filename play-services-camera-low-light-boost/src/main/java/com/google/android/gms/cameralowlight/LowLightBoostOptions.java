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

import org.microg.gms.common.PublicApi;

import java.util.Objects;

/**
 * Properties of a {@link LowLightBoostSession}.
 */
public class LowLightBoostOptions {
    @NonNull
    private final Surface target;
    @NonNull
    private final String cameraId;
    private final int captureWidth;
    private final int captureHeight;
    private final boolean enableLowLightBoost;

    public LowLightBoostOptions(@NonNull Surface target, @NonNull String cameraId, int captureWidth, int captureHeight, boolean enableLowLightBoost) {
        this.target = Objects.requireNonNull(target, "target");
        this.cameraId = Objects.requireNonNull(cameraId, "cameraId");
        this.captureWidth = captureWidth;
        this.captureHeight = captureHeight;
        this.enableLowLightBoost = enableLowLightBoost;
    }

    public LowLightBoostOptions(@NonNull Surface target, @NonNull String cameraId, int captureWidth, int captureHeight) {
        this(target, cameraId, captureWidth, captureHeight, false);
    }

    /**
     * Camera ID of the camera to use.
     */
    @NonNull
    public final String getCameraId() {
        return cameraId;
    }

    /**
     * Height of the render target.
     */
    public final int getCaptureHeight() {
        return captureHeight;
    }

    /**
     * Width of the render target.
     */
    public final int getCaptureWidth() {
        return captureWidth;
    }

    /**
     * Destination surface of the brightened preview.
     */
    @NonNull
    public final Surface getTarget() {
        return target;
    }

    /**
     * Initial boost mode of the session.
     */
    public final boolean getEnableLowLightBoost() {
        return enableLowLightBoost;
    }
}
