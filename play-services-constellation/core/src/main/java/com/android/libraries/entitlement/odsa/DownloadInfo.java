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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/**
 * Download information described in GSMA Service Entitlement Configuration section 6.5.3 table 38.
 */
@AutoValue
public abstract class DownloadInfo {
    /**
     * The ICCID of the eSIM profile to download from SM-DP+. This is not an empty string when
     * {@link #profileSmdpAddresses()} is used to trigger the profile download.
     */
    @NonNull
    public abstract String profileIccid();

    /**
     * Address(es) of SM-DP+ to obtain eSIM profile. It is an empty list if {@link
     * #profileActivationCode()} is not empty.
     */
    @NonNull
    public abstract ImmutableList<String> profileSmdpAddresses();

    /**
     * Activation code as defined in SGP.22 to permit the download of an eSIM profile from an
     * SM-DP+. It is an empty string if {@link #profileSmdpAddresses()} is not empty.
     */
    @NonNull
    public abstract String profileActivationCode();

    /** Returns builder of {@link DownloadInfo}. */
    @NonNull
    public static Builder builder() {
        return new AutoValue_DownloadInfo.Builder()
                .setProfileActivationCode("")
                .setProfileSmdpAddresses(ImmutableList.of())
                .setProfileIccid("");
    }

    /** Builder of DownloadInfo. */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * Set the ICCID of the download profile.
         *
         * @param iccid The ICCID of the eSIM profile to download from SM-DP+.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setProfileIccid(@NonNull String iccid);

        /**
         * Set the activation code.
         *
         * @param activationCode Activation code as defined in SGP.22 to permit the download of an
         *                       eSIM profile from an SM-DP+.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setProfileActivationCode(@NonNull String activationCode);

        /**
         * Set address(es) of SM-DP+ to obtain eSIM profile.
         *
         * @param smdpAddress Address(es) of SM-DP+ to obtain eSIM profile.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setProfileSmdpAddresses(@NonNull ImmutableList<String> smdpAddress);

        /** Build the DownloadInfo object. */
        @NonNull
        public abstract DownloadInfo build();
    }
}
