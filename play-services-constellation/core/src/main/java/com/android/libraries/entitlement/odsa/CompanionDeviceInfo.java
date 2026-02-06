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

import com.google.auto.value.AutoValue;

/**
 * Companion device info described in GSMA Service Entitlement Configuration section 6.5.5 table 41.
 */
@AutoValue
public abstract class CompanionDeviceInfo {
    /**
     * User friendly identification for the companion device which can be used by the Service
     * Provider in Web Views.
     */
    @NonNull
    public abstract String companionTerminalFriendlyName();

    /** Manufacturer of the companion device. */
    @NonNull
    public abstract String companionTerminalVendor();

    /** Model of the companion device. */
    @Nullable
    public abstract String companionTerminalModel();

    /** eUICC identifier (EID) of the companion device being managed. */
    @Nullable
    public abstract String companionTerminalEid();

    /** Returns the builder of {@link CompanionDeviceInfo}. */
    @NonNull
    public static Builder builder() {
        return new AutoValue_CompanionDeviceInfo.Builder();
    }

    /** The builder. */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * Set user friendly identification for the companion device.
         *
         * @param companionTerminalFriendlyName User friendly identification for the companion
         *                                      device which can be used by the Service Provider in
         *                                      Web Views.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setCompanionTerminalFriendlyName(
                @NonNull String companionTerminalFriendlyName);

        /**
         * Set manufacturer of the companion device.
         *
         * @param companionTerminalVendor manufacturer of the companion device.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setCompanionTerminalVendor(@NonNull String companionTerminalVendor);

        /**
         * Set model of the companion device.
         *
         * @param companionTerminalModel Model of the companion device.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setCompanionTerminalModel(@NonNull String companionTerminalModel);

        /**
         * Set EID of the companion device.
         *
         * @param companionTerminalEid eUICC identifier (EID) of the companion device being managed.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setCompanionTerminalEid(@NonNull String companionTerminalEid);

        /** Build the CompanionDeviceInfo object. */
        @NonNull
        public abstract CompanionDeviceInfo build();
    }
}
