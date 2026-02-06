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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.libraries.entitlement.EsimOdsaOperation;
import com.android.libraries.entitlement.EsimOdsaOperation.CompanionService;
import com.android.libraries.entitlement.EsimOdsaOperation.OdsaServiceStatus;
import com.android.libraries.entitlement.utils.Ts43Constants;
import com.android.libraries.entitlement.utils.Ts43Constants.AppId;
import com.android.libraries.entitlement.utils.Ts43Constants.NotificationAction;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Acquire configuration operation described in GSMA Service Entitlement Configuration section 6.
 */
public final class AcquireConfigurationOperation {
    /** Indicating polling interval not available. */
    public static final int POLLING_INTERVAL_NOT_AVAILABLE = -1;

    /**
     * HTTP request parameters specific to on device service activation (ODSA) acquire configuration
     * operation. See GSMA spec TS.43 section 6.2.
     */
    @AutoValue
    public abstract static class AcquireConfigurationRequest {
        /**
         * Returns the application id. Can only be {@link Ts43Constants#APP_ODSA_COMPANION},
         * {@link Ts43Constants#APP_ODSA_PRIMARY}, or
         * {@link Ts43Constants#APP_ODSA_SERVER_INITIATED_REQUESTS}.
         */
        @AppId
        public abstract String appId();

        /**
         * Returns the unique identifier of the companion device, like IMEI. Used by HTTP parameter
         * {@code companion_terminal_id}.
         */
        @NonNull
        public abstract String companionTerminalId();

        /**
         * Returns the ICCID of the companion device. Used by HTTP parameter
         * {@code companion_terminal_iccid}.
         */
        @NonNull
        public abstract String companionTerminalIccid();

        /**
         * Returns the EID of the companion device. Used by HTTP parameter
         * {@code companion_terminal_eid}.
         */
        @NonNull
        public abstract String companionTerminalEid();

        /**
         * Returns the ICCID of the primary device eSIM. Used by HTTP parameter
         * {@code terminal_iccid}.
         */
        @NonNull
        public abstract String terminalIccid();

        /**
         * Returns the eUICC identifier (EID) of the primary device eSIM. Used by HTTP parameter
         * {@code terminal_eid}.
         */
        @NonNull
        public abstract String terminalEid();

        /**
         * Returns the unique identifier of the primary device eSIM, like the IMEI associated with
         * the eSIM. Used by HTTP parameter {@code target_terminal_id}.
         */
        @NonNull
        public abstract String targetTerminalId();

        /**
         * Returns the ICCID primary device eSIM. Used by HTTP parameter
         * {@code target_terminal_iccid}.
         */
        @NonNull
        public abstract String targetTerminalIccid();

        /**
         * Returns the eUICC identifier (EID) of the primary device eSIM. Used by HTTP parameter
         * {@code target_terminal_eid}.
         */
        @NonNull
        public abstract String targetTerminalEid();

        /**
         * Returns the notification token used to register for entitlement configuration request
         * from network. Used by HTTP parameter {@code notif_token}.
         */
        @NonNull
        public abstract String notificationToken();

        /**
         * Returns the action associated with the notification token. Used by HTTP parameter
         * {@code notif_action}.
         */
        @NotificationAction
        public abstract int notificationAction();

        /** Returns a new {@link Builder} object. */
        @NonNull
        public static Builder builder() {
            return new AutoValue_AcquireConfigurationOperation_AcquireConfigurationRequest
                    .Builder()
                    .setCompanionTerminalId("")
                    .setCompanionTerminalIccid("")
                    .setCompanionTerminalEid("")
                    .setTerminalIccid("")
                    .setTerminalEid("")
                    .setTargetTerminalId("")
                    .setTargetTerminalIccid("")
                    .setTargetTerminalEid("")
                    .setNotificationToken("")
                    .setNotificationAction(Ts43Constants.NOTIFICATION_ACTION_ENABLE_FCM);
        }

