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

/**
 * Get phone number operation described in GSMA Service Entitlement Configuration section 6.
 */
public final class GetPhoneNumberOperation {
    /**
     * Get phone number request described in GSMA Service Entitlement Configuration section 6.4.8.
     */
    @AutoValue
    public abstract static class GetPhoneNumberRequest {
        /**
         * Returns the terminal id.
         */
        @NonNull
        public abstract String terminalId();

        /** Returns a new {@link GetPhoneNumberRequest.Builder} object. */
        @NonNull
        public static Builder builder() {
            return new AutoValue_GetPhoneNumberOperation_GetPhoneNumberRequest
                .Builder()
                .setTerminalId("");
        }

        /** Builder. */
        @AutoValue.Builder
        public abstract static class Builder {
            /**
             * Sets the terminal id.
             *
             * @param terminalId The terminal id.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setTerminalId(@NonNull String terminalId);

            /** Returns the {@link GetPhoneNumberRequest} object. */
            @NonNull
            public abstract GetPhoneNumberRequest build();
        }
    }

    /**
     * Get phone number response described in GSMA Service Entitlement Configuration section
     * 6.5.8.
     */
    @AutoValue
    public abstract static class GetPhoneNumberResponse extends OdsaResponse {

        /** The phone number of the subscriber in E.164 format. */
        public abstract String msisdn();

        /** Returns a new {@link GetPhoneNumberResponse.Builder} object. */
        @NonNull
        public static Builder builder() {
            return new AutoValue_GetPhoneNumberOperation_GetPhoneNumberResponse
                    .Builder()
                    .setMsisdn("");
        }

        /** Builder. */
        @AutoValue.Builder
        public abstract static class Builder extends OdsaResponse.Builder {
            /**
             * Sets the phone number of the subscriber.
             *
             * @param msisdn The phone number of the subscriber in E.164 format.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setMsisdn(@NonNull String msisdn);

            /** Returns the {@link GetPhoneNumberResponse} object. */
            @NonNull
            public abstract GetPhoneNumberResponse build();
        }
    }

    private GetPhoneNumberOperation() {
    }
}
