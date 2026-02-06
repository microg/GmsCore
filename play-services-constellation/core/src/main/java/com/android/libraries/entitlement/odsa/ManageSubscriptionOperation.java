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
import com.android.libraries.entitlement.EsimOdsaOperation.MessageButton;
import com.android.libraries.entitlement.EsimOdsaOperation.OdsaOperationType;
import com.android.libraries.entitlement.utils.HttpConstants;
import com.android.libraries.entitlement.utils.HttpConstants.ContentType;
import com.android.libraries.entitlement.utils.Ts43Constants;
import com.android.libraries.entitlement.utils.Ts43Constants.AppId;
import com.android.libraries.entitlement.utils.Ts43Constants.NotificationAction;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;

/**
 * Manage subscription operation described in GSMA Service Entitlement Configuration section 6.5.3.
 */
public final class ManageSubscriptionOperation {
    /**
     * HTTP request parameters specific to on device service activation (ODSA) manage subscription
     * request. See GSMA spec TS.43 section 6.2.
     */
    @AutoValue
    public abstract static class ManageSubscriptionRequest {
        /**
         * Returns the application id. Can only be {@link Ts43Constants#APP_ODSA_COMPANION},
         * {@link Ts43Constants#APP_ODSA_PRIMARY}, or
         * {@link Ts43Constants#APP_ODSA_SERVER_INITIATED_REQUESTS}.
         */
        @NonNull
        @AppId
        public abstract String appId();

        /**
         * Returns the detailed type of the eSIM ODSA operation. Used by HTTP parameter
         * {@code operation_type}.
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
         * Returns the OEM of the companion device. Used by HTTP parameter
         * {@code companion_terminal_vendor}.
         */
        @NonNull
        public abstract String companionTerminalVendor();

        /**
         * Returns the model of the companion device. Used by HTTP parameter
         * {@code companion_terminal_model}.
         */
        @NonNull
        public abstract String companionTerminalModel();

        /**
         * Returns the software version of the companion device. Used by HTTP parameter
         * {@code companion_terminal_sw_version}.
         */
        @NonNull
        public abstract String companionTerminalSoftwareVersion();

        /**
         * Returns the user-friendly version of the companion device. Used by HTTP parameter
         * {@code companion_terminal_friendly_name}.
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
         * Returns the unique identifiers of the primary device eSIM if more than one, like the
         * IMEIs on dual-SIM devices. Used by HTTP parameter {@code target_terminal_imeis}.
         *
         * <p>This is a non-standard params required by some carriers.
         */
        @NonNull
        public abstract ImmutableList<String> targetTerminalIds();

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
         * Returns the serial number of primary device. Used by HTTP parameter
         * {@code target_terminal_sn}.
         *
         * <p>This is a non-standard params required by some carriers.
         */
        @NonNull
        public abstract String targetTerminalSerialNumber();

        /**
         * Returns the model of primary device. Used by HTTP parameter
         * {@code target_terminal_model}.
         *
         * <p>This is a non-standard params required by some carriers.
         */
        @NonNull
        public abstract String targetTerminalModel();

        /**
         * Returns the unique identifier of the old device eSIM, like the IMEI associated with the
         * eSIM. Used by HTTP parameter {@code old_terminal_id}.
         */
        @NonNull
        public abstract String oldTerminalId();

        /**
         * Returns the ICCID of old device eSIM. Used by HTTP parameter {@code old_terminal_iccid}.
         */
        @NonNull
        public abstract String oldTerminalIccid();

        /**
         * Returns the identifier of the specific plan offered by an MNO. Used by HTTP parameter
         * {@code plan_id}.
         */
        @NonNull
        public abstract String planId();

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

        /**
         * Returns the user response to the MSG content.
         * Used by HTTP parameter {@code MSG_response}.
         */
        @NonNull
        public abstract String messageResponse();

        /**
         * Returns whether the user has accepted or rejected the MSG content.
         * Used by HTTP parameter {@code MSG_btn}.
         */
        @NonNull
        @MessageButton
        public abstract String messageButton();

