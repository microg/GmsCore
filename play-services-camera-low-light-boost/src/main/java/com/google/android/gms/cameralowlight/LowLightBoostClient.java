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
import androidx.annotation.RequiresApi;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.common.api.OptionalModuleApi;
import com.google.android.gms.tasks.Task;

import org.microg.gms.common.PublicApi;

/**
 * Client for checking support and creating Camera Low Light Boost sessions.
 */
@PublicApi
@RequiresApi(30)
public interface LowLightBoostClient extends HasApiKey<Api.ApiOptions.NoOptions>, OptionalModuleApi {
    /**
     * Creates a low light boost session for the supplied options.
     */
    @NonNull
    Task<LowLightBoostSession> createSession(
            @NonNull LowLightBoostOptions options,
            @NonNull LowLightBoostCallback callback
    );

    /**
     * Returns whether a camera supports low light boost.
     */
    @NonNull
    Task<Boolean> isCameraSupported(@NonNull String cameraId);

    /**
     * Returns whether the current device can use Camera Low Light Boost.
     */
    @NonNull
    Task<Boolean> isDeviceSupported();

    /**
     * Returns whether the dynamically delivered implementation module is installed.
     */
    @NonNull
    Task<Boolean> isModuleInstalled();

    /**
     * Installs the dynamically delivered implementation module.
     */
    @NonNull
    Task<Boolean> installModule(@Nullable InstallStatusCallback callback);

    /**
     * Installs the dynamically delivered implementation module.
     */
    @NonNull
    default Task<Boolean> installModule() {
        return installModule(null);
    }

    /**
     * Releases the dynamically delivered implementation module.
     */
    @NonNull
    Task<Void> releaseModule();

    /**
     * Receives module installation lifecycle events.
     */
    interface InstallStatusCallback {
        void onError(@NonNull String errorMessage);

        void onCancelled();

        void onDownloadProgressUpdate(int progress);

        void onDownloadPending();

        void onDownloadStart();

        void onDownloadPaused();

        void onDownloadComplete();

        void onInstalled();
    }
}