        /** Builder */
        @AutoValue.Builder
        public abstract static class Builder {
            /**
             * Sets the application id.
             *
             * @param appId The application id. Can only be
             *              {@link Ts43Constants#APP_ODSA_COMPANION},
             *              {@link Ts43Constants#APP_ODSA_PRIMARY}, or
             *              {@link Ts43Constants#APP_ODSA_SERVER_INITIATED_REQUESTS}.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setAppId(@NonNull @AppId String appId);

            /**
             * Sets the unique identifier of the companion device, like IMEI. Used by HTTP parameter
             * {@code companion_terminal_id} if set.
             *
             * <p>Used by companion device ODSA operation.
             *
             * @param companionTerminalId The unique identifier of the companion device.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setCompanionTerminalId(@NonNull String companionTerminalId);

            /**
             * Sets the ICCID of the companion device. Used by HTTP parameter
             * {@code companion_terminal_iccid} if set.
             *
             * <p>Used by companion device ODSA operation.
             *
             * @param companionTerminalIccid The ICCID of the companion device.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setCompanionTerminalIccid(
                    @NonNull String companionTerminalIccid);

            /**
             * Sets the eUICC identifier (EID) of the companion device. Used by HTTP parameter
             * {@code companion_terminal_eid} if set.
             *
             * <p>Used by companion device ODSA operation.
             *
             * @param companionTerminalEid The eUICC identifier (EID) of the companion device.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setCompanionTerminalEid(@NonNull String companionTerminalEid);

            /**
             * Sets the ICCID of the primary device eSIM in case of primary SIM not present. Used by
             * HTTP parameter {@code terminal_eid} if set.
             *
             * <p>Used by primary device ODSA operation.
             *
             * @param terminalIccid The ICCID of the primary device eSIM in case of primary SIM not
             *                      present.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setTerminalIccid(@NonNull String terminalIccid);

            /**
             * Sets the eUICC identifier (EID) of the primary device eSIM in case of primary SIM not
             * present. Used by HTTP parameter {@code terminal_eid} if set.
             *
             * <p>Used by primary device ODSA operation.
             *
             * @param terminalEid The eUICC identifier (EID) of the primary device eSIM in case of
             *                    primary SIM not present.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setTerminalEid(@NonNull String terminalEid);

            /**
             * Sets the unique identifier of the primary device eSIM in case of multiple SIM, like
             * the IMEI associated with the eSIM. Used by HTTP parameter {@code target_terminal_id}
             * if set.
             *
             * <p>Used by primary device ODSA operation.
             *
             * @param targetTerminalId The unique identifier of the primary device eSIM in case of
             *                         multiple SIM.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setTargetTerminalId(@NonNull String targetTerminalId);

            /**
             * Sets the ICCID primary device eSIM in case of multiple SIM. Used by HTTP parameter
             * {@code target_terminal_iccid} if set.
             *
             * <p>Used by primary device ODSA operation.
             *
             * @param targetTerminalIccid The ICCID primary device eSIM in case of multiple SIM.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setTargetTerminalIccid(@NonNull String targetTerminalIccid);

            /**
             * Sets the eUICC identifier (EID) of the primary device eSIM in case of multiple SIM.
             * Used by HTTP parameter {@code target_terminal_eid} if set.
             *
             * <p>Used by primary device ODSA operation.
             *
             * @param targetTerminalEid The eUICC identifier (EID) of the primary device eSIM in
             *                          case of multiple SIM.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setTargetTerminalEid(@NonNull String targetTerminalEid);

            /**
             * Sets the notification token used to register for entitlement configuration request
             * from network. Used by HTTP parameter {@code notif_token} if set.
             *
             * <p>Used by primary device ODSA operation.
             *
             * @param notificationToken The notification token used to register for entitlement
             *                          configuration request from network.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setNotificationToken(@NonNull String notificationToken);

            /**
             * Sets the action associated with the notification token. Used by HTTP parameter
             * {@code notif_action} if set.
             *
             * <p>Used by primary device ODSA operation.
             *
             * @param notificationAction The action associated with the notification token.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setNotificationAction(
                    @NotificationAction int notificationAction);

            /** Returns build the {@link AcquireConfigurationRequest} object. */
            @NonNull
            public abstract AcquireConfigurationRequest build();
        }
    }

