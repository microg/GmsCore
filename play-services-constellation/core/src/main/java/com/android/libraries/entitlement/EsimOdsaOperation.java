/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.libraries.entitlement;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.StringDef;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * HTTP request parameters specific to on device service activation (ODSA). See GSMA spec TS.43
 * section 6.2.
 */
@AutoValue
public abstract class EsimOdsaOperation {
    /** ODSA operation unknown. For initialization only. */
    public static final String OPERATION_UNKNOWN = "";

    /** ODSA operation: CheckEligibility. */
    public static final String OPERATION_CHECK_ELIGIBILITY = "CheckEligibility";

    /** ODSA operation: ManageSubscription. */
    public static final String OPERATION_MANAGE_SUBSCRIPTION = "ManageSubscription";

    /** ODSA operation: ManageService. */
    public static final String OPERATION_MANAGE_SERVICE = "ManageService";

    /** ODSA operation: AcquireConfiguration. */
    public static final String OPERATION_ACQUIRE_CONFIGURATION = "AcquireConfiguration";

    /** ODSA operation: AcquireTemporaryToken. */
    public static final String OPERATION_ACQUIRE_TEMPORARY_TOKEN = "AcquireTemporaryToken";

    /** ODSA operation: GetPhoneNumber */
    public static final String OPERATION_GET_PHONE_NUMBER = "GetPhoneNumber";

    /** ODSA operation: GetSubscriberInfo */
    public static final String OPERATION_GET_SUBSCRIBER_INFO = "GetSubscriberInfo";

    /** ODSA operation: AcquirePlan */
    public static final String OPERATION_ACQUIRE_PLAN = "AcquirePlan";

