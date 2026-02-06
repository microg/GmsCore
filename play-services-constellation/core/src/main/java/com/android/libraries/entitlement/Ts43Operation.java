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

package com.android.libraries.entitlement;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.libraries.entitlement.EsimOdsaOperation.OdsaServiceStatus;
import com.android.libraries.entitlement.http.HttpConstants;
import com.android.libraries.entitlement.odsa.AcquireConfigurationOperation.AcquireConfigurationRequest;
import com.android.libraries.entitlement.odsa.AcquireConfigurationOperation.AcquireConfigurationResponse;
import com.android.libraries.entitlement.odsa.AcquireTemporaryTokenOperation.AcquireTemporaryTokenRequest;
import com.android.libraries.entitlement.odsa.AcquireTemporaryTokenOperation.AcquireTemporaryTokenResponse;
import com.android.libraries.entitlement.odsa.CheckEligibilityOperation;
import com.android.libraries.entitlement.odsa.CheckEligibilityOperation.CheckEligibilityRequest;
import com.android.libraries.entitlement.odsa.CheckEligibilityOperation.CheckEligibilityResponse;
import com.android.libraries.entitlement.odsa.DownloadInfo;
import com.android.libraries.entitlement.odsa.GetPhoneNumberOperation.GetPhoneNumberRequest;
import com.android.libraries.entitlement.odsa.GetPhoneNumberOperation.GetPhoneNumberResponse;
import com.android.libraries.entitlement.odsa.GetSubscriberInfoOperation.GetSubscriberInfoRequest;
import com.android.libraries.entitlement.odsa.GetSubscriberInfoOperation.GetSubscriberInfoResponse;
import com.android.libraries.entitlement.odsa.ManageServiceOperation.ManageServiceRequest;
import com.android.libraries.entitlement.odsa.ManageServiceOperation.ManageServiceResponse;
import com.android.libraries.entitlement.odsa.ManageSubscriptionOperation.ManageSubscriptionRequest;
import com.android.libraries.entitlement.odsa.ManageSubscriptionOperation.ManageSubscriptionResponse;
import com.android.libraries.entitlement.odsa.MessageInfo;
import com.android.libraries.entitlement.odsa.OdsaResponse;
import com.android.libraries.entitlement.odsa.PlanOffer;
import com.android.libraries.entitlement.utils.Ts43Constants;
import com.android.libraries.entitlement.utils.Ts43XmlDoc;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** TS43 operations described in GSMA Service Entitlement Configuration spec. */
public class Ts43Operation {
    private static final String TAG = "Ts43";

    /**
     * The normal token retrieved via {@link Ts43Authentication#getAuthToken(int, String, String,
     * String)} or {@link Ts43Authentication#getAuthToken(URL)}.
     */
    public static final int TOKEN_TYPE_NORMAL = 1;

