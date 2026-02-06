/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.libraries.entitlement.odsa;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.libraries.entitlement.EsimOdsaOperation.OdsaOperationResult;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.net.URL;

/** ODSA general response described in GSMA Service Entitlement Configuration section 6.5.1. */
public abstract class OdsaResponse {
    /** Operation result. */
    @OdsaOperationResult
    public abstract int operationResult();

    /**
     * The provided URL shall present a web view to user on the reason(s) why the authentication
     * failed.
     */
    @Nullable
    public abstract URL generalErrorUrl();

    /**
     * User data sent to the Service Provider when requesting the {@link #generalErrorUrl()} web
     * view. It should contain user-specific attributes to improve user experience.
     */
    @Nullable
    public abstract String generalErrorUserData();

    /**
     * User-specific content string to be shown to the user.
     */
    @Nullable
    public abstract String generalErrorText();

    /** Builder */
    public abstract static class Builder {
        /**
         * Set the operation result.
         *
         * @param operationResult The operation result.
         * @return The builder.
         */
        @NonNull
        @CanIgnoreReturnValue
        public abstract Builder setOperationResult(@OdsaOperationResult int operationResult);

        /**
         * Set the URL to the web view to user on the reason(s) why the authentication failed.
         *
         * @param url The provided URL shall present a web view to user on the reason(s) why the
         *            authentication failed.
         * @return The builder.
         */
        @NonNull
        @CanIgnoreReturnValue
        public abstract Builder setGeneralErrorUrl(@NonNull URL url);

        /**
         * Set the user data of {@link #generalErrorUrl()}.
         *
         * @param userData User data sent to the Service Provider when requesting the {@link
         *                 #generalErrorUrl()} web view. It should contain user-specific attributes
         *                 to improve user experience.
         * @return The builder.
         */
        @NonNull
        @CanIgnoreReturnValue
        public abstract Builder setGeneralErrorUserData(@NonNull String userData);

        /**
         * Set the user text of {@link #generalErrorText()}.
         *
         * @param text User-specific content string to be shown to the user.
         * @return The builder.
         */
        @NonNull
        @CanIgnoreReturnValue
        public abstract Builder setGeneralErrorText(@NonNull String text);
    }
}
