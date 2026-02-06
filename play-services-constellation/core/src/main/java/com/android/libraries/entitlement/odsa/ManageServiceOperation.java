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

import com.android.libraries.entitlement.EsimOdsaOperation;
import com.android.libraries.entitlement.EsimOdsaOperation.CompanionService;
import com.android.libraries.entitlement.EsimOdsaOperation.OdsaOperationType;
import com.android.libraries.entitlement.EsimOdsaOperation.OdsaServiceStatus;
import com.android.libraries.entitlement.utils.Ts43Constants;
import com.android.libraries.entitlement.utils.Ts43Constants.AppId;

import com.google.auto.value.AutoValue;

/** Manage service operation described in GSMA Service Entitlement Configuration section 6. */
public final class ManageServiceOperation {
    /**
     * HTTP request parameters specific to on device service activation (ODSA) manage service
     * request. See GSMA spec TS.43 section 6.2.
     */
    @AutoValue
    public abstract static class ManageServiceRequest {
        /**
         * Returns the application id. Can only be {@link Ts43Constants#APP_ODSA_COMPANION}, {@link
         * Ts43Constants#APP_ODSA_PRIMARY}, or
         * {@link Ts43Constants#APP_ODSA_SERVER_INITIATED_REQUESTS}.
         */
        @AppId
        public abstract String appId();

        /**
         * Returns the detailed type of the eSIM ODSA operation. Used by HTTP parameter {@code
         * operation_type}.
         */
        @OdsaOperationType
        public abstract int operationType();

        /**
         * Returns the unique identifier of the companion device, like IMEI. Used by HTTP parameter
         * {@code companion_terminal_id}.
         */
        @NonNull
        public abstract String companionTerminalId();

        /**
         * Returns the OEM of the companion device. Used by HTTP parameter {@code
         * companion_terminal_vendor}.
         */
        @NonNull
        public abstract String companionTerminalVendor();

        /**
         * Returns the model of the companion device. Used by HTTP parameter {@code
         * companion_terminal_model}.
         */
        @NonNull
        public abstract String companionTerminalModel();

        /**
         * Returns the software version of the companion device. Used by HTTP parameter {@code
         * companion_terminal_sw_version}.
         */
        @NonNull
        public abstract String companionTerminalSoftwareVersion();

        /**
         * Returns the user-friendly version of the companion device. Used by HTTP parameter {@code
         * companion_terminal_friendly_name}.
         */
        @NonNull
        public abstract String companionTerminalFriendlyName();

        /**
         * Returns the service type of the companion device, e.g. if the MSISDN is same as the
         * primary device. Used by HTTP parameter {@code companion_terminal_service}.
         */
        @NonNull
        @CompanionService
        public abstract String companionTerminalService();

        /**
         * Returns the ICCID of the companion device. Used by HTTP parameter {@code
         * companion_terminal_iccid}.
         */
        @NonNull
        public abstract String companionTerminalIccid();

        /** Returns a new {@link Builder} object. */
        @NonNull
        public static Builder builder() {
            return new AutoValue_ManageServiceOperation_ManageServiceRequest.Builder()
                    .setAppId(Ts43Constants.APP_UNKNOWN)
                    .setOperationType(EsimOdsaOperation.OPERATION_TYPE_NOT_SET)
                    .setCompanionTerminalId("")
                    .setCompanionTerminalVendor("")
                    .setCompanionTerminalModel("")
                    .setCompanionTerminalSoftwareVersion("")
                    .setCompanionTerminalFriendlyName("")
                    .setCompanionTerminalService(EsimOdsaOperation.COMPANION_SERVICE_UNKNOWN)
                    .setCompanionTerminalIccid("");
        }

