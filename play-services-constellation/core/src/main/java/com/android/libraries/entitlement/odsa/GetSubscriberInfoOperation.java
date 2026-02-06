/*
 * Copyright (C) 2025 The Android Open Source Project
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

/**
 * Get subscriber info operation.
 */
public final class GetSubscriberInfoOperation {
    /** Request for GetSubscriberInfo. */
    @AutoValue
    public abstract static class GetSubscriberInfoRequest {
        /** Optional requestor id. */
        @NonNull
        public abstract String requestorId();

        /** Returns a new {@link GetSubscriberInfoRequest.Builder} object. */
        @NonNull
        public static Builder builder() {
            return new AutoValue_GetSubscriberInfoOperation_GetSubscriberInfoRequest.Builder()
                    .setRequestorId("");
        }

        /** Builder. */
        @AutoValue.Builder
        public abstract static class Builder {
            /** Sets requestor id (optional). */
            @NonNull
            public abstract Builder setRequestorId(@NonNull String requestorId);

            /** Returns the {@link GetSubscriberInfoRequest}. */
            @NonNull
            public abstract GetSubscriberInfoRequest build();
        }
    }

    /** Response for GetSubscriberInfo. */
    @AutoValue
    public abstract static class GetSubscriberInfoResponse extends OdsaResponse {
        /** The phone number of the subscriber in E.164 format. */
        @NonNull
        public abstract String msisdn();

        /** SIM identifier type. */
        @NonNull
        public abstract String simIdType();

        /** SIM identifier value. */
        @NonNull
        public abstract String simId();

        /** Optional MVNO name. */
        @NonNull
        public abstract String mvnoName();

        /** Returns a new {@link GetSubscriberInfoResponse.Builder} object. */
        @NonNull
        public static Builder builder() {
            return new AutoValue_GetSubscriberInfoOperation_GetSubscriberInfoResponse.Builder()
                    .setMsisdn("")
                    .setSimIdType("")
                    .setSimId("")
                    .setMvnoName("");
        }

        /** Builder. */
        @AutoValue.Builder
        public abstract static class Builder extends OdsaResponse.Builder {
            @NonNull
            public abstract Builder setMsisdn(@NonNull String msisdn);

            @NonNull
            public abstract Builder setSimIdType(@NonNull String simIdType);

            @NonNull
            public abstract Builder setSimId(@NonNull String simId);

            @NonNull
            public abstract Builder setMvnoName(@NonNull String mvnoName);

            @NonNull
            public abstract GetSubscriberInfoResponse build();
        }
    }

    private GetSubscriberInfoOperation() {}
}