    /** ODSA operation: VerifyPhoneNumber */
    public static final String OPERATION_VERIFY_PHONE_NUMBER = "VerifyPhoneNumber";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            OPERATION_UNKNOWN,
            OPERATION_CHECK_ELIGIBILITY,
            OPERATION_MANAGE_SUBSCRIPTION,
            OPERATION_MANAGE_SERVICE,
            OPERATION_ACQUIRE_CONFIGURATION,
            OPERATION_ACQUIRE_PLAN,
            OPERATION_ACQUIRE_TEMPORARY_TOKEN,
            OPERATION_GET_PHONE_NUMBER,
            OPERATION_GET_SUBSCRIBER_INFO,
            OPERATION_VERIFY_PHONE_NUMBER
    })
    public @interface OdsaOperation {
    }

    /** eSIM device’s service is unknown. */
    public static final int SERVICE_STATUS_UNKNOWN = -1;

    /** eSIM device’s service is activated. */
    public static final int SERVICE_STATUS_ACTIVATED = 1;

    /** eSIM device’s service is being activated. */
    public static final int SERVICE_STATUS_ACTIVATING = 2;

    /** eSIM device’s service is not activated. */
    public static final int SERVICE_STATUS_DEACTIVATED = 3;

    /** eSIM device’s service is not activated and the associated ICCID should not be reused. */
    public static final int SERVICE_STATUS_DEACTIVATED_NO_REUSE = 4;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            SERVICE_STATUS_UNKNOWN,
            SERVICE_STATUS_ACTIVATED,
            SERVICE_STATUS_ACTIVATING,
            SERVICE_STATUS_DEACTIVATED,
            SERVICE_STATUS_DEACTIVATED_NO_REUSE
    })
    public @interface OdsaServiceStatus {
    }

    /** Indicates that operation_type is not set. */
    public static final int OPERATION_TYPE_NOT_SET = -1;

    /** To activate a subscription, used by {@link #OPERATION_MANAGE_SUBSCRIPTION}. */
    public static final int OPERATION_TYPE_SUBSCRIBE = 0;

    /** To cancel a subscription, used by {@link #OPERATION_MANAGE_SUBSCRIPTION}. */
    public static final int OPERATION_TYPE_UNSUBSCRIBE = 1;

    /** To manage an existing subscription, for {@link #OPERATION_MANAGE_SUBSCRIPTION}. */
    public static final int OPERATION_TYPE_CHANGE_SUBSCRIPTION = 2;

    /**
     * To transfer a subscription from an existing device, used by {@link
     * #OPERATION_MANAGE_SUBSCRIPTION}.
     */
    public static final int OPERATION_TYPE_TRANSFER_SUBSCRIPTION = 3;

    /**
     * To inform the network of a subscription update, used by
     * {@link #OPERATION_MANAGE_SUBSCRIPTION}.
     */
    public static final int OPERATION_TYPE_UPDATE_SUBSCRIPTION = 4;

    /** To activate a service, used by {@link #OPERATION_MANAGE_SERVICE}. */
    public static final int OPERATION_TYPE_ACTIVATE_SERVICE = 10;

    /** To deactivate a service, used by {@link #OPERATION_MANAGE_SERVICE}. */
    public static final int OPERATION_TYPE_DEACTIVATE_SERVICE = 11;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            OPERATION_TYPE_NOT_SET,
            OPERATION_TYPE_SUBSCRIBE,
            OPERATION_TYPE_UNSUBSCRIBE,
            OPERATION_TYPE_CHANGE_SUBSCRIPTION,
            OPERATION_TYPE_TRANSFER_SUBSCRIPTION,
            OPERATION_TYPE_UPDATE_SUBSCRIPTION,
            OPERATION_TYPE_ACTIVATE_SERVICE,
            OPERATION_TYPE_DEACTIVATE_SERVICE
    })
    public @interface OdsaOperationType {
    }

    /** Operation result unknown. */
    public static final int OPERATION_RESULT_UNKNOWN = -1;

    /** Operation was a success. */
    public static final int OPERATION_RESULT_SUCCESS = 1;

    /** There was a general error during processing. */
    public static final int OPERATION_RESULT_ERROR_GENERAL = 100;

    /** An invalid operation value was provided in request. */
    public static final int OPERATION_RESULT_ERROR_INVALID_OPERATION = 101;

    /** An invalid parameter name or value was provided in request. */
    public static final int OPERATION_RESULT_ERROR_INVALID_PARAMETER = 102;

    /**
     * The optional operation is not supported by the carrier. Device should continue with the flow.
     * This error only applies to optional operations (for example ManageService).
     */
    public static final int OPERATION_RESULT_WARNING_NOT_SUPPORTED_OPERATION = 103;

    /** The user has entered an invalid response for the MSG content. */
    public static final int OPERATION_RESULT_ERROR_INVALID_MSG_RESPONSE = 104;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            OPERATION_RESULT_UNKNOWN,
            OPERATION_RESULT_SUCCESS,
            OPERATION_RESULT_ERROR_GENERAL,
            OPERATION_RESULT_ERROR_INVALID_OPERATION,
            OPERATION_RESULT_ERROR_INVALID_PARAMETER,
            OPERATION_RESULT_WARNING_NOT_SUPPORTED_OPERATION,
            OPERATION_RESULT_ERROR_INVALID_MSG_RESPONSE
    })
    public @interface OdsaOperationResult {
    }

    /** Companion service unknown. For initialization only. */
    public static final String COMPANION_SERVICE_UNKNOWN = "";

    /** Indicates the companion device carries the same MSISDN as the primary device. */
    public static final String COMPANION_SERVICE_SHARED_NUMBER = "SharedNumber";

    /** Indicates the companion device carries a different MSISDN as the primary device. */
    public static final String COMPANION_SERVICE_DIFFERENT_NUMBER = "DiffNumber";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            COMPANION_SERVICE_UNKNOWN,
            COMPANION_SERVICE_SHARED_NUMBER,
            COMPANION_SERVICE_DIFFERENT_NUMBER
    })
    public @interface CompanionService {
    }

    /** Indicates the MSG content has been rejected by the user. */
    public static final String MESSAGE_BUTTON_REJECTED = "0";

    /** Indicates the MSG content has been accepted by the user. */
    public static final String MESSAGE_BUTTON_ACCEPTED = "1";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            MESSAGE_BUTTON_REJECTED,
            MESSAGE_BUTTON_ACCEPTED
    })
    public @interface MessageButton {
    }

    /** Returns the ODSA operation. Used by HTTP parameter {@code operation}. */
    public abstract String operation();

    /**
     * Returns the detailed type of the ODSA operation. Used by HTTP parameter
     * {@code operation_type}.
     */
    public abstract int operationType();

    /**
     * Returns the comma separated list of operation targets used with temporary token from
     * AcquireTemporaryToken operation. Used by HTTP parameter {@code operation_targets}.
     */
    public abstract ImmutableList<String> operationTargets();

    /**
     * Returns the unique identifier of the companion device, like IMEI. Used by HTTP parameter
     * {@code
     * companion_terminal_id}.
     */
    public abstract String companionTerminalId();

    /**
     * Returns the OEM of the companion device. Used by HTTP parameter {@code
     * companion_terminal_vendor}.
     */
    public abstract String companionTerminalVendor();

    /**
     * Returns the model of the companion device. Used by HTTP parameter {@code
     * companion_terminal_model}.
     */
    public abstract String companionTerminalModel();

    /**
     * Returns the software version of the companion device. Used by HTTP parameter {@code
     * companion_terminal_sw_version}.
     */
    public abstract String companionTerminalSoftwareVersion();

    /**
     * Returns the user-friendly version of the companion device. Used by HTTP parameter {@code
     * companion_terminal_friendly_name}.
     */
    public abstract String companionTerminalFriendlyName();

    /**
     * Returns the service type of the companion device, e.g. if the MSISDN is same as the primary
     * device. Used by HTTP parameter {@code companion_terminal_service}.
     */
    public abstract String companionTerminalService();

    /**
     * Returns the ICCID of the companion device. Used by HTTP parameter {@code
     * companion_terminal_iccid}.
     */
    public abstract String companionTerminalIccid();

    /**
     * Returns the EID of the companion device. Used by HTTP parameter
     * {@code companion_terminal_eid}.
     */
    public abstract String companionTerminalEid();

    /**
     * Returns the ICCID of the primary device eSIM. Used by HTTP parameter {@code terminal_iccid}.
     */
    public abstract String terminalIccid();

    /**
     * Returns the eUICC identifier (EID) of the primary device eSIM. Used by HTTP parameter {@code
     * terminal_eid}.
     */
    public abstract String terminalEid();

    /**
     * Returns the unique identifier of the primary device eSIM, like the IMEI associated with the
     * eSIM. Used by HTTP parameter {@code target_terminal_id}.
     */
    public abstract String targetTerminalId();

    /**
     * Returns the unique identifiers of the primary device eSIM if more than one, like the IMEIs on
     * dual-SIM devices. Used by HTTP parameter {@code target_terminal_imeis}.
     *
     * <p>This is a non-standard params required by some carriers.
     */
    @NonNull
    public abstract ImmutableList<String> targetTerminalIds();

    /**
     * Returns the ICCID primary device eSIM. Used by HTTP parameter {@code target_terminal_iccid}.
     */
    public abstract String targetTerminalIccid();

    /**
     * Returns the eUICC identifier (EID) of the primary device eSIM. Used by HTTP parameter {@code
     * target_terminal_eid}.
     */
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
     * Returns the model of primary device. Used by HTTP parameter {@code target_terminal_model}.
     *
     * <p>This is a non-standard params required by some carriers.
     */
    @NonNull
    public abstract String targetTerminalModel();

    /**
     * Returns the unique identifier of the old device eSIM, like the IMEI associated with the eSIM.
     * Used by HTTP parameter {@code old_terminal_id}.
     */
    public abstract String oldTerminalId();

    /** Returns the ICCID of old device eSIM. Used by HTTP parameter {@code old_terminal_iccid}. */
    public abstract String oldTerminalIccid();

    /**
     * Returns the user response to the MSG content. Used by HTTP parameter {@code MSG_response}.
     */
    public abstract String messageResponse();

    /**
     * Returns whether the user has accepted or rejected the MSG content.
     * Used by HTTP parameter {@code MSG_btn}.
     */
    @MessageButton
    public abstract String messageButton();

    /** Optional requestor id for certain operations (e.g., GetSubscriberInfo). */
    public abstract String requestorId();

    /** Returns a new {@link Builder} object. */
    public static Builder builder() {
        return new AutoValue_EsimOdsaOperation.Builder()
                .setOperation(OPERATION_UNKNOWN)
                .setOperationType(OPERATION_TYPE_NOT_SET)
                .setOperationTargets(ImmutableList.of())
                .setCompanionTerminalId("")
                .setCompanionTerminalVendor("")
                .setCompanionTerminalModel("")
                .setCompanionTerminalSoftwareVersion("")
                .setCompanionTerminalFriendlyName("")
                .setCompanionTerminalService(COMPANION_SERVICE_UNKNOWN)
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
                .setMessageResponse("")
                .setMessageButton("")
                .setRequestorId("");
    }

    /**
     * Builder.
     *
     * <p>For ODSA, the rule of which parameters are required varies or each
     * operation/operation_type.
     * The Javadoc below gives high-level description, but please refer to GSMA spec TS.43 section
     * 6.2
     * for details.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * Sets the eSIM ODSA operation. Used by HTTP parameter {@code operation}.
         *
         * @param operation ODSA operation.
         * @return The builder.
         * @see #OPERATION_CHECK_ELIGIBILITY
         * @see #OPERATION_MANAGE_SUBSCRIPTION
         * @see #OPERATION_MANAGE_SERVICE
         * @see #OPERATION_ACQUIRE_CONFIGURATION
         * @see #OPERATION_ACQUIRE_TEMPORARY_TOKEN
         * @see #OPERATION_GET_PHONE_NUMBER
         * @see #OPERATION_ACQUIRE_PLAN
         * @see #OPERATION_VERIFY_PHONE_NUMBER
         */
        @NonNull
        public abstract Builder setOperation(@NonNull @OdsaOperation String operation);

        /**
         * Sets the detailed type of the eSIM ODSA operation. Used by HTTP parameter
         * "operation_type" if
         * set.
         *
         * <p>Required by some operation.
         *
         * @see #OPERATION_TYPE_SUBSCRIBE
         * @see #OPERATION_TYPE_UNSUBSCRIBE
         * @see #OPERATION_TYPE_CHANGE_SUBSCRIPTION
         * @see #OPERATION_TYPE_TRANSFER_SUBSCRIPTION
         * @see #OPERATION_TYPE_UPDATE_SUBSCRIPTION
         * @see #OPERATION_TYPE_ACTIVATE_SERVICE
         * @see #OPERATION_TYPE_DEACTIVATE_SERVICE
         */
        @NonNull
        public abstract Builder setOperationType(@OdsaOperationType int operationType);

        /**
         * Sets the operation targets to be used with temporary token from AcquireTemporaryToken
         * operation. Used by HTTP parameter {@code operation_targets} if set.
         */
        @NonNull
        public abstract Builder setOperationTargets(
                @NonNull @OdsaOperation ImmutableList<String> operationTargets);

        /**
         * Sets the unique identifier of the companion device, like IMEI. Used by HTTP parameter
         * {@code
         * companion_terminal_id} if set.
         *
         * <p>Used by companion device ODSA operation.
         *
         * @param companionTerminalId The unique identifier of the companion device.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setCompanionTerminalId(@NonNull String companionTerminalId);

        /**
         * Sets the OEM of the companion device. Used by HTTP parameter {@code
         * companion_terminal_vendor} if set.
         *
         * <p>Used by companion device ODSA operation.
         *
         * @param companionTerminalVendor The OEM of the companion device.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setCompanionTerminalVendor(@NonNull String companionTerminalVendor);

        /**
         * Sets the model of the companion device. Used by HTTP parameter {@code
         * companion_terminal_model} if set.
         *
         * <p>Used by companion device ODSA operation.
         *
         * @param companionTerminalModel The model of the companion device.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setCompanionTerminalModel(@NonNull String companionTerminalModel);

        /**
         * Sets the software version of the companion device. Used by HTTP parameter {@code
         * companion_terminal_sw_version} if set.
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
         * Sets the user-friendly version of the companion device. Used by HTTP parameter {@code
         * companion_terminal_friendly_name} if set.
         *
         * <p>Used by companion device ODSA operation.
         *
         * @param companionTerminalFriendlyName The user-friendly version of the companion device.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setCompanionTerminalFriendlyName(
                @NonNull String companionTerminalFriendlyName);

        /**
         * Sets the service type of the companion device, e.g. if the MSISDN is same as the primary
         * device. Used by HTTP parameter {@code companion_terminal_service} if set.
         *
         * <p>Used by companion device ODSA operation.
         *
         * @param companionTerminalService The service type of the companion device.
         * @return The builder.
         * @see #COMPANION_SERVICE_SHARED_NUMBER
         * @see #COMPANION_SERVICE_DIFFERENT_NUMBER
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
        public abstract Builder setCompanionTerminalIccid(@NonNull String companionTerminalIccid);

        /**
         * Sets the eUICC identifier (EID) of the companion device. Used by HTTP parameter {@code
         * companion_terminal_eid} if set.
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
         * HTTP
         * parameter {@code terminal_eid} if set.
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
         *                    primary
         *                    SIM not present.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setTerminalEid(@NonNull String terminalEid);

        /**
         * Sets the unique identifier of the primary device eSIM in case of multiple SIM, like the
         * IMEI
         * associated with the eSIM. Used by HTTP parameter {@code target_terminal_id} if set.
         *
         * <p>Used by primary device ODSA operation.
         *
         * @param targetTerminalId The unique identifier of the primary device eSIM in case of
         *                         multiple
         *                         SIM.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setTargetTerminalId(@NonNull String targetTerminalId);

        /**
         * Sets the unique identifiers of the primary device eSIM if more than one, like the IMEIs
         * on
         * dual-SIM devices. Used by HTTP parameter {@code target_terminal_imeis}.
         *
         * <p>This is a non-standard params required by some carriers.
         *
         * @param targetTerminalIds The unique identifiers of the primary device eSIM if more than
         *                          one.
         * @return The builder.
         */
        public abstract Builder setTargetTerminalIds(
                @NonNull ImmutableList<String> targetTerminalIds);

        /**
         * Sets the ICCID primary device eSIM in case of multiple SIM. Used by HTTP parameter {@code
         * target_terminal_iccid} if set.
         *
         * <p>Used by primary device ODSA operation.
         *
         * @param targetTerminalIccid The ICCID primary device eSIM in case of multiple SIM.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setTargetTerminalIccid(@NonNull String targetTerminalIccid);

        /**
         * Sets the eUICC identifier (EID) of the primary device eSIM in case of multiple SIM. Used
         * by
         * HTTP parameter {@code target_terminal_eid} if set.
         *
         * <p>Used by primary device ODSA operation.
         *
         * @param terminalEid The eUICC identifier (EID) of the primary device eSIM in case of
         *                    multiple
         *                    SIM.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setTargetTerminalEid(@NonNull String terminalEid);

        /**
         * Sets the serial number of primary device. Used by HTTP parameter
         * {@code target_terminal_sn}.
         *
         * @param targetTerminalSerialNumber The serial number of primary device.
         *                                   <p>This is a non-standard params required by some
         *                                   carriers.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setTargetTerminalSerialNumber(
                @NonNull String targetTerminalSerialNumber);

        /**
         * Sets the model of primary device. Used by HTTP parameter {@code target_terminal_model}.
         *
         * @param targetTerminalModel The model of primary device.
         *                            <p>This is a non-standard params required by some carriers.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setTargetTerminalModel(@NonNull String targetTerminalModel);

        /**
         * Sets the unique identifier of the old device eSIM, like the IMEI associated with the
         * eSIM.
         * Used by HTTP parameter {@code old_terminal_id} if set.
         *
         * <p>Used by primary device ODSA operation.
         *
         * @param oldTerminalId The unique identifier of the old device eSIM.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setOldTerminalId(@NonNull String oldTerminalId);

        /**
         * Sets the ICCID old device eSIM. Used by HTTP parameter {@code old_terminal_iccid} if set.
         *
         * <p>Used by primary device ODSA operation.
         *
         * @param oldTerminalIccid The ICCID old device eSIM.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setOldTerminalIccid(@NonNull String oldTerminalIccid);

        /**
         * Sets the user response to the MSG content. Used by HTTP parameter {@code MSG_response}
         * if set.
         *
         * <p>Used by primary device ODSA operation.
         *
         * @param messageResponse The response entered by the user on the device UI.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setMessageResponse(@NonNull String messageResponse);

        /**
         * Sets whether the user has accepted or rejected the MSG content. Used by HTTP parameter
         * {@code MSG_btn} if set.
         *
         * <p>Used by primary device ODSA operation.
         *
         * @param messageButton Whether the user has accepted or rejected the MSG content.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setMessageButton(@NonNull String messageButton);

        /**
         * Sets the optional requestor id parameter for operations that require it.
         */
        @NonNull
        public abstract Builder setRequestorId(@NonNull String requestorId);

        /** Returns the {@link EsimOdsaOperation} object. */
        @NonNull
        public abstract EsimOdsaOperation build();
    }
}