        /** Builder */
        @AutoValue.Builder
        public abstract static class Builder {
            /**
             * Sets the application id.
             *
             * @param appId The application id. Can only be
             *              {@link Ts43Constants#APP_ODSA_COMPANION},
             *              {@link Ts43Constants#APP_ODSA_PRIMARY}, or {@link
             *              Ts43Constants#APP_ODSA_SERVER_INITIATED_REQUESTS}.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setAppId(@NonNull @AppId String appId);

            /**
             * Sets the detailed type of the eSIM ODSA operation.
             *
             * @param operationType Operation type. Only {@link
             *                      EsimOdsaOperation#OPERATION_TYPE_ACTIVATE_SERVICE} and {@link
             *                      EsimOdsaOperation#OPERATION_TYPE_DEACTIVATE_SERVICE} are
             *                      allowed.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setOperationType(@OdsaOperationType int operationType);

            /**
             * Sets the unique identifier of the companion device, like IMEI. Used by HTTP parameter
             * {@code companion_terminal_id} if set.
             *
             * @param companionTerminalId The unique identifier of the companion device.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setCompanionTerminalId(String companionTerminalId);

            /**
             * Sets the OEM of the companion device. Used by HTTP parameter {@code
             * companion_terminal_vendor} if set.
             *
             * @param companionTerminalVendor The OEM of the companion device.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setCompanionTerminalVendor(String companionTerminalVendor);

            /**
             * Sets the model of the companion device. Used by HTTP parameter {@code
             * companion_terminal_model} if set.
             *
             * @param companionTerminalModel The model of the companion device.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setCompanionTerminalModel(
                    @NonNull String companionTerminalModel);

            /**
             * Sets the software version of the companion device. Used by HTTP parameter {@code
             * companion_terminal_sw_version} if set.
             *
             * @param companionTerminalSoftwareVersion The software version of the companion device.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setCompanionTerminalSoftwareVersion(
                    @NonNull String companionTerminalSoftwareVersion);

            /**
             * Sets the user-friendly version of the companion device. Used by HTTP parameter {@code
             * companion_terminal_friendly_name} if set.
             *
             * <p>Used by companion device ODSA operation.
             *
             * @param companionTerminalFriendlyName The user-friendly version of the companion
             *                                      device.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setCompanionTerminalFriendlyName(
                    @NonNull String companionTerminalFriendlyName);

            /**
             * Sets the service type of the companion device, e.g. if the MSISDN is same as the
             * primary device. Used by HTTP parameter {@code companion_terminal_service} if set.
             *
             * <p>Used by companion device ODSA operation.
             *
             * @param companionTerminalService The service type of the companion device.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setCompanionTerminalService(
                    @NonNull @CompanionService String companionTerminalService);

            /**
             * Sets the ICCID of the companion device. Used by HTTP parameter {@code
             * companion_terminal_iccid} if set.
             *
             * <p>Used by companion device ODSA operation.
             *
             * @param companionTerminalIccid The ICCID of the companion device.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setCompanionTerminalIccid(
                    @NonNull String companionTerminalIccid);

            /** Build the {@link ManageServiceRequest} object. */
            @NonNull
            public abstract ManageServiceRequest build();
        }
    }

    /**
     * Manage service response described in GSMA Service Entitlement Configuration section 6.5.4
     * table 39.
     */
    @AutoValue
    public abstract static class ManageServiceResponse extends OdsaResponse {
        /** Service status. */
        @OdsaServiceStatus
        public abstract int serviceStatus();

        /** Returns a new {@link ManageServiceResponse.Builder} object. */
        @NonNull
        public static Builder builder() {
            return new AutoValue_ManageServiceOperation_ManageServiceResponse.Builder()
                    .setServiceStatus(EsimOdsaOperation.SERVICE_STATUS_UNKNOWN);
        }

        /** Builder */
        @AutoValue.Builder
        public abstract static class Builder extends OdsaResponse.Builder {
            /**
             * Set the service status.
             *
             * @param serviceStatus Service status
             * @return The builder.
             */
            @NonNull
            public abstract Builder setServiceStatus(@OdsaServiceStatus int serviceStatus);

            /** Build the {@link ManageServiceResponse} object. */
            @NonNull
            public abstract ManageServiceResponse build();
        }
    }

    private ManageServiceOperation() {
    }
}