    /**
     * The temporary token retrieved via {@link
     * Ts43Operation#acquireTemporaryToken(AcquireTemporaryTokenRequest)}.
     */
    public static final int TOKEN_TYPE_TEMPORARY = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TOKEN_TYPE_NORMAL, TOKEN_TYPE_TEMPORARY})
    public @interface TokenType {
    }

    /** The application context. */
    @NonNull
    private final Context mContext;

    /**
     * The TS.43 entitlement version to use. For example, {@code "9.0"}. If {@code null}, version
     * {@code "2.0"} will be used by default.
     */
    @NonNull
    private final String mEntitlementVersion;

    /** The entitlement server address. */
    @NonNull
    private final URL mEntitlementServerAddress;

    /**
     * The authentication token used for TS.43 operation. This token could be automatically updated
     * after each TS.43 operation if the server provides the new token in the operation's HTTP
     * response.
     */
    @Nullable
    private String mAuthToken;

    /**
     * The temporary token retrieved from {@link
     * #acquireTemporaryToken(AcquireTemporaryTokenRequest)}.
     */
    @Nullable
    private String mTemporaryToken;

    /**
     * Token type. When token type is {@link #TOKEN_TYPE_NORMAL}, {@link #mAuthToken} is used. When
     * toke type is {@link #TOKEN_TYPE_TEMPORARY}, {@link #mTemporaryToken} is used.
     */
    @TokenType
    private int mTokenType;

    private final ServiceEntitlement mServiceEntitlement;

    /** IMEI of the device. */
    private final String mImei;

    /** used to identify the requesting application. Optional */
    @NonNull
    private final String mAppName;

    /**
     * Constructor of Ts43Operation.
     *
     * @param slotIndex The logical SIM slot index involved in ODSA operation.
     * @param entitlementServerAddress The entitlement server address.
     * @param entitlementVersion The TS.43 entitlement version to use. For example,
     *                           {@code "9.0"}. If {@code null}, version {@code "2.0"} will be used
     *                           by default.
     * @param authToken The authentication token.
     * @param tokenType The token type. Can be {@link #TOKEN_TYPE_NORMAL} or
     *                  {@link #TOKEN_TYPE_TEMPORARY}.
     * @param appName The name of the device application making the request or empty string
     *                if unspecified.
     */
    public Ts43Operation(
            @NonNull Context context,
            int slotIndex,
            @NonNull URL entitlementServerAddress,
            @Nullable String entitlementVersion,
            @NonNull String authToken,
            @TokenType int tokenType,
            @NonNull String appName) {
        mContext = context;
        mEntitlementServerAddress = entitlementServerAddress;
        if (entitlementVersion != null) {
            mEntitlementVersion = entitlementVersion;
        } else {
            mEntitlementVersion = Ts43Constants.DEFAULT_ENTITLEMENT_VERSION;
        }

        if (tokenType == TOKEN_TYPE_NORMAL) {
            mAuthToken = authToken;
        } else if (tokenType == TOKEN_TYPE_TEMPORARY) {
            mTemporaryToken = authToken;
        } else {
            throw new IllegalArgumentException("Invalid token type " + tokenType);
        }
        mTokenType = tokenType;

        CarrierConfig carrierConfig =
                CarrierConfig.builder().setServerUrl(mEntitlementServerAddress.toString()).build();

        mServiceEntitlement =
                new ServiceEntitlement(
                        mContext, carrierConfig, SubscriptionManager.getSubscriptionId(slotIndex));

        String imei = null;
        TelephonyManager telephonyManager = mContext.getSystemService(TelephonyManager.class);
        if (telephonyManager != null) {
            if (slotIndex < 0 || slotIndex >= telephonyManager.getActiveModemCount()) {
                throw new IllegalArgumentException("getAuthToken: invalid slot index " + slotIndex);
            }
            imei = telephonyManager.getImei(slotIndex);
        }
        mImei = Strings.nullToEmpty(imei);
        mAppName = appName;
    }

    /**
     * To verify if end-user is allowed to invoke the ODSA application as described in GSMA Service
     * Entitlement Configuration section 6.2 and 6.5.2.
     *
     * @return {@code true} if the end-user is allowed to perform ODSA operation.
     * @throws ServiceEntitlementException The exception for error case. If it's an HTTP response
     *                                     error from the server, the error code can be retrieved by
     *                                     {@link ServiceEntitlementException#getHttpStatus()}
     */
    @NonNull
    public CheckEligibilityResponse checkEligibility(
            @NonNull CheckEligibilityRequest checkEligibilityRequest)
            throws ServiceEntitlementException {
        Objects.requireNonNull(checkEligibilityRequest);

        ServiceEntitlementRequest.Builder builder =
                ServiceEntitlementRequest.builder()
                        .setEntitlementVersion(mEntitlementVersion)
                        .setTerminalId(mImei)
                        .setAppName(mAppName);

        if (mTokenType == TOKEN_TYPE_NORMAL) {
            builder.setAuthenticationToken(mAuthToken);
        } else if (mTokenType == TOKEN_TYPE_TEMPORARY) {
            builder.setTemporaryToken(mTemporaryToken);
        }

        String notificationToken = checkEligibilityRequest.notificationToken();
        if (!TextUtils.isEmpty(notificationToken)) {
            builder.setNotificationToken(notificationToken);
        }
        int notificationAction = checkEligibilityRequest.notificationAction();
        if (Ts43Constants.isValidNotificationAction(notificationAction)) {
            builder.setNotificationAction(notificationAction);
        }

        ServiceEntitlementRequest request = builder.build();

        EsimOdsaOperation operation =
                EsimOdsaOperation.builder()
                        .setOperation(EsimOdsaOperation.OPERATION_CHECK_ELIGIBILITY)
                        .setCompanionTerminalId(checkEligibilityRequest.companionTerminalId())
                        .setCompanionTerminalVendor(
                                checkEligibilityRequest.companionTerminalVendor())
                        .setCompanionTerminalModel(checkEligibilityRequest.companionTerminalModel())
                        .setCompanionTerminalSoftwareVersion(
                                checkEligibilityRequest.companionTerminalSoftwareVersion())
                        .setCompanionTerminalFriendlyName(
                                checkEligibilityRequest.companionTerminalFriendlyName())
                        .build();

        String rawXml;
        try {
            rawXml =
                    mServiceEntitlement.performEsimOdsa(checkEligibilityRequest.appId(), request,
                            operation);
        } catch (ServiceEntitlementException e) {
            Log.w(TAG, "manageSubscription: Failed to perform ODSA operation. e=" + e);
            throw e;
        }

        // Build the response of check eligibility operation. Refer to GSMA Service Entitlement
        // Configuration section 6.5.2.
        CheckEligibilityResponse.Builder responseBuilder = CheckEligibilityResponse.builder();

        Ts43XmlDoc ts43XmlDoc = new Ts43XmlDoc(rawXml);

        try {
            processGeneralResult(ts43XmlDoc, responseBuilder);
        } catch (MalformedURLException e) {
            throw new ServiceEntitlementException(
                    ServiceEntitlementException.ERROR_MALFORMED_HTTP_RESPONSE,
                    "checkEligibility: Malformed URL " + rawXml);
        }

        // Parse the eligibility
        String eligibilityString =
                ts43XmlDoc.get(
                        ImmutableList.of(Ts43XmlDoc.CharacteristicType.APPLICATION),
                        Ts43XmlDoc.Parm.PRIMARY_APP_ELIGIBILITY);
        if (TextUtils.isEmpty(eligibilityString)) {
            eligibilityString =
                    ts43XmlDoc.get(
                            ImmutableList.of(Ts43XmlDoc.CharacteristicType.APPLICATION),
                            Ts43XmlDoc.Parm.COMPANION_APP_ELIGIBILITY);
        }

        int eligibility = CheckEligibilityOperation.ELIGIBILITY_RESULT_UNKNOWN;
        if (!TextUtils.isEmpty(eligibilityString)) {
            switch (eligibilityString) {
                case Ts43XmlDoc.ParmValues.DISABLED:
                    eligibility = CheckEligibilityOperation.ELIGIBILITY_RESULT_DISABLED;
                    break;
                case Ts43XmlDoc.ParmValues.ENABLED:
                    eligibility = CheckEligibilityOperation.ELIGIBILITY_RESULT_ENABLED;
                    break;
                case Ts43XmlDoc.ParmValues.INCOMPATIBLE:
                    eligibility = CheckEligibilityOperation.ELIGIBILITY_RESULT_INCOMPATIBLE;
                    break;
            }
        }
        responseBuilder.setAppEligibility(eligibility);

        // Parse companion device services
        String companionDeviceServices =
                ts43XmlDoc.get(
                        ImmutableList.of(Ts43XmlDoc.CharacteristicType.APPLICATION),
                        Ts43XmlDoc.Parm.COMPANION_DEVICE_SERVICES);

        if (!TextUtils.isEmpty(companionDeviceServices)) {
            List<String> companionDeviceServicesList =
                    Arrays.asList(companionDeviceServices.split("\\s*,\\s*"));
            responseBuilder.setCompanionDeviceServices(
                    ImmutableList.copyOf(companionDeviceServicesList));
        }

        // Parse notEnabledURL
        URL notEnabledURL = null;
        String notEnabledURLString =
                ts43XmlDoc.get(
                        ImmutableList.of(Ts43XmlDoc.CharacteristicType.APPLICATION),
                        Ts43XmlDoc.Parm.NOT_ENABLED_URL);

        try {
            notEnabledURL = new URL(notEnabledURLString);
            responseBuilder.setNotEnabledUrl(notEnabledURL);
        } catch (MalformedURLException e) {
            Log.w(TAG, "checkEligibility: malformed URL " + notEnabledURLString);
        }

        // Parse notEnabledUserData
        String notEnabledUserData =
                ts43XmlDoc.get(
                        ImmutableList.of(Ts43XmlDoc.CharacteristicType.APPLICATION),
                        Ts43XmlDoc.Parm.NOT_ENABLED_USER_DATA);

        if (!TextUtils.isEmpty(notEnabledUserData)) {
            responseBuilder.setNotEnabledUserData(notEnabledUserData);
        }

        // Parse notEnabledContentsType
        String notEnabledContentsTypeString =
                ts43XmlDoc.get(
                        ImmutableList.of(Ts43XmlDoc.CharacteristicType.APPLICATION),
                        Ts43XmlDoc.Parm.NOT_ENABLED_CONTENTS_TYPE);

        int notEnabledContentsType = HttpConstants.ContentType.UNKNOWN;
        if (!TextUtils.isEmpty(notEnabledContentsTypeString)) {
            switch (notEnabledContentsTypeString) {
                case Ts43XmlDoc.ParmValues.CONTENTS_TYPE_XML:
                    notEnabledContentsType = HttpConstants.ContentType.XML;
                    break;
                case Ts43XmlDoc.ParmValues.CONTENTS_TYPE_JSON:
                    notEnabledContentsType = HttpConstants.ContentType.JSON;
                    break;
            }
        }
        responseBuilder.setNotEnabledContentsType(notEnabledContentsType);

        return responseBuilder.build();
    }

    /**
     * To request for subscription-related action on a primary or companion device as described in
     * GSMA Service Entitlement Configuration section 6.2 and 6.5.3.
     *
     * @param manageSubscriptionRequest The manage subscription request.
     * @return The response of manage subscription request.
     * @throws ServiceEntitlementException The exception for error case. If it's an HTTP response
     *                                     error from the server, the error code can be retrieved by
     *                                     {@link ServiceEntitlementException#getHttpStatus()}
     */
    @NonNull
    public ManageSubscriptionResponse manageSubscription(
            @NonNull ManageSubscriptionRequest manageSubscriptionRequest)
            throws ServiceEntitlementException {
        Objects.requireNonNull(manageSubscriptionRequest);

        ServiceEntitlementRequest.Builder builder =
                ServiceEntitlementRequest.builder()
                        .setEntitlementVersion(mEntitlementVersion)
                        .setTerminalId(mImei)
                        .setAppName(mAppName)
                        .setAcceptContentType(ServiceEntitlementRequest.ACCEPT_CONTENT_TYPE_XML);

        if (mTokenType == TOKEN_TYPE_NORMAL) {
            builder.setAuthenticationToken(mAuthToken);
        } else if (mTokenType == TOKEN_TYPE_TEMPORARY) {
            builder.setTemporaryToken(mTemporaryToken);
        }

        String notificationToken = manageSubscriptionRequest.notificationToken();
        if (!TextUtils.isEmpty(notificationToken)) {
            builder.setNotificationToken(notificationToken);
        }
        int notificationAction = manageSubscriptionRequest.notificationAction();
        if (Ts43Constants.isValidNotificationAction(notificationAction)) {
            builder.setNotificationAction(notificationAction);
        }

        ServiceEntitlementRequest request = builder.build();

        EsimOdsaOperation operation =
                EsimOdsaOperation.builder()
                        .setOperation(EsimOdsaOperation.OPERATION_MANAGE_SUBSCRIPTION)
                        .setOperationType(manageSubscriptionRequest.operationType())
                        .setCompanionTerminalId(manageSubscriptionRequest.companionTerminalId())
                        .setCompanionTerminalVendor(
                                manageSubscriptionRequest.companionTerminalVendor())
                        .setCompanionTerminalModel(
                                manageSubscriptionRequest.companionTerminalModel())
                        .setCompanionTerminalSoftwareVersion(
                                manageSubscriptionRequest.companionTerminalSoftwareVersion())
                        .setCompanionTerminalFriendlyName(
                                manageSubscriptionRequest.companionTerminalFriendlyName())
                        .setCompanionTerminalService(
                                manageSubscriptionRequest.companionTerminalService())
                        .setCompanionTerminalIccid(
                                manageSubscriptionRequest.companionTerminalIccid())
                        .setCompanionTerminalEid(manageSubscriptionRequest.companionTerminalEid())
                        .setTerminalIccid(manageSubscriptionRequest.terminalIccid())
                        .setTerminalEid(manageSubscriptionRequest.terminalEid())
                        .setTargetTerminalId(manageSubscriptionRequest.targetTerminalId())
                        // non TS.43 standard support
                        .setTargetTerminalIds(manageSubscriptionRequest.targetTerminalIds())
                        .setTargetTerminalIccid(manageSubscriptionRequest.targetTerminalIccid())
                        .setTargetTerminalEid(manageSubscriptionRequest.targetTerminalEid())
                        // non TS.43 standard support
                        .setTargetTerminalSerialNumber(
                                manageSubscriptionRequest.targetTerminalSerialNumber())
                        // non TS.43 standard support
                        .setTargetTerminalModel(manageSubscriptionRequest.targetTerminalModel())
                        .setOldTerminalId(manageSubscriptionRequest.oldTerminalId())
                        .setOldTerminalIccid(manageSubscriptionRequest.oldTerminalIccid())
                        .setMessageResponse(manageSubscriptionRequest.messageResponse())
                        .setMessageButton(manageSubscriptionRequest.messageButton())
                        .build();

        String rawXml;
        try {
            rawXml =
                    mServiceEntitlement.performEsimOdsa(
                            manageSubscriptionRequest.appId(), request, operation);
        } catch (ServiceEntitlementException e) {
            Log.w(TAG, "manageSubscription: Failed to perform ODSA operation. e=" + e);
            throw e;
        }

        // Build the response of manage subscription operation. Refer to GSMA Service Entitlement
        // Configuration section 6.5.3.
        ManageSubscriptionResponse.Builder responseBuilder = ManageSubscriptionResponse.builder();

        Ts43XmlDoc ts43XmlDoc;
        try {
            ts43XmlDoc = new Ts43XmlDoc(rawXml);
            processGeneralResult(ts43XmlDoc, responseBuilder);
        } catch (MalformedURLException e) {
            throw new ServiceEntitlementException(
                    ServiceEntitlementException.ERROR_MALFORMED_HTTP_RESPONSE,
                    "manageSubscription: Malformed URL " + rawXml);
        }

        int subscriptionResult = ManageSubscriptionResponse.SUBSCRIPTION_RESULT_UNKNOWN;

        // Parse subscription result.
        String subscriptionResultString =
                ts43XmlDoc.get(
                        ImmutableList.of(Ts43XmlDoc.CharacteristicType.APPLICATION),
                        Ts43XmlDoc.Parm.SUBSCRIPTION_RESULT);

        if (!TextUtils.isEmpty(subscriptionResultString)) {
            switch (subscriptionResultString) {
                case Ts43XmlDoc.ParmValues.SUBSCRIPTION_RESULT_CONTINUE_TO_WEBSHEET:
                    subscriptionResult =
                            ManageSubscriptionResponse.SUBSCRIPTION_RESULT_CONTINUE_TO_WEBSHEET;

                    String subscriptionServiceURLString =
                            ts43XmlDoc.get(
                                    ImmutableList.of(Ts43XmlDoc.CharacteristicType.APPLICATION),
                                    Ts43XmlDoc.Parm.SUBSCRIPTION_SERVICE_URL);

                    if (!TextUtils.isEmpty(subscriptionServiceURLString)) {
                        try {
                            responseBuilder.setSubscriptionServiceUrl(
                                    new URL(subscriptionServiceURLString));

                            String subscriptionServiceUserDataString =
                                    ts43XmlDoc.get(
                                            ImmutableList.of(
                                                    Ts43XmlDoc.CharacteristicType.APPLICATION),
                                            Ts43XmlDoc.Parm.SUBSCRIPTION_SERVICE_USER_DATA);
                            if (!TextUtils.isEmpty(subscriptionServiceUserDataString)) {
                                responseBuilder.setSubscriptionServiceUserData(
                                        subscriptionServiceUserDataString);
                            }

                            String subscriptionServiceContentsTypeString =
                                    ts43XmlDoc.get(
                                            ImmutableList.of(
                                                    Ts43XmlDoc.CharacteristicType.APPLICATION),
                                            Ts43XmlDoc.Parm.SUBSCRIPTION_SERVICE_CONTENTS_TYPE);
                            if (!TextUtils.isEmpty(subscriptionServiceContentsTypeString)) {
                                int contentsType = HttpConstants.ContentType.UNKNOWN;
                                switch (subscriptionServiceContentsTypeString) {
                                    case Ts43XmlDoc.ParmValues.CONTENTS_TYPE_XML:
                                        contentsType = HttpConstants.ContentType.XML;
                                        break;
                                    case Ts43XmlDoc.ParmValues.CONTENTS_TYPE_JSON:
                                        contentsType = HttpConstants.ContentType.JSON;
                                        break;
                                }
                                responseBuilder.setSubscriptionServiceContentsType(contentsType);
                            }
                        } catch (MalformedURLException e) {
                            Log.w(TAG, "Malformed URL received. " + subscriptionServiceURLString);
                        }
                    }
                    break;
                case Ts43XmlDoc.ParmValues.SUBSCRIPTION_RESULT_DOWNLOAD_PROFILE:
                    subscriptionResult =
                            ManageSubscriptionResponse.SUBSCRIPTION_RESULT_DOWNLOAD_PROFILE;
                    DownloadInfo downloadInfo =
                            parseDownloadInfo(
                                    ImmutableList.of(
                                            Ts43XmlDoc.CharacteristicType.APPLICATION,
                                            Ts43XmlDoc.CharacteristicType.DOWNLOAD_INFO),
                                    ts43XmlDoc);
                    if (downloadInfo != null) {
                        responseBuilder.setDownloadInfo(downloadInfo);
                    }
                    break;
                case Ts43XmlDoc.ParmValues.SUBSCRIPTION_RESULT_DONE:
                    subscriptionResult = ManageSubscriptionResponse.SUBSCRIPTION_RESULT_DONE;
                    break;
                case Ts43XmlDoc.ParmValues.SUBSCRIPTION_RESULT_DELAYED_DOWNLOAD:
                    subscriptionResult =
                            ManageSubscriptionResponse.SUBSCRIPTION_RESULT_DELAYED_DOWNLOAD;
                    break;
                case Ts43XmlDoc.ParmValues.SUBSCRIPTION_RESULT_DISMISS:
                    subscriptionResult = ManageSubscriptionResponse.SUBSCRIPTION_RESULT_DISMISS;
                    break;
                case Ts43XmlDoc.ParmValues.SUBSCRIPTION_RESULT_DELETE_PROFILE_IN_USE:
                    subscriptionResult =
                            ManageSubscriptionResponse.SUBSCRIPTION_RESULT_DELETE_PROFILE_IN_USE;
                    break;
                case Ts43XmlDoc.ParmValues.SUBSCRIPTION_RESULT_REDOWNLOADABLE_PROFILE_IS_MANDATORY:
                    subscriptionResult =
                            ManageSubscriptionResponse
                                    .SUBSCRIPTION_RESULT_REDOWNLOADABLE_PROFILE_IS_MANDATORY;
                    break;
                case Ts43XmlDoc.ParmValues.SUBSCRIPTION_RESULT_REQUIRES_USER_INPUT:
                    subscriptionResult =
                            ManageSubscriptionResponse.SUBSCRIPTION_RESULT_REQUIRES_USER_INPUT;
                    break;
            }
        }

        responseBuilder.setSubscriptionResult(subscriptionResult);
        return responseBuilder.build();
    }

    /**
     * To activate/deactivate the service on the primary or companion device as described in GSMA
     * Service Entitlement Configuration section 6.2 and 6.5.4. This is an optional operation.
     *
     * @param manageServiceRequest The manage service request.
     * @return The response of manage service request.
     * @throws ServiceEntitlementException The exception for error case. If it's an HTTP response
     *                                     error from the server, the error code can be retrieved by
     *                                     {@link ServiceEntitlementException#getHttpStatus()}
     */
    @NonNull
    public ManageServiceResponse manageService(@NonNull ManageServiceRequest manageServiceRequest)
            throws ServiceEntitlementException {
        Objects.requireNonNull(manageServiceRequest);

        ServiceEntitlementRequest.Builder builder =
                ServiceEntitlementRequest.builder()
                        .setEntitlementVersion(mEntitlementVersion)
                        .setTerminalId(mImei)
                        .setAppName(mAppName);

        if (mTokenType == TOKEN_TYPE_NORMAL) {
            builder.setAuthenticationToken(mAuthToken);
        } else if (mTokenType == TOKEN_TYPE_TEMPORARY) {
            builder.setTemporaryToken(mTemporaryToken);
        }

        ServiceEntitlementRequest request = builder.build();

        EsimOdsaOperation operation =
                EsimOdsaOperation.builder()
                        .setOperation(EsimOdsaOperation.OPERATION_MANAGE_SERVICE)
                        .setOperationType(manageServiceRequest.operationType())
                        .setCompanionTerminalId(manageServiceRequest.companionTerminalId())
                        .setCompanionTerminalVendor(manageServiceRequest.companionTerminalVendor())
                        .setCompanionTerminalModel(manageServiceRequest.companionTerminalModel())
                        .setCompanionTerminalSoftwareVersion(
                                manageServiceRequest.companionTerminalSoftwareVersion())
                        .setCompanionTerminalFriendlyName(
                                manageServiceRequest.companionTerminalFriendlyName())
                        .setCompanionTerminalService(
                                manageServiceRequest.companionTerminalService())
                        .setCompanionTerminalIccid(manageServiceRequest.companionTerminalIccid())
                        .build();

        String rawXml;
        try {
            rawXml =
                    mServiceEntitlement.performEsimOdsa(manageServiceRequest.appId(), request,
                            operation);
        } catch (ServiceEntitlementException e) {
            Log.w(TAG, "manageService: Failed to perform ODSA operation. e=" + e);
            throw e;
        }

        // Build the response of manage service operation. Refer to GSMA Service Entitlement
        // Configuration section 6.5.4.
        ManageServiceResponse.Builder responseBuilder = ManageServiceResponse.builder();

        Ts43XmlDoc ts43XmlDoc = new Ts43XmlDoc(rawXml);

        try {
            processGeneralResult(ts43XmlDoc, responseBuilder);
        } catch (MalformedURLException e) {
            throw new ServiceEntitlementException(
                    ServiceEntitlementException.ERROR_MALFORMED_HTTP_RESPONSE,
                    "manageService: Malformed URL " + rawXml);
        }

        // Parse service status.
        String serviceStatusString =
                ts43XmlDoc.get(
                        ImmutableList.of(Ts43XmlDoc.CharacteristicType.APPLICATION),
                        Ts43XmlDoc.Parm.SERVICE_STATUS);

        if (!TextUtils.isEmpty(serviceStatusString)) {
            responseBuilder.setServiceStatus(getServiceStatusFromString(serviceStatusString));
        }

        return responseBuilder.build();
    }

    /**
     * To provide service related data about a primary or companion device as described in GSMA
     * Service Entitlement Configuration section 6.2 and 6.5.5.
     *
     * @param acquireConfigurationRequest The acquire configuration request.
     * @return The response of acquire configuration request.
     * @throws ServiceEntitlementException The exception for error case. If it's an HTTP response
     *                                     error from the server, the error code can be retrieved by
     *                                     {@link ServiceEntitlementException#getHttpStatus()}
     */
    @NonNull
    public AcquireConfigurationResponse acquireConfiguration(
            @NonNull AcquireConfigurationRequest acquireConfigurationRequest)
            throws ServiceEntitlementException {
        Objects.requireNonNull(acquireConfigurationRequest);

        ServiceEntitlementRequest.Builder builder = ServiceEntitlementRequest.builder()
                .setEntitlementVersion(mEntitlementVersion)
                .setTerminalId(mImei)
                .setAppName(mAppName)
                .setAuthenticationToken(mAuthToken);

        String notificationToken = acquireConfigurationRequest.notificationToken();
        if (!TextUtils.isEmpty(notificationToken)) {
            builder.setNotificationToken(notificationToken);
        }
        int notificationAction = acquireConfigurationRequest.notificationAction();
        if (Ts43Constants.isValidNotificationAction(notificationAction)) {
            builder.setNotificationAction(notificationAction);
        }

        ServiceEntitlementRequest request = builder.build();

        EsimOdsaOperation operation =
                EsimOdsaOperation.builder()
                        .setOperation(EsimOdsaOperation.OPERATION_ACQUIRE_CONFIGURATION)
                        .setCompanionTerminalId(acquireConfigurationRequest.companionTerminalId())
                        .setCompanionTerminalIccid(
                                acquireConfigurationRequest.companionTerminalIccid())
                        .setCompanionTerminalEid(acquireConfigurationRequest.companionTerminalEid())
                        .setTerminalIccid(acquireConfigurationRequest.terminalIccid())
                        .setTerminalEid(acquireConfigurationRequest.terminalEid())
                        .setTargetTerminalId(acquireConfigurationRequest.targetTerminalId())
                        .setTargetTerminalIccid(acquireConfigurationRequest.targetTerminalIccid())
                        .setTargetTerminalEid(acquireConfigurationRequest.targetTerminalEid())
                        .build();

        String rawXml;
        try {
            rawXml =
                    mServiceEntitlement.performEsimOdsa(
                            acquireConfigurationRequest.appId(), request, operation);
        } catch (ServiceEntitlementException e) {
            Log.w(TAG, "acquireConfiguration: Failed to perform ODSA operation. e=" + e);
            throw e;
        }

        AcquireConfigurationResponse.Builder responseBuilder =
                AcquireConfigurationResponse.builder();
        AcquireConfigurationResponse.Configuration.Builder configBuilder =
                AcquireConfigurationResponse.Configuration.builder();

        Ts43XmlDoc ts43XmlDoc = new Ts43XmlDoc(rawXml);

        try {
            processGeneralResult(ts43XmlDoc, responseBuilder);
        } catch (MalformedURLException e) {
            throw new ServiceEntitlementException(
                    ServiceEntitlementException.ERROR_MALFORMED_HTTP_RESPONSE,
                    "manageSubscription: Malformed URL " + rawXml);
        }

        // Parse service status.
        String serviceStatusString =
                ts43XmlDoc.get(
                        ImmutableList.of(
                                Ts43XmlDoc.CharacteristicType.APPLICATION,
                                Ts43XmlDoc.CharacteristicType.PRIMARY_CONFIGURATION),
                        Ts43XmlDoc.Parm.SERVICE_STATUS);

        if (!TextUtils.isEmpty(serviceStatusString)) {
            configBuilder.setServiceStatus(getServiceStatusFromString(serviceStatusString));
        }

        // Parse ICCID
        String iccIdString =
                ts43XmlDoc.get(
                        ImmutableList.of(
                                Ts43XmlDoc.CharacteristicType.APPLICATION,
                                Ts43XmlDoc.CharacteristicType.PRIMARY_CONFIGURATION),
                        Ts43XmlDoc.Parm.ICCID);

        if (!TextUtils.isEmpty(iccIdString)) {
            configBuilder.setIccid(iccIdString);
        }

        // Parse polling interval
        String pollingIntervalString =
                ts43XmlDoc.get(
                        ImmutableList.of(
                                Ts43XmlDoc.CharacteristicType.APPLICATION,
                                Ts43XmlDoc.CharacteristicType.PRIMARY_CONFIGURATION),
                        Ts43XmlDoc.Parm.POLLING_INTERVAL);

        if (!TextUtils.isEmpty(pollingIntervalString)) {
            try {
                configBuilder.setPollingInterval(Integer.parseInt(pollingIntervalString));
            } catch (NumberFormatException e) {
                Log.w(
                        TAG, "acquireConfiguration: Failed to parse polling interval "
                                + pollingIntervalString);
            }
        }

        // Parse download info
        DownloadInfo downloadInfo =
                parseDownloadInfo(
                        ImmutableList.of(
                                Ts43XmlDoc.CharacteristicType.APPLICATION,
                                Ts43XmlDoc.CharacteristicType.PRIMARY_CONFIGURATION,
                                Ts43XmlDoc.CharacteristicType.DOWNLOAD_INFO),
                        ts43XmlDoc);
        if (downloadInfo != null) {
            configBuilder.setDownloadInfo(downloadInfo);
        }

        // Parse message info
        MessageInfo messageInfo =
                parseMessageInfo(
                        ImmutableList.of(
                                Ts43XmlDoc.CharacteristicType.APPLICATION,
                                Ts43XmlDoc.CharacteristicType.PRIMARY_CONFIGURATION,
                                Ts43XmlDoc.CharacteristicType.MSG),
                        ts43XmlDoc);
        if (messageInfo != null) {
            configBuilder.setMessageInfo(messageInfo);
        }

        // TODO: Support different type of configuration.
        configBuilder.setType(
                AcquireConfigurationResponse.Configuration.CONFIGURATION_TYPE_PRIMARY);

        // TODO: Support multiple configurations.
        return responseBuilder.setConfigurations(ImmutableList.of(configBuilder.build())).build();
    }

    /**
     * Acquire available mobile plans to be offered by the MNO to a specific user or MDM as
     * described in GSMA Service Entitlement Configuration section 6.2 and 6.5.6.
     *
     * @return List of mobile plans. Empty list if not available.
     * @throws ServiceEntitlementException The exception for error case. If it's an HTTP response
     *                                     error from the server, the error code can be retrieved by
     *                                     {@link ServiceEntitlementException#getHttpStatus()}
     */
    @NonNull
    public List<PlanOffer> acquirePlans() throws ServiceEntitlementException {
        return Collections.emptyList();
    }

    /**
     * To request a temporary token used to establish trust between ECS and the client as described
     * in GSMA Service Entitlement Configuration section 6.2 and 6.5.7.
     *
     * @param acquireTemporaryTokenRequest The acquire temporary token request.
     * @return The temporary token response.
     * @throws ServiceEntitlementException The exception for error case. If it's an HTTP response
     *                                     error from the server, the error code can be retrieved by
     *                                     {@link ServiceEntitlementException#getHttpStatus()}
     */
    @NonNull
    @SuppressWarnings("AndroidJdkLibsChecker") // java.time.Instant
    public AcquireTemporaryTokenResponse acquireTemporaryToken(
            @NonNull AcquireTemporaryTokenRequest acquireTemporaryTokenRequest)
            throws ServiceEntitlementException {
        Objects.requireNonNull(acquireTemporaryTokenRequest);

        ServiceEntitlementRequest request =
                ServiceEntitlementRequest.builder()
                        .setEntitlementVersion(mEntitlementVersion)
                        .setTerminalId(mImei)
                        .setAuthenticationToken(mAuthToken)
                        .setAppName(mAppName)
                        .build();

        EsimOdsaOperation operation =
                EsimOdsaOperation.builder()
                        .setOperation(EsimOdsaOperation.OPERATION_ACQUIRE_TEMPORARY_TOKEN)
                        .setOperationTargets(acquireTemporaryTokenRequest.operationTargets())
                        .setCompanionTerminalId(acquireTemporaryTokenRequest.companionTerminalId())
                        .build();

        String rawXml;
        try {
            rawXml =
                    mServiceEntitlement.performEsimOdsa(
                            acquireTemporaryTokenRequest.appId(), request, operation);
        } catch (ServiceEntitlementException e) {
            Log.w(TAG, "acquireTemporaryToken: Failed to perform ODSA operation. e=" + e);
            throw e;
        }

        Ts43XmlDoc ts43XmlDoc = new Ts43XmlDoc(rawXml);
        AcquireTemporaryTokenResponse.Builder responseBuilder =
                AcquireTemporaryTokenResponse.builder();

        try {
            processGeneralResult(ts43XmlDoc, responseBuilder);
        } catch (MalformedURLException e) {
            throw new ServiceEntitlementException(
                    ServiceEntitlementException.ERROR_MALFORMED_HTTP_RESPONSE,
                    "AcquireTemporaryTokenResponse: Malformed URL " + rawXml);
        }

        // Parse the operation targets.
        String operationTargets =
                Strings.nullToEmpty(
                        ts43XmlDoc.get(
                                ImmutableList.of(Ts43XmlDoc.CharacteristicType.APPLICATION),
                                Ts43XmlDoc.Parm.OPERATION_TARGETS));

        if (operationTargets != null) {
            List<String> operationTargetsList = Arrays.asList(operationTargets.split("\\s*,\\s*"));
            responseBuilder.setOperationTargets(ImmutableList.copyOf(operationTargetsList));
        }

        // Parse the temporary token
        String temporaryToken =
                ts43XmlDoc.get(
                        ImmutableList.of(Ts43XmlDoc.CharacteristicType.APPLICATION),
                        Ts43XmlDoc.Parm.TEMPORARY_TOKEN);

        if (temporaryToken == null) {
            throw new ServiceEntitlementException(
                    ServiceEntitlementException.ERROR_TOKEN_NOT_AVAILABLE,
                    "temporary token is not available.");
        }

        responseBuilder.setTemporaryToken(temporaryToken);

        String temporaryTokenExpiry =
                ts43XmlDoc.get(
                        ImmutableList.of(Ts43XmlDoc.CharacteristicType.APPLICATION),
                        Ts43XmlDoc.Parm.TEMPORARY_TOKEN_EXPIRY);

        // Parse the token expiration time.
        Instant expiry;
        try {
            expiry = OffsetDateTime.parse(temporaryTokenExpiry).toInstant();
            responseBuilder.setTemporaryTokenExpiry(expiry);
        } catch (DateTimeParseException e) {
            Log.w(TAG, "Failed to parse temporaryTokenExpiry: " + temporaryTokenExpiry);
        }

        return responseBuilder.build();
    }

    /**
     * Get the phone number as described in GSMA Service Entitlement Configuration section 6.2 and
     * 6.5.8.
     *
     * @param getPhoneNumberRequest The get phone number request.
     * @return The phone number response from the network.
     * @throws ServiceEntitlementException The exception for error case. If it's an HTTP response
     *                                     error from the server, the error code can be retrieved by
     *                                     {@link ServiceEntitlementException#getHttpStatus()}
     */
    @NonNull
    public GetPhoneNumberResponse getPhoneNumber(
            @NonNull GetPhoneNumberRequest getPhoneNumberRequest)
            throws ServiceEntitlementException {
        ServiceEntitlementRequest.Builder builder =
                ServiceEntitlementRequest.builder()
                        .setEntitlementVersion(mEntitlementVersion);

        if (!TextUtils.isEmpty(getPhoneNumberRequest.terminalId())) {
            builder.setTerminalId(getPhoneNumberRequest.terminalId());
        } else {
            builder.setTerminalId(mImei);
        }

        if (mTokenType == TOKEN_TYPE_NORMAL) {
            builder.setAuthenticationToken(mAuthToken);
        } else if (mTokenType == TOKEN_TYPE_TEMPORARY) {
            builder.setTemporaryToken(mTemporaryToken);
        }

        ServiceEntitlementRequest request = builder.setAppName(mAppName).build();

        EsimOdsaOperation operation =
                EsimOdsaOperation.builder()
                        .setOperation(EsimOdsaOperation.OPERATION_GET_PHONE_NUMBER)
                        .build();

        String rawXml;
        try {
            rawXml =
                    mServiceEntitlement.performEsimOdsa(
                        Ts43Constants.APP_PHONE_NUMBER_INFORMATION, request, operation);
        } catch (ServiceEntitlementException e) {
            Log.w(TAG, "getPhoneNumber: Failed to perform ODSA operation. e=" + e);
            throw e;
        }

        // Build the response of get phone number operation. Refer to GSMA Service Entitlement
        // Configuration section 6.5.8.
        GetPhoneNumberResponse.Builder responseBuilder = GetPhoneNumberResponse.builder();

        Ts43XmlDoc ts43XmlDoc = new Ts43XmlDoc(rawXml);

        try {
            processGeneralResult(ts43XmlDoc, responseBuilder);
        } catch (MalformedURLException e) {
            throw new ServiceEntitlementException(
                    ServiceEntitlementException.ERROR_MALFORMED_HTTP_RESPONSE,
                    "getPhoneNumber: Malformed URL " + rawXml);
        }

        // Parse msisdn.
        String msisdn =
                ts43XmlDoc.get(
                        ImmutableList.of(Ts43XmlDoc.CharacteristicType.APPLICATION),
                        Ts43XmlDoc.Parm.MSISDN);

        if (!TextUtils.isEmpty(msisdn)) {
            responseBuilder.setMsisdn(msisdn);
        }

        return responseBuilder.build();
    }

    /**
     * Get subscriber information (MSISDN, SIM identifiers, MVNO name).
     * Prefers JSON response; falls back to XML if needed.
     */
    @NonNull
    public GetSubscriberInfoResponse getSubscriberInfo(@NonNull GetSubscriberInfoRequest request) throws ServiceEntitlementException {
        // Build request preferring JSON
        ServiceEntitlementRequest.Builder serBuilder =
                ServiceEntitlementRequest.builder()
                        .setEntitlementVersion(mEntitlementVersion)
                        .setTerminalId(mImei)
                        .setAppName(mAppName)
                        .setAcceptContentType(ServiceEntitlementRequest.ACCEPT_CONTENT_TYPE_JSON);

        if (mTokenType == TOKEN_TYPE_NORMAL) {
            serBuilder.setAuthenticationToken(mAuthToken);
        } else if (mTokenType == TOKEN_TYPE_TEMPORARY) {
            serBuilder.setTemporaryToken(mTemporaryToken);
        }

        ServiceEntitlementRequest ser = serBuilder.build();

        EsimOdsaOperation operation =
                EsimOdsaOperation.builder()
                        .setOperation(EsimOdsaOperation.OPERATION_GET_SUBSCRIBER_INFO)
                        .setRequestorId(request.requestorId())
                        .build();

        String body;
        try {
            body =
                    mServiceEntitlement.performEsimOdsa(
                            Ts43Constants.APP_PHONE_NUMBER_INFORMATION, ser, operation);
        } catch (ServiceEntitlementException e) {
            Log.w(TAG, "getSubscriberInfo: Failed to perform ODSA operation. e=" + e);
            throw e;
        }

        GetSubscriberInfoResponse.Builder respBuilder = GetSubscriberInfoResponse.builder();

        Ts43XmlDoc ts43XmlDoc = new Ts43XmlDoc(body);
        try {
            processGeneralResult(ts43XmlDoc, respBuilder);
        } catch (MalformedURLException e) {
            throw new ServiceEntitlementException(
                    ServiceEntitlementException.ERROR_MALFORMED_HTTP_RESPONSE,
                    "getSubscriberInfo: Malformed URL " + body);
        }

        ImmutableList<String> path =
                ImmutableList.of(
                        Ts43XmlDoc.CharacteristicType.APPLICATION, "SubscriberInfo");
        String msisdn = ts43XmlDoc.get(path, "MSISDN");
        String simIdType = ts43XmlDoc.get(path, "SimIdType");
        String simId = ts43XmlDoc.get(path, "SimID");
        String mvno = ts43XmlDoc.get(path, "MvnoName");
        if (!TextUtils.isEmpty(msisdn)) respBuilder.setMsisdn(msisdn);
        if (!TextUtils.isEmpty(simIdType)) respBuilder.setSimIdType(simIdType);
        if (!TextUtils.isEmpty(simId)) respBuilder.setSimId(simId);
        if (!TextUtils.isEmpty(mvno)) respBuilder.setMvnoName(mvno);

        return respBuilder.build();
    }

    /**
     * Parse the download info from {@link ManageSubscriptionResponse}.
     *
     * @param characteristics The XML nodes to search activation code.
     * @param ts43XmlDoc The XML format http response.
     * @return The download info.
     */
    @Nullable
    @SuppressWarnings("AndroidJdkLibsChecker") // java.util.Base64
    private DownloadInfo parseDownloadInfo(
            @NonNull ImmutableList<String> characteristics, @NonNull Ts43XmlDoc ts43XmlDoc) {
        String activationCode =
                Strings.nullToEmpty(
                        ts43XmlDoc.get(characteristics, Ts43XmlDoc.Parm.PROFILE_ACTIVATION_CODE));
        String smdpAddress =
                Strings.nullToEmpty(
                        ts43XmlDoc.get(characteristics, Ts43XmlDoc.Parm.PROFILE_SMDP_ADDRESS));
        String iccid =
                Strings.nullToEmpty(ts43XmlDoc.get(characteristics, Ts43XmlDoc.Parm.PROFILE_ICCID));

        // DownloadInfo should contain either activationCode or smdpAddress + iccid
        if (!activationCode.isEmpty()) {
            // decode the activation code, which is in base64 format
            try {
                activationCode = new String(Base64.getDecoder().decode(activationCode));
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Failed to decode the activation code " + activationCode);
                return null;
            }
            return DownloadInfo.builder()
                    .setProfileActivationCode(activationCode)
                    .setProfileIccid(iccid)
                    .build();
        } else if (!smdpAddress.isEmpty() && !iccid.isEmpty()) {
            return DownloadInfo.builder()
                    .setProfileIccid(iccid)
                    .setProfileSmdpAddresses(
                            ImmutableList.copyOf(Arrays.asList(smdpAddress.split("\\s*,\\s*"))))
                    .build();
        } else {
            Log.w(
                    TAG,
                    "Failed to parse download info. activationCode="
                            + activationCode
                            + ", smdpAddress="
                            + smdpAddress
                            + ", iccid="
                            + iccid);
            return null;
        }
    }

    /**
     * Parse the MSG info from {@link AcquireConfigurationResponse}.
     *
     * @param characteristics The XML nodes to search.
     * @param ts43XmlDoc The XML format http response.
     * @return The MSG info.
     */
    @Nullable
    private MessageInfo parseMessageInfo(
            @NonNull ImmutableList<String> characteristics, @NonNull Ts43XmlDoc ts43XmlDoc) {
        String message =
                Strings.nullToEmpty(ts43XmlDoc.get(characteristics, Ts43XmlDoc.Parm.MESSAGE));
        String acceptButton =
                Strings.nullToEmpty(ts43XmlDoc.get(characteristics, Ts43XmlDoc.Parm.ACCEPT_BUTTON));
        String acceptButtonLabel =
                Strings.nullToEmpty(
                        ts43XmlDoc.get(characteristics, Ts43XmlDoc.Parm.ACCEPT_BUTTON_LABEL));
        String rejectButton =
                Strings.nullToEmpty(ts43XmlDoc.get(characteristics, Ts43XmlDoc.Parm.REJECT_BUTTON));
        String rejectButtonLabel =
                Strings.nullToEmpty(
                        ts43XmlDoc.get(characteristics, Ts43XmlDoc.Parm.REJECT_BUTTON_LABEL));
        String acceptFreetext =
                Strings.nullToEmpty(
                        ts43XmlDoc.get(characteristics, Ts43XmlDoc.Parm.ACCEPT_FREETEXT));

        // MessageInfo should contain message, accept button, reject button, and accept freetext
        if (!message.isEmpty() && !acceptButton.isEmpty() && !rejectButton.isEmpty()
                && !acceptFreetext.isEmpty()) {
            return MessageInfo.builder()
                    .setMessage(message)
                    .setAcceptButton(acceptButton)
                    .setAcceptButtonLabel(acceptButtonLabel)
                    .setRejectButton(rejectButton)
                    .setRejectButtonLabel(rejectButtonLabel)
                    .setAcceptFreetext(acceptFreetext)
                    .build();
        } else {
            Log.w(
                    TAG,
                    "Failed to parse message info. message="
                            + message
                            + ", acceptButton="
                            + acceptButton
                            + ", acceptButtonLabel="
                            + acceptButtonLabel
                            + ", rejectButton="
                            + rejectButton
                            + ", rejectButtonLabel="
                            + rejectButtonLabel
                            + ", acceptFreetext="
                            + acceptFreetext);
            return null;
        }
    }

    /**
     * Process the common ODSA result from HTTP response.
     *
     * @param ts43XmlDoc The TS.43 ODSA operation response in XLM format.
     * @param builder The response builder.
     * @throws MalformedURLException when HTTP response is not well formatted.
     */
    private void processGeneralResult(
            @NonNull Ts43XmlDoc ts43XmlDoc, @NonNull OdsaResponse.Builder builder)
            throws MalformedURLException {
        // Now start to parse the result from HTTP response.
        // Parse the operation result.
        String operationResult =
                ts43XmlDoc.get(
                        ImmutableList.of(Ts43XmlDoc.CharacteristicType.APPLICATION),
                        Ts43XmlDoc.Parm.OPERATION_RESULT);

        builder.setOperationResult(EsimOdsaOperation.OPERATION_RESULT_UNKNOWN);
        if (!TextUtils.isEmpty(operationResult)) {
            switch (operationResult) {
                case Ts43XmlDoc.ParmValues.OPERATION_RESULT_SUCCESS:
                    builder.setOperationResult(EsimOdsaOperation.OPERATION_RESULT_SUCCESS);
                    break;
                case Ts43XmlDoc.ParmValues.OPERATION_RESULT_ERROR_GENERAL:
                    builder.setOperationResult(EsimOdsaOperation.OPERATION_RESULT_ERROR_GENERAL);
                    break;
                case Ts43XmlDoc.ParmValues.OPERATION_RESULT_ERROR_INVALID_OPERATION:
                    builder.setOperationResult(
                            EsimOdsaOperation.OPERATION_RESULT_ERROR_INVALID_OPERATION);
                    break;
                case Ts43XmlDoc.ParmValues.OPERATION_RESULT_ERROR_INVALID_PARAMETER:
                    builder.setOperationResult(
                            EsimOdsaOperation.OPERATION_RESULT_ERROR_INVALID_PARAMETER);
                    break;
                case Ts43XmlDoc.ParmValues.OPERATION_RESULT_WARNING_NOT_SUPPORTED_OPERATION:
                    builder.setOperationResult(
                            EsimOdsaOperation.OPERATION_RESULT_WARNING_NOT_SUPPORTED_OPERATION);
                    break;
                case Ts43XmlDoc.ParmValues.OPERATION_RESULT_ERROR_INVALID_MSG_RESPONSE:
                    builder.setOperationResult(
                            EsimOdsaOperation.OPERATION_RESULT_ERROR_INVALID_MSG_RESPONSE);
                    break;
            }
        }

        // Parse the general error URL
        String generalErrorUrl =
                ts43XmlDoc.get(
                        ImmutableList.of(Ts43XmlDoc.CharacteristicType.APPLICATION),
                        Ts43XmlDoc.Parm.GENERAL_ERROR_URL);
        if (!TextUtils.isEmpty(generalErrorUrl)) {
            builder.setGeneralErrorUrl(new URL(generalErrorUrl));
        }

        // Parse the general error URL user data
        String generalErrorUserData =
                ts43XmlDoc.get(
                        ImmutableList.of(Ts43XmlDoc.CharacteristicType.APPLICATION),
                        Ts43XmlDoc.Parm.GENERAL_ERROR_USER_DATA);
        if (!TextUtils.isEmpty(generalErrorUserData)) {
            builder.setGeneralErrorUserData(generalErrorUserData);
        }

        // Parse the general error text
        String generalErrorText =
                ts43XmlDoc.get(
                        ImmutableList.of(Ts43XmlDoc.CharacteristicType.APPLICATION),
                        Ts43XmlDoc.Parm.GENERAL_ERROR_TEXT);
        if (!TextUtils.isEmpty(generalErrorText)) {
            builder.setGeneralErrorText(generalErrorText);
        }

        // Parse the token for next operation.
        String token =
                ts43XmlDoc.get(
                        ImmutableList.of(Ts43XmlDoc.CharacteristicType.TOKEN),
                        Ts43XmlDoc.Parm.TOKEN);
        if (!TextUtils.isEmpty(token)) {
            // Some servers issue the new token in operation result for next operation to use.
            // We need to save it.
            mAuthToken = token;
            Log.d(TAG, "processGeneralResult: Token replaced.");
        }
    }

    /**
     * Get the service status from string as described in GSMA Service Entitlement Configuration
     * section 6.5.4.
     *
     * @param serviceStatusString Service status in string format defined in GSMA Service
     *                            Entitlement Configuration section 6.5.4.
     * @return The converted service status. {@link EsimOdsaOperation#SERVICE_STATUS_UNKNOWN} if not
     * able to convert.
     */
    @OdsaServiceStatus
    private int getServiceStatusFromString(@NonNull String serviceStatusString) {
        switch (serviceStatusString) {
            case Ts43XmlDoc.ParmValues.SERVICE_STATUS_ACTIVATED:
                return EsimOdsaOperation.SERVICE_STATUS_ACTIVATED;
            case Ts43XmlDoc.ParmValues.SERVICE_STATUS_ACTIVATING:
                return EsimOdsaOperation.SERVICE_STATUS_ACTIVATING;
            case Ts43XmlDoc.ParmValues.SERVICE_STATUS_DEACTIVATED:
                return EsimOdsaOperation.SERVICE_STATUS_DEACTIVATED;
            case Ts43XmlDoc.ParmValues.SERVICE_STATUS_DEACTIVATED_NO_REUSE:
                return EsimOdsaOperation.SERVICE_STATUS_DEACTIVATED_NO_REUSE;
        }
        return EsimOdsaOperation.SERVICE_STATUS_UNKNOWN;
    }
}