        /** Returns a new {@link Builder} object. */
        @NonNull
        public static Builder builder() {
            return new AutoValue_ManageSubscriptionOperation_ManageSubscriptionRequest.Builder()
                    .setAppId(Ts43Constants.APP_UNKNOWN)
                    .setOperationType(EsimOdsaOperation.OPERATION_TYPE_NOT_SET)
                    .setCompanionTerminalId("")
                    .setCompanionTerminalVendor("")
                    .setCompanionTerminalModel("")
                    .setCompanionTerminalSoftwareVersion("")
                    .setCompanionTerminalFriendlyName("")
                    .setCompanionTerminalService(EsimOdsaOperation.COMPANION_SERVICE_UNKNOWN)
                    .setCompanionTerminalIccid("")
                    .setCompanionTerminalEid("")
                    .setTerminalIccid("")
                    .setTerminalEid("")
                    .setTargetTerminalId("")
                    .setTargetTerminalIds(ImmutableList.of())
                    .setTargetTerminalIccid("")
                    .setTargetTerminalEid("")
                    .setTargetTerminalSerialNumber("")
                    .setTargetTerminalModel("")
                    .setOldTerminalId("")
                    .setOldTerminalIccid("")
                    .setPlanId("")
                    .setNotificationToken("")
                    .setNotificationAction(Ts43Constants.NOTIFICATION_ACTION_ENABLE_FCM)
                    .setMessageResponse("")
                    .setMessageButton("");
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
             * Sets the detailed type of the eSIM ODSA operation. Used by HTTP parameter
             * {@code operation_type} if set.
             *
             * @param operationType The detailed type of the eSIM ODSA operation.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setOperationType(@OdsaOperationType int operationType);

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
            public abstract Builder setCompanionTerminalId(String companionTerminalId);

            /**
             * Sets the OEM of the companion device. Used by HTTP parameter
             * {@code companion_terminal_vendor} if set.
             *
             * <p>Used by companion device ODSA operation.
             *
             * @param companionTerminalVendor The OEM of the companion device.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setCompanionTerminalVendor(
                    @NonNull String companionTerminalVendor);

            /**
             * Sets the model of the companion device. Used by HTTP parameter
             * {@code companion_terminal_model} if set.
             *
             * <p>Used by companion device ODSA operation.
             *
             * @param companionTerminalModel The model of the companion device.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setCompanionTerminalModel(
                    @NonNull String companionTerminalModel);

            /**
             * Sets the software version of the companion device. Used by HTTP parameter
             * {@code companion_terminal_sw_version} if set.
             *
             * <p>Used by companion device ODSA operation.
             *
             * @param companionTerminalSoftwareVersion The software version of the companion device.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setCompanionTerminalSoftwareVersion(
                    @NonNull String companionTerminalSoftwareVersion);

            /**
             * Sets the user-friendly version of the companion device. Used by HTTP parameter
             * {@code companion_terminal_friendly_name} if set.
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
             * Sets the unique identifiers of the primary device eSIM if more than one, like the
             * IMEIs on dual-SIM devices. Used by HTTP parameter {@code target_terminal_imeis}
             * if set.
             *
             * <p>This is a non-standard params required by some carriers.
             *
             * @param targetTerminalIds The unique identifiers of the primary device eSIM if more
             *                          than one.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setTargetTerminalIds(
                    @NonNull ImmutableList<String> targetTerminalIds);

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
             * Sets the serial number of primary device. Used by HTTP parameter
             * {@code target_terminal_sn} if set.
             *
             * <p>Used by primary device ODSA operation.
             *
             * @param targetTerminalSerialNumber The serial number of primary device. This is a
             *                                   non-standard params required by some carriers.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setTargetTerminalSerialNumber(
                    @NonNull String targetTerminalSerialNumber);

            /**
             * Sets the model of primary device. Used by HTTP parameter
             * {@code target_terminal_model} if set.
             *
             * <p>Used by primary device ODSA operation.
             *
             * @param targetTerminalModel The model of primary device. This is a non-standard params
             *                            required by some carriers.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setTargetTerminalModel(@NonNull String targetTerminalModel);

            /**
             * Sets the unique identifier of the old device eSIM, like the IMEI associated with the
             * eSIM.Used by HTTP parameter {@code old_terminal_id} if set.
             *
             * <p>Used by primary device ODSA operation.
             *
             * @param oldTerminalId The unique identifier of the old device eSIM.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setOldTerminalId(@NonNull String oldTerminalId);

            /**
             * Sets the ICCID old device eSIM. Used by HTTP parameter {@code old_terminal_iccid}
             * if set.
             *
             * <p>Used by primary device ODSA operation.
             *
             * @param oldTerminalIccid The ICCID old device eSIM.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setOldTerminalIccid(@NonNull String oldTerminalIccid);

            /**
             * Sets the identifier of the specific plan offered by an MNO. Used by HTTP parameter
             * {@code plan_id} if set.
             *
             * <p>Used by primary device ODSA operation.
             *
             * @param planId The identifier of the specific plan offered by an MNO.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setPlanId(@NonNull String planId);

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

            /**
             * Sets the user response to the MSG content. Used by HTTP parameter
             * {@code MSG_response} if set.
             *
             * <p>Used by primary device ODSA operation.
             *
             * @param messageResponse The response entered by the user on the device UI.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setMessageResponse(@NonNull String messageResponse);

            /**
             * Sets whether the user has accepted or rejected the MSG content. Used by HTTP
             * parameter {@code MSG_btn} if set.
             *
             * <p>Used by primary device ODSA operation.
             *
             * @param messageButton Whether the user has accepted or rejected the MSG content.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setMessageButton(@NonNull String messageButton);

            /** Returns the {@link ManageSubscriptionRequest} object. */
            @NonNull
            public abstract ManageSubscriptionRequest build();
        }
    }