    /**
     * Acquire configuration response described in GSMA Service Entitlement Configuration section
     * section 6.5.5 table 40.
     */
    @AutoValue
    public abstract static class AcquireConfigurationResponse extends OdsaResponse {
        /** Configuration */
        @AutoValue
        public abstract static class Configuration {
            /** The configuration type is unknown. */
            public static final int CONFIGURATION_TYPE_UNKNOWN = -1;

            /** The configuration is for ODSA primary device. */
            public static final int CONFIGURATION_TYPE_PRIMARY = 1;

            /** The configuration is for companion device. */
            public static final int CONFIGURATION_TYPE_COMPANION = 2;

            /** The configuration is for server-initiated ODSA. */
            public static final int CONFIGURATION_TYPE_ENTERPRISE = 3;

            @Retention(RetentionPolicy.SOURCE)
            @IntDef({
                    CONFIGURATION_TYPE_UNKNOWN,
                    CONFIGURATION_TYPE_PRIMARY,
                    CONFIGURATION_TYPE_COMPANION,
                    CONFIGURATION_TYPE_ENTERPRISE
            })
            public @interface ConfigurationType {
            }

            /** Indicates the configuration type. */
            @ConfigurationType
            public abstract int type();

            /**
             * Integrated Circuit Card Identification - Identifier of the eSIM profile on the
             * device’s eSIM. {@code null} if an eSIM profile does not exist for the device.
             */
            @Nullable
            public abstract String iccid();

            /**
             * Indicates the applicable companion device service. {@code null} if not for companion
             * configuration.
             */
            @Nullable
            @CompanionService
            public abstract String companionDeviceService();

            /**
             * Service status.
             *
             * @see EsimOdsaOperation#SERVICE_STATUS_UNKNOWN
             * @see EsimOdsaOperation#SERVICE_STATUS_ACTIVATED
             * @see EsimOdsaOperation#SERVICE_STATUS_ACTIVATING
             * @see EsimOdsaOperation#SERVICE_STATUS_DEACTIVATED
             * @see EsimOdsaOperation#SERVICE_STATUS_DEACTIVATED_NO_REUSE
             */
            @OdsaServiceStatus
            public abstract int serviceStatus();

            /**
             * Specifies the minimum interval (in minutes) with which the device application may
             * poll the ECS to refresh the current {@link #serviceStatus()} using {@link
             * AcquireConfigurationRequest}. This parameter will be present only when {@link
             * #serviceStatus()} is {@link EsimOdsaOperation#SERVICE_STATUS_ACTIVATING}. If
             * parameter is not present or value is 0, this polling procedure is not triggered and
             * ODSA app will keep waiting for any external action to continue the flow.
             *
             * <p>The maximum number of {@link AcquireConfigurationRequest} before sending a {@link
             * #serviceStatus()} with {@link EsimOdsaOperation#SERVICE_STATUS_DEACTIVATED_NO_REUSE}
             * will be defined as an ECS configuration variable (MaxRefreshRequest).
             *
             * <p>{@link #POLLING_INTERVAL_NOT_AVAILABLE} when polling interval is not available.
             */
            public abstract int pollingInterval();

            /**
             * Specifies how and where to download the eSIM profile associated with the device.
             * Present in case the profile is to be downloaded at this stage.
             */
            @Nullable
            public abstract DownloadInfo downloadInfo();

            /** Includes all information collected by the ES of the companion device. */
            @Nullable
            public abstract CompanionDeviceInfo companionDeviceInfo();

            /**
             * Specifies how to communicate terms and conditions to the user or query information
             * from the user without a webview.
             */
            @Nullable
            public abstract MessageInfo messageInfo();

