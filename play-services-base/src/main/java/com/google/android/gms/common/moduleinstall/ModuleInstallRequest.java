/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.moduleinstall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.api.OptionalModuleApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Request object that is used to request installation of optional modules in
 * {@link ModuleInstallClient#installModules(ModuleInstallRequest)}.
 */
public class ModuleInstallRequest {
    private final @NonNull List<OptionalModuleApi> apis;
    private final @Nullable InstallStatusListener listener;
    private final @Nullable Executor listenerExecutor;

    private ModuleInstallRequest(@NonNull List<OptionalModuleApi> apis, @Nullable InstallStatusListener listener, @Nullable Executor listenerExecutor) {
        this.apis = apis;
        this.listener = listener;
        this.listenerExecutor = listenerExecutor;
    }

    /**
     * Returns the list of APIs that require optional modules.
     */
    public @NonNull List<OptionalModuleApi> getApis() {
        return apis;
    }

    /**
     * Returns the listener that is attached to this request.
     */
    public @Nullable InstallStatusListener getListener() {
        return listener;
    }

    /**
     * Returns the {@link Executor} to use for the {@link InstallStatusListener}.
     */
    public @Nullable Executor getListenerExecutor() {
        return listenerExecutor;
    }

    /**
     * Returns a new {@link Builder} for {@link ModuleInstallRequest}.
     */
    public static @NonNull Builder newBuilder() {
        return new Builder();
    }

    /**
     * The builder for creating an instance of {@link ModuleInstallRequest}.
     */
    public static class Builder {
        private final @NonNull List<OptionalModuleApi> apis = new ArrayList<>();
        private @Nullable InstallStatusListener listener;
        private @Nullable Executor listenerExecutor;

        /**
         * Adds an {@link OptionalModuleApi} so that the optional module required by this API can be installed.
         */
        public @NonNull Builder addApi(@NonNull OptionalModuleApi api) {
            this.apis.add(api);
            return this;
        }

        /**
         * Sets an {@link InstallStatusListener} to the {@link ModuleInstallRequest}.
         * <p>
         * The listener will be called on the main thread.
         *
         * @param listener The {@link InstallStatusListener} to receive {@link ModuleInstallStatusUpdate} to monitor the progress of optional module
         *                 installation progress.
         */
        public @NonNull Builder setListener(@NonNull InstallStatusListener listener) {
            return setListener(listener, null);
        }

        /**
         * Sets an {@link InstallStatusListener} to the {@link ModuleInstallRequest}.
         *
         * @param listener         The {@link InstallStatusListener} to receive {@link ModuleInstallStatusUpdate} to monitor the progress of optional module
         *                         installation progress.
         * @param listenerExecutor The {@link Executor} to use to call the listener.
         */
        public @NonNull Builder setListener(@NonNull InstallStatusListener listener, @Nullable Executor listenerExecutor) {
            this.listener = listener;
            this.listenerExecutor = listenerExecutor;
            return this;
        }

        /**
         * Returns a new {@link ModuleInstallRequest} object.
         */
        public @NonNull ModuleInstallRequest build() {
            return new ModuleInstallRequest(apis, listener, listenerExecutor);
        }
    }
}