    /**
     * Manage subscription response described in GSMA Service Entitlement Configuration section
     * 6.5.3 table 37.
     */
    @AutoValue
    public abstract static class ManageSubscriptionResponse extends OdsaResponse {
        /** Subscription result unknown. */
        public static final int SUBSCRIPTION_RESULT_UNKNOWN = -1;

        /**
         * Indicates that end-user must go through the subscription web view procedure, using
         * information included below.
         */
        public static final int SUBSCRIPTION_RESULT_CONTINUE_TO_WEBSHEET = 1;

        /**
         * Indicates that a eSIM profile must be downloaded by the device, with further information
         * included in response.
         */
        public static final int SUBSCRIPTION_RESULT_DOWNLOAD_PROFILE = 2;

        /**
         * Indicates that subscription flow has ended and the end-user has already downloaded the
         * eSIM profile so there is no need to perform any other action.
         */
        public static final int SUBSCRIPTION_RESULT_DONE = 3;

        /**
         * Indicates that an eSIM profile is not ready to be downloaded when a user requests to
         * transfer subscription or to add the new subscription through native UX on the eSIM
         * device.
         */
        public static final int SUBSCRIPTION_RESULT_DELAYED_DOWNLOAD = 4;

        /**
         * Indicates that subscription flow has ended without completing the ODSA procedure. An eSIM
         * profile is not available.
         */
        public static final int SUBSCRIPTION_RESULT_DISMISS = 5;

        /**
         * Indicates that the profile in use needs to be deleted to complete the subscription
         * transfer.
         */
        public static final int SUBSCRIPTION_RESULT_DELETE_PROFILE_IN_USE = 6;

        /**
         * Indicates that implementing redownloadable profile is mandatory. If device is not able to
         * support this, it should end the process.
         * This parameter only applies when peration_type is
         * {@link EsimOdsaOperation#OPERATION_TYPE_TRANSFER_SUBSCRIPTION}.
         */
        public static final int SUBSCRIPTION_RESULT_REDOWNLOADABLE_PROFILE_IS_MANDATORY = 7;

        /**
         * Indicates that user input without a webview is required in order to complete the
         * operation_type requested with the information submitted to the ECS.
         */
        public static final int SUBSCRIPTION_RESULT_REQUIRES_USER_INPUT = 8;

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({
                SUBSCRIPTION_RESULT_UNKNOWN,
                SUBSCRIPTION_RESULT_CONTINUE_TO_WEBSHEET,
                SUBSCRIPTION_RESULT_DOWNLOAD_PROFILE,
                SUBSCRIPTION_RESULT_DONE,
                SUBSCRIPTION_RESULT_DELAYED_DOWNLOAD,
                SUBSCRIPTION_RESULT_DISMISS,
                SUBSCRIPTION_RESULT_DELETE_PROFILE_IN_USE,
                SUBSCRIPTION_RESULT_REDOWNLOADABLE_PROFILE_IS_MANDATORY,
                SUBSCRIPTION_RESULT_REQUIRES_USER_INPUT
        })
        public @interface SubscriptionResult {
        }