            /** Returns the builder. */
            @NonNull
            public static Builder builder() {
                return new AutoValue_AcquireConfigurationOperation_AcquireConfigurationResponse_Configuration
                        .Builder()
                        .setType(CONFIGURATION_TYPE_UNKNOWN)
                        .setIccid("")
                        .setServiceStatus(EsimOdsaOperation.SERVICE_STATUS_UNKNOWN)
                        .setPollingInterval(POLLING_INTERVAL_NOT_AVAILABLE);
            }

            /** The builder of {@link Configuration} */
            @AutoValue.Builder
            public abstract static class Builder {
                /**
                 * Set the configuration type.
                 *
                 * @param configType The configuration type.
                 * @return The builder.
                 */
                @NonNull
                public abstract Builder setType(@ConfigurationType int configType);

                /**
                 * Set the iccid.
                 *
                 * @param iccid Integrated Circuit Card Identification - Identifier of the eSIM
                 *              profile on the device’s eSIM.
                 * @return The builder.
                 */
                @NonNull
                public abstract Builder setIccid(@NonNull String iccid);

                /**
                 * Set the applicable companion device service.
                 *
                 * @param companionDeviceService Indicates the applicable companion device service.
                 * @return The builder.
                 */
                @NonNull
                public abstract Builder setCompanionDeviceService(
                        @NonNull @CompanionService String companionDeviceService);

                /**
                 * Set the service status.
                 *
                 * @param serviceStatus Service status.
                 * @return The builder.
                 */
                @NonNull
                public abstract Builder setServiceStatus(@OdsaServiceStatus int serviceStatus);

                /**
                 * Set the polling interval.
                 *
                 * @param pollingInterval The minimum interval (in minutes) with which the device
                 *                        application may poll the ECS to refresh the current
                 *                        {@link #serviceStatus()} using
                 *                        {@link AcquireConfigurationRequest}.
                 * @return The builder.
                 */
                @NonNull
                public abstract Builder setPollingInterval(int pollingInterval);

                /**
                 * Set the download information.
                 *
                 * @param downloadInfo Specifies how and where to download the eSIM profile
                 *                     associated with the device.
                 * @return The builder.
                 */
                @NonNull
                public abstract Builder setDownloadInfo(@NonNull DownloadInfo downloadInfo);

                /**
                 * Set the companion device info.
                 *
                 * @param companionDeviceInfo Includes all information collected by the ES of the
                 *                            companion device.
                 * @return The builder.
                 */
                @NonNull
                public abstract Builder setCompanionDeviceInfo(
                        @NonNull CompanionDeviceInfo companionDeviceInfo);

                /**
                 * Set the MSG information.
                 *
                 * @param messageInfo Specifies how to communicate terms and conditions to the user
                 *                    or query information from the user without a webview.
                 * @return The builder.
                 */
                @NonNull
                public abstract Builder setMessageInfo(@NonNull MessageInfo messageInfo);

                /** Returns build the {@link Configuration} object. */
                @NonNull
                public abstract Configuration build();
            }
        }

        /**
         * Configurations defined in GSMA Service Entitlement Configuration section 6.5.5. Could be
         * more than one if multiple companion device(s) associated with the requesting device that
         * carry a configuration for ODSA.
         */
        @NonNull
        public abstract ImmutableList<Configuration> configurations();

        /** Returns the builder. */
        @NonNull
        public static Builder builder() {
            return new AutoValue_AcquireConfigurationOperation_AcquireConfigurationResponse
                    .Builder()
                    .setConfigurations(ImmutableList.of());
        }

        /** The builder of {@link AcquireConfigurationResponse} */
        @AutoValue.Builder
        public abstract static class Builder extends OdsaResponse.Builder {
            /**
             * Set the configurations
             *
             * @param configs Configurations defined in GSMA Service Entitlement Configuration
             *                section 6.5.5. Could be more than one if multiple companion device(s)
             *                associated with the requesting device that carry a configuration for
             *                ODSA.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setConfigurations(
                    @NonNull ImmutableList<Configuration> configs);

            /** Returns build the {@link AcquireConfigurationResponse} object. */
            @NonNull
            public abstract AcquireConfigurationResponse build();
        }
    }

    private AcquireConfigurationOperation() {
    }
}
