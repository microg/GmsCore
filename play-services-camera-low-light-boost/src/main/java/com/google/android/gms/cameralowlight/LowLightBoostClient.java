/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.cameralowlight;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.common.api.OptionalModuleApi;
import com.google.android.gms.tasks.Task;

import org.microg.gms.common.PublicApi;

/**
 * The Low Light Boost client is used to provide availability checks and module installation. The client also provides a way to
 * create a {@link LowLightBoostSession}.
 */
@PublicApi
public interface LowLightBoostClient extends HasApiKey<Api.ApiOptions.NoOptions>, OptionalModuleApi {
    /**
     * Creates a {@link LowLightBoostSession}.
     * <p>
     * A low light boost session manages the necessary camera capture targets for a low light capture session and provides
     * rendering to a target surface, which the app can then display and encode as video or as a still image.
     *
     * @param options  The options for the session.
     * @param callback An implementation of {@link LowLightBoostCallback}.
     * @return A task that resolves to the created {@link LowLightBoostSession} or null if the session could not be created.
     */
    @NonNull
    Task<LowLightBoostSession> createSession(@NonNull LowLightBoostOptions options, @NonNull LowLightBoostCallback callback);

    /**
     * Installs the {@link LowLightBoost} module.
     *
     * @return A task that resolves to true if the module is installed successfully.
     */
    @NonNull
    default Task<Boolean> installModule() {
        return installModule(null);
    }

    /**
     * Installs the {@link LowLightBoost} module.
     *
     * @param callback A callback to receive status updates.
     * @return A task that resolves to true if the module is installed successfully.
     */
    @NonNull
    Task<Boolean> installModule(@Nullable InstallStatusCallback callback);

    /**
     * Queries whether a camera supports low light boost.
     *
     * @param cameraId The camera id to query.
     * @return A task that resolves to true if the camera supports low light boost, false otherwise.
     */
    @NonNull
    Task<Boolean> isCameraSupported(@NonNull String cameraId);

    /**
     * Checks if the module is supported on this device.
     * <p>
     * Only devices which support low light boost will be able to create a {@link LowLightBoostSession}.
     *
     * @return A task that resolves to true if the device is supported, false otherwise.
     */
    @NonNull
    Task<Boolean> isDeviceSupported();

    /**
     * Checks if the module is already installed.
     *
     * @return A task that resolves to true if the module is installed, false otherwise.
     */
    @NonNull
    Task<Boolean> isModuleInstalled();

    /**
     * Marks the {@link LowLightBoost} module as no longer needed for this application. The module will be released at some point in
     * the future.
     */
    @NonNull
    Task<Void> releaseModule();

    /**
     * A callback for the module installation status.
     */
    interface InstallStatusCallback {
        /**
         * Called when the installation of the module is cancelled.
         */
        void onCancelled();

        /**
         * Called when the download of the module has completed.
         */
        void onDownloadComplete();

        /**
         * Called when the download of the module has paused.
         */
        void onDownloadPaused();

        /**
         * Called when the download of the module is pending.
         */
        void onDownloadPending();

        /**
         * Called while the module is being downloaded with the current download progress.
         *
         * @param progress The current download progress, in the range 0, 100.
         */
        void onDownloadProgressUpdate(int progress);

        /**
         * Called when the download of the module has started.
         */
        void onDownloadStart();

        /**
         * Called when an error occurs during the installation of the module.
         *
         * @param description A description of the error.
         */
        void onError(@NonNull String description);

        /**
         * Called when the module has finished installing.
         */
        void onInstalled();
    }
}
