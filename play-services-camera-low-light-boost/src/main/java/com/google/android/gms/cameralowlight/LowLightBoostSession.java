/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.cameralowlight;

import android.hardware.camera2.TotalCaptureResult;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.microg.gms.common.PublicApi;

import java.util.concurrent.Executor;

/**
 * Active Camera Low Light Boost session.
 */
@PublicApi
@RequiresApi(30)
public interface LowLightBoostSession {
    /**
     * Returns the camera output surface that receives frames for this session.
     */
    @NonNull
    Surface getCameraSurface();

    /**
     * Supplies capture metadata associated with the frames sent to the session surface.
     */
    void processCaptureResult(@NonNull TotalCaptureResult captureResult);

    /**
     * Enables or disables low light boost.
     */
    void enableLowLightBoost(boolean enabled);

    /**
     * Returns whether low light boost is currently enabled for this session.
     */
    boolean isLowLightBoostEnabled();

    /**
     * Registers a callback that receives boost strength changes.
     */
    void setSceneDetectorCallback(@Nullable SceneDetectorCallback callback, @Nullable Executor executor);

    /**
     * Releases this session and its rendering resources.
     */
    void release();
}