        /** The subscription result. */
        @SubscriptionResult
        public abstract int subscriptionResult();

        /**
         * URL refers to web views responsible for a certain action on the eSIM device subscription.
         * The
         * Service Provider can provide different URL based on the operation_type input parameter
         * ({@link EsimOdsaOperation#OPERATION_TYPE_SUBSCRIBE}, {@link
         * EsimOdsaOperation#OPERATION_TYPE_UNSUBSCRIBE}, {@link
         * EsimOdsaOperation#OPERATION_TYPE_CHANGE_SUBSCRIPTION}).
         *
         * <p>{@code null} if {@link #subscriptionResult()} is not {@link
         * #SUBSCRIPTION_RESULT_CONTINUE_TO_WEBSHEET}.
         */
        @Nullable
        public abstract URL subscriptionServiceUrl();

        /**
         * User data sent to the Service Provider when requesting the
         * {@link #subscriptionServiceUrl()}
         * web view. It should contain user-specific attributes to improve user experience.
         *
         * <p>{@code null} if {@link #subscriptionResult()} is not {@link
         * #SUBSCRIPTION_RESULT_CONTINUE_TO_WEBSHEET}.
         */
        @Nullable
        public abstract String subscriptionServiceUserData();

        /**
         * Specifies content and HTTP method to use when reaching out to the web server specified by
         * {@link #subscriptionServiceUrl()}.
         */
        @ContentType
        public abstract int subscriptionServiceContentsType();

        /**
         * Specifies how and where to download the eSIM profile associated with the companion or
         * primary device.
         *
         * <p>{@code null} if {@link #subscriptionResult()} is not {@link
         * #SUBSCRIPTION_RESULT_DOWNLOAD_PROFILE}.
         */
        @Nullable
        public abstract DownloadInfo downloadInfo();

        /** Returns the builder. */
        @NonNull
        public static Builder builder() {
            return new AutoValue_ManageSubscriptionOperation_ManageSubscriptionResponse.Builder()
                    .setSubscriptionResult(SUBSCRIPTION_RESULT_UNKNOWN)
                    .setSubscriptionServiceContentsType(HttpConstants.UNKNOWN);
        }

        /** Builder */
        @AutoValue.Builder
        public abstract static class Builder extends OdsaResponse.Builder {
            /**
             * Set subscription result.
             *
             * @param subscriptionResult The subscription result.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setSubscriptionResult(
                    @SubscriptionResult int subscriptionResult);

            /**
             * Set the URL refers to web views responsible for a certain action on the eSIM device
             * subscription.
             *
             * @param url URL refers to web views responsible for a certain action on the eSIM
             *            device subscription. The Service Provider can provide different URL based
             *            on the operation_type input parameter (
             *            {@link EsimOdsaOperation#OPERATION_TYPE_SUBSCRIBE}, {@link
             *            EsimOdsaOperation#OPERATION_TYPE_UNSUBSCRIBE}, {@link
             *            EsimOdsaOperation#OPERATION_TYPE_CHANGE_SUBSCRIPTION}).
             * @return The builder.
             */
            @NonNull
            public abstract Builder setSubscriptionServiceUrl(@NonNull URL url);

            /**
             * Set user data sent to the Service Provider.
             *
             * @param userData User data sent to the Service Provider when requesting the {@link
             *                 #subscriptionServiceUrl()} web view. It should contain user-specific
             *                 attributes to improve user experience.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setSubscriptionServiceUserData(@NonNull String userData);

            /**
             * Set the content type.
             *
             * @param contentType Specifies content and HTTP method to use when reaching out to the
             *                    web server specified by {@link #subscriptionServiceUrl()}.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setSubscriptionServiceContentsType(
                    @ContentType int contentType);

            /**
             * Set download information of eSIM profile associated with the companion or primary
             * device.
             *
             * @param downloadInfo Specifies how and where to download the eSIM profile associated
             *                     with the companion or primary device.
             * @return The builder.
             */
            @NonNull
            public abstract Builder setDownloadInfo(@NonNull DownloadInfo downloadInfo);

            /** Returns the {@link ManageSubscriptionResponse} object. */
            @NonNull
            public abstract ManageSubscriptionResponse build();
        }
    }

    private ManageSubscriptionOperation() {
    }
}
