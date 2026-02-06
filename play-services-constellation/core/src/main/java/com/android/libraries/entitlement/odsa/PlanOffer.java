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

/** Mobile plan described in GSMA Service Entitlement Configuration section 6.5.6 table 43. */
@AutoValue
public abstract class PlanOffer {
    /** ID for the plan offered by the MNO. */
    @NonNull
    public abstract String planId();

    /**
     * Name of the plan offered by the MNO. It is considered as an optional parameter due to it is
     * not required in any request, but it is recommended to make easier the plan identification.
     */
    @Nullable
    public abstract String planName();

    /**
     * Description of the plan offered by the MNO. It is considered as an optional parameter due to
     * it is not required in any request, but it is recommended to make easier the plan
     * identification.
     */
    @Nullable
    public abstract String planDescription();

    /** Returns the builder of {@link PlanOffer}. */
    public static Builder builder() {
        return new AutoValue_PlanOffer.Builder();
    }

    /** Builder of PlanOffer */
    @AutoValue.Builder
    public abstract static class Builder {
        /** Sets ID for the plan offered by the MNO. */
        @NonNull
        public abstract Builder setPlanId(@NonNull String planId);

        /**
         * Sets name of the plan offered by the MNO. It is considered as an optional parameter due
         * to it is not required in any request, but it is recommended to make easier the plan
         * identification.
         */
        @NonNull
        public abstract Builder setPlanName(@NonNull String planName);

        /**
         * Sets description of the plan offered by the MNO. It is considered as an optional
         * parameter due to it is not required in any request, but it is recommended to make easier
         * the plan identification.
         */
        @NonNull
        public abstract Builder setPlanDescription(@NonNull String planDescription);

        /** Build the {@link PlanOffer} object. */
        @NonNull
        public abstract PlanOffer build();
    }
}
