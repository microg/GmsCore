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
 * A low light boost session.
 * <p>
 * Manages the necessary surfaces to provide to the camera capture session and outputs the brightened preview to a
 * {@link Surface} provided by the app.
 */
@RequiresApi(30)
public interface LowLightBoostSession {
    /**
     * Enables automatic preview brightening.
     * <p>
     * Enables low light boost to automatically vary the level of brightening applied based on what's appropriate for the scene's
     * estimated luminance. Disabling low light boost will disable preview brightening.
     *
     * @param enable True to enable low boost and False to disable.
     */
    void enableLowLightBoost(boolean enable);

    /**
     * Returns the camera surface.
     *
     * @return The camera surface with the requested dimensions. To be used as a capture target.
     */
    @NonNull
    Surface getCameraSurface();

    /**
     * Returns whether low light boost is enabled.
     */
    boolean isLowLightBoostEnabled();

    /**
     * Provides the latest capture result to the render service.
     * <p>
     * This must be called for every call to {@link android.hardware.camera2.CameraCaptureSession.CaptureCallback#onCaptureCompleted} in order for automatic
     * adjustment of preview brightening to function.
     *
     * @param captureResult The latest capture result.
     */
    void processCaptureResult(@NonNull TotalCaptureResult captureResult);

    /**
     * Releases this session, freeing up resources.
     * <p>
     * The session should no longer be used once released.
     */
    void release();

    /**
     * Sets the callback for scene detector.
     *
     * @param callback The callback for scene detector.
     * @param executor The executor to run the callback. If not provided, the callback will be run on the main thread.
     */
    void setSceneDetectorCallback(@Nullable SceneDetectorCallback callback, @Nullable Executor executor);
}
