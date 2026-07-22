/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.moduleinstall;

import android.app.PendingIntent;
import androidx.annotation.NonNull;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.common.api.OptionalModuleApi;
import com.google.android.gms.tasks.Task;

/**
 * Interface for module install APIs.
 */
public interface ModuleInstallClient extends HasApiKey<Api.ApiOptions.NoOptions> {
    /**
     * Checks if the optional modules required by the {@link OptionalModuleApi} are already present on device.
     * <p>
     * This method is intended to be used in cases where you don't want to unconditionally trigger an immediate installation if
     * the modules aren't available already. If you need to trigger an immediate installation, use
     * {@link #installModules(ModuleInstallRequest)} instead.
     *
     * @param apis the {@link OptionalModuleApi}s that require optional modules.
     * @return a {@link Task} with value {@link ModuleAvailabilityResponse} indicating whether the requested modules are already present.
     */
    Task<ModuleAvailabilityResponse> areModulesAvailable(@NonNull OptionalModuleApi... apis);

    /**
     * Defers installation of optional modules required by the {@link OptionalModuleApi}. When called, Google Play services will
     * optimize the best time to install those modules in the background.
     * <p>
     * If your app requires immediate access to those modules, use {@link #installModules(ModuleInstallRequest)} to install the
     * optional modules right away.
     *
     * @param apis the {@link OptionalModuleApi}s that require optional modules.
     * @return a successful {@link Task} if the deferred install request is received.
     */
    Task<Void> deferredInstall(@NonNull OptionalModuleApi... apis);

    /**
     * Gets the {@link ModuleInstallIntentResponse} that includes a {@link PendingIntent} to initiate the optional module download
     * and installation flow.
     *
     * @param apis the {@link OptionalModuleApi}s that require optional modules.
     * @return a {@link Task} with value {@link ModuleInstallIntentResponse} which includes the {@link PendingIntent} that can be used to
     * launch the UI flow. A null {@link PendingIntent} indicates that the optional modules are already present on device.
     */
    Task<ModuleInstallIntentResponse> getInstallModulesIntent(@NonNull OptionalModuleApi... apis);

    /**
     * Triggers an immediate installation request from a {@link ModuleInstallRequest}.
     * <p>
     * The {@link Task} completes once the {@link ModuleInstallRequest} has been initiated. This method does not wait for installation to
     * complete. To monitor the install/download progress of the request, set a {@link InstallStatusListener} when building the
     * {@link ModuleInstallRequest} to receive {@link ModuleInstallStatusUpdate}, and make sure to
     * {@link #unregisterListener(InstallStatusListener)} once the installation completes. The listener is only registered if the
     * modules requested are not already installed.
     * <p>
     * The {@link ModuleInstallResponse} indicates whether the modules are already installed and contains an integer session id
     * that is corresponding to a unique install request. A session id of 0 and/or
     * {@link ModuleInstallResponse#areModulesAlreadyInstalled()} returned {@code true} indicate that the optional modules are
     * already installed. You don't need to interact with session id unless the same {@link InstallStatusListener} object is used in
     * multiple {@link ModuleInstallRequest}s.
     *
     * @param request the {@link ModuleInstallRequest} you build for the install request.
     * @return a {@link Task} with value {@link ModuleInstallResponse} if the install request is received.
     */
    Task<ModuleInstallResponse> installModules(@NonNull ModuleInstallRequest request);

    /**
     * Initiates a request to release optional modules required by {@link OptionalModuleApi} when they are no longer needed.
     * <p>
     * This method notifies Google Play services that the optional modules are no longer needed for this app, but it does not
     * guarantee the optional modules can be removed. Google Play services will try to clean up the optional modules when
     * they are not used by any apps.
     * <p>
     * If this method is called when an install request with all the specified modules is pending, this method will do the best-
     * effort to cancel that install request. You can monitor the {@link ModuleInstallStatusUpdate} through
     * {@link InstallStatusListener} to listen for the {@link ModuleInstallStatusUpdate.InstallState#STATE_CANCELED} state when
     * the install request is canceled.
     *
     * @param apis the {@link OptionalModuleApi}s that require optional modules.
     * @return a successful {@link Task} if the release modules request is received.
     */
    Task<Void> releaseModules(@NonNull OptionalModuleApi... apis);

    /**
     * Unregisters a listener you previously set in {@link ModuleInstallRequest}.
     *
     * @param listener the same {@link InstallStatusListener} that is set in the {@link ModuleInstallRequest}.
     * @return {@code true} if the given listener was found and unregistered, {@code false} otherwise.
     */
    Task<Boolean> unregisterListener(@NonNull InstallStatusListener listener);
}
