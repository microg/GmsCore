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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.android.libraries.entitlement.eapaka.EapAkaApi;
import com.android.libraries.entitlement.http.HttpResponse;
import com.android.libraries.entitlement.utils.DebugUtils;
import com.android.libraries.entitlement.utils.Ts43Constants;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;

/**
 * Implements protocol for carrier service entitlement configuration query and operation, based on
 * GSMA TS.43 spec.
 */
public class ServiceEntitlement {
    /**
     * App ID for Voice-Over-LTE entitlement.
     */
    public static final String APP_VOLTE = Ts43Constants.APP_VOLTE;
    /**
     * App ID for Voice-Over-WiFi entitlement.
     */
    public static final String APP_VOWIFI = Ts43Constants.APP_VOWIFI;
    /**
     * App ID for SMS-Over-IP entitlement.
     */
    public static final String APP_SMSOIP = Ts43Constants.APP_SMSOIP;
    /**
     * App ID for on device service activation (ODSA) for companion device.
     */
    public static final String APP_ODSA_COMPANION = Ts43Constants.APP_ODSA_COMPANION;
    /**
     * App ID for on device service activation (ODSA) for primary device.
     */
    public static final String APP_ODSA_PRIMARY = Ts43Constants.APP_ODSA_PRIMARY;
    /**
     * App ID for data plan information entitlement.
     */
    public static final String APP_DATA_PLAN_BOOST = Ts43Constants.APP_DATA_PLAN_BOOST;

    /**
     * App ID for server initiated requests, entitlement and activation.
     */
    public static final String APP_ODSA_SERVER_INITIATED_REQUESTS =
            Ts43Constants.APP_ODSA_SERVER_INITIATED_REQUESTS;

    /**
     * App ID for direct carrier billing.
     */
    public static final String APP_DIRECT_CARRIER_BILLING =
            Ts43Constants.APP_DIRECT_CARRIER_BILLING;

    /**
     * App ID for private user identity.
     */
    public static final String APP_PRIVATE_USER_IDENTITY = Ts43Constants.APP_PRIVATE_USER_IDENTITY;

    /**
     * App ID for phone number information.
     */
    public static final String APP_PHONE_NUMBER_INFORMATION =
            Ts43Constants.APP_PHONE_NUMBER_INFORMATION;

    /**
     * App ID for satellite entitlement.
     */
    public static final String APP_SATELLITE_ENTITLEMENT = Ts43Constants.APP_SATELLITE_ENTITLEMENT;

    /**
     * App ID for ODSA for Cross-TS.43 platform device, Entitlement and Activation.
     */
    public static final String APP_ODSA_CROSS_TS43 = Ts43Constants.APP_ODSA_CROSS_TS43;

    private final CarrierConfig carrierConfig;
    private final EapAkaApi eapAkaApi;
    private ServiceEntitlementRequest mOidcRequest;
    /**
     * Creates an instance for service entitlement configuration query and operation for the
     * carrier.
     *
     * @param context           context of application
     * @param carrierConfig     carrier specific configs used in the queries and operations.
     * @param simSubscriptionId the subscription ID of the carrier's SIM on device. This indicates
     *                          which SIM to retrieve IMEI/IMSI from and perform EAP-AKA
     *                          authentication with. See
     *                          {@link android.telephony.SubscriptionManager}
     *                          for how to get the subscription ID.
     */
    public ServiceEntitlement(Context context, CarrierConfig carrierConfig, int simSubscriptionId) {
        this(context, carrierConfig, simSubscriptionId, /* saveHttpHistory= */ false);
    }

    /**
     * Creates an instance for service entitlement configuration query and operation for the
     * carrier.
     *
     * @param context context of application
     * @param carrierConfig carrier specific configs used in the queries and operations.
     * @param simSubscriptionId the subscription ID of the carrier's SIM on device. This indicates
     *     which SIM to retrieve IMEI/IMSI from and perform EAP-AKA authentication with. See {@link
     *     android.telephony.SubscriptionManager} for how to get the subscription ID.
     * @param saveHttpHistory set to {@code true} to save the history of request and response which
     *     can later be retrieved by {@code getHistory()}. Intended for debugging.
     */
    public ServiceEntitlement(
            Context context,
            CarrierConfig carrierConfig,
            int simSubscriptionId,
            boolean saveHttpHistory) {
        this(
                context,
                carrierConfig,
                simSubscriptionId,
                saveHttpHistory,
                DebugUtils.getBypassEapAkaResponse());
    }

    /**
     * Creates an instance for service entitlement configuration query and operation for the
     * carrier.
     *
     * @param context context of application
     * @param carrierConfig carrier specific configs used in the queries and operations.
     * @param simSubscriptionId the subscription ID of the carrier's SIM on device. This indicates
     *     which SIM to retrieve IMEI/IMSI from and perform EAP-AKA authentication with. See {@link
     *     android.telephony.SubscriptionManager} for how to get the subscription ID.
     * @param saveHttpHistory set to {@code true} to save the history of request and response which
     *     can later be retrieved by {@code getHistory()}. Intended for debugging.
     * @param bypassEapAkaResponse set to non empty string to bypass EAP-AKA authentication.
     *     The client will accept any challenge from the server and return this string as a
     *     response. Must not be {@code null}. Intended for testing.
     */
    public ServiceEntitlement(
            Context context,
            CarrierConfig carrierConfig,
            int simSubscriptionId,
            boolean saveHttpHistory,
            String bypassEapAkaResponse) {
        this.carrierConfig = carrierConfig;
        this.eapAkaApi =
                new EapAkaApi(context, simSubscriptionId, saveHttpHistory, bypassEapAkaResponse);
    }

    @VisibleForTesting
    ServiceEntitlement(CarrierConfig carrierConfig, EapAkaApi eapAkaApi) {
        this.carrierConfig = carrierConfig;
        this.eapAkaApi = eapAkaApi;
    }

    /**
     * Retrieves service entitlement configuration. For on device service activation (ODSA) of eSIM
     * for companion/primary devices, use {@link #performEsimOdsa} instead.
     *
     * <p>Supported {@code appId}: {@link #APP_VOLTE}, {@link #APP_VOWIFI}, {@link #APP_SMSOIP}.
     *
     * <p>This method sends an HTTP GET request to entitlement server, responds to EAP-AKA
     * challenge if needed, and returns the raw configuration doc as a string. The following
     * parameters are set in the HTTP request:
     *
     * <ul>
     * <li>"app": {@code appId}
     * <li>"vers": 0, or {@code request.configurationVersion()} if it's not 0.
     * <li>"entitlement_version": "2.0", or {@code request.entitlementVersion()} if it's not empty.
     * <li>"token": not set, or {@code request.authenticationToken()} if it's not empty.
     * <li>"IMSI": if "token" is set, set to {@link android.telephony.TelephonyManager#getImei}.
     * <li>"EAP_ID": if "token" is not set, set this parameter to trigger embedded EAP-AKA
     * authentication as described in TS.43 section 2.6.1. Its value is derived from IMSI as per
     * GSMA spec RCC.14 section C.2.
     * <li>"terminal_id": IMEI, or {@code request.terminalId()} if it's not empty.
     * <li>"terminal_vendor": {@link android.os.Build#MANUFACTURER}, or {@code
     * request.terminalVendor()} if it's not empty.
     * <li>"terminal_model": {@link android.os.Build#MODEL}, or {@code request.terminalModel()} if
     * it's not empty.
     * <li>"terminal_sw_version": {@link android.os.Build.VERSION#BASE_OS}, or {@code
     * request.terminalSoftwareVersion()} if it's not empty.
     * <li>"app_name": not set, or {@code request.appName()} if it's not empty.
     * <li>"app_version": not set, or {@code request.appVersion()} if it's not empty.
     * <li>"notif_token": not set, or {@code request.notificationToken()} if it's not empty.
     * <li>"notif_action": {@code request.notificationAction()} if "notif_token" is set, otherwise
     * not set.
     * </ul>
     *
     * <p>Requires permission: READ_PRIVILEGED_PHONE_STATE, or carrier privilege.
     *
     * @param appId   an app ID string defined in TS.43 section 2.2, e.g. {@link #APP_VOWIFI}.
     * @param request contains parameters that can be used in the HTTP request.
     */
    @NonNull
    public String queryEntitlementStatus(String appId, ServiceEntitlementRequest request)
            throws ServiceEntitlementException {
        return queryEntitlementStatus(ImmutableList.of(appId), request);
    }

    /**
     * Retrieves service entitlement configurations for multiple app IDs in one HTTP
     * request/response. For on device service activation (ODSA) of eSIM for companion/primary
     * devices, use {@link #performEsimOdsa} instead.
     *
     * <p>Same as {@link #queryEntitlementStatus(String, ServiceEntitlementRequest)} except that
     * multiple "app" parameters will be set in the HTTP request, in the order as they appear in
     * parameter {@code appIds}.
     */
    @NonNull
    public String queryEntitlementStatus(
            ImmutableList<String> appIds, ServiceEntitlementRequest request)
            throws ServiceEntitlementException {
        return getEntitlementStatusResponse(appIds, request).body();
    }

    /**
     * Retrieves service entitlement configurations for multiple app IDs in one HTTP
     * request/response. For on device service activation (ODSA) of eSIM for companion/primary
     * devices, use {@link #performEsimOdsa} instead.
     *
     * <p>Same as {@link #queryEntitlementStatus(String, ServiceEntitlementRequest)} except that
     * multiple "app" parameters will be set in the HTTP request, in the order as they appear in
     * parameter {@code appIds}. Additional parameters from {@code additionalHeaders} are set to the
     * HTTP request.
     */
    @NonNull
    public String queryEntitlementStatus(
            ImmutableList<String> appIds,
            ServiceEntitlementRequest request,
            ImmutableMap<String, String> additionalHeaders)
            throws ServiceEntitlementException {
        return getEntitlementStatusResponse(appIds, request, additionalHeaders).body();
    }

    /**
     * Retrieves service entitlement configurations for multiple app IDs in one HTTP
     * request/response. For on device service activation (ODSA) of eSIM for companion/primary
     * devices, use {@link #performEsimOdsa} instead.
     *
     * <p>Same as {@link #queryEntitlementStatus(ImmutableList, ServiceEntitlementRequest)}
     * except that it returns the full HTTP response instead of just the body.
     */
    @NonNull
    public HttpResponse getEntitlementStatusResponse(ImmutableList<String> appIds,
            ServiceEntitlementRequest request)
            throws ServiceEntitlementException {
        return getEntitlementStatusResponse(appIds, request, ImmutableMap.of());
    }

    /**
     * Retrieves service entitlement configurations for multiple app IDs in one HTTP
     * request/response. For on device service activation (ODSA) of eSIM for companion/primary
     * devices, use {@link #performEsimOdsa} instead.
     */
    @NonNull
    public HttpResponse getEntitlementStatusResponse(
            ImmutableList<String> appIds,
            ServiceEntitlementRequest request,
            ImmutableMap<String, String> additionalHeaders)
            throws ServiceEntitlementException {
        return eapAkaApi.queryEntitlementStatus(appIds, carrierConfig, request, additionalHeaders);
    }

    /**
     * Performs on device service activation (ODSA) of eSIM for companion/primary devices.
     *
     * <p>Supported {@code appId}: {@link #APP_ODSA_COMPANION}, {@link #APP_ODSA_PRIMARY}.
     *
     * <p>Similar to {@link #queryEntitlementStatus(String, ServiceEntitlementRequest)}, this method
     * sends an HTTP GET request to entitlement server, responds to EAP-AKA challenge if needed, and
     * returns the raw configuration doc as a string. Additional parameters from {@code operation}
     * are set to the HTTP request. See {@link EsimOdsaOperation} for details.
     */
    @NonNull
    public String performEsimOdsa(
            String appId, ServiceEntitlementRequest request, EsimOdsaOperation operation)
            throws ServiceEntitlementException {
        return performEsimOdsa(appId, request, operation, ImmutableMap.of());
    }

    /**
     * Performs on device service activation (ODSA) of eSIM for companion/primary devices.
     *
     * <p>Supported {@code appId}: {@link #APP_ODSA_COMPANION}, {@link #APP_ODSA_PRIMARY}.
     *
     * <p>Similar to {@link #queryEntitlementStatus(String, ServiceEntitlementRequest)}, this method
     * sends an HTTP GET request to entitlement server, responds to EAP-AKA challenge if needed, and
     * returns the raw configuration doc as a string. Additional parameters from {@code operation}
     * are set to the HTTP request. See {@link EsimOdsaOperation} for details. Additional parameters
     * from {@code additionalHeaders} are set to the HTTP request.
     */
    @NonNull
    public String performEsimOdsa(
            String appId,
            ServiceEntitlementRequest request,
            EsimOdsaOperation operation,
            ImmutableMap<String, String> additionalHeaders)
            throws ServiceEntitlementException {
        return getEsimOdsaResponse(appId, request, operation, additionalHeaders).body();
    }

    /**
     * Retrieves the HTTP response after performing on device service activation (ODSA) of eSIM for
     * companion/primary devices.
     *
     * <p>Same as {@link #performEsimOdsa(String, ServiceEntitlementRequest, EsimOdsaOperation)}
     * except that it returns the full HTTP response instead of just the body.
     */
    @NonNull
    public HttpResponse getEsimOdsaResponse(
            String appId, ServiceEntitlementRequest request, EsimOdsaOperation operation)
            throws ServiceEntitlementException {
        return getEsimOdsaResponse(appId, request, operation, ImmutableMap.of());
    }

    /**
     * Retrieves the HTTP response after performing on device service activation (ODSA) of eSIM for
     * companion/primary devices.
     *
     * <p>Same as {@link #performEsimOdsa(String, ServiceEntitlementRequest, EsimOdsaOperation)}
     * except that it returns the full HTTP response instead of just the body. Additional parameters
     * from {@code additionalHeaders} are set to the HTTP request.
     */
    @NonNull
    public HttpResponse getEsimOdsaResponse(
            String appId,
            ServiceEntitlementRequest request,
            EsimOdsaOperation operation,
            ImmutableMap<String, String> additionalHeaders)
            throws ServiceEntitlementException {
        return eapAkaApi.performEsimOdsaOperation(
                appId, carrierConfig, request, operation, additionalHeaders);
    }

    /**
     * Retrieves the endpoint for OpenID Connect(OIDC) authentication.
     *
     * <p>Implementation based on section 2.8.2 of TS.43
     *
     * <p>The user should call {@link #queryEntitlementStatusFromOidc(String url)} with the
     * authentication result to retrieve the service entitlement configuration.
     *
     * @param appId an app ID string defined in TS.43 section 2.2
     * @param request contains parameters that can be used in the HTTP request
     */
    @NonNull
    public String acquireOidcAuthenticationEndpoint(String appId, ServiceEntitlementRequest request)
            throws ServiceEntitlementException {
        return acquireOidcAuthenticationEndpoint(appId, request, ImmutableMap.of());
    }

    /**
     * Retrieves the endpoint for OpenID Connect(OIDC) authentication.
     *
     * <p>Implementation based on section 2.8.2 of TS.43
     *
     * <p>The user should call {@link #queryEntitlementStatusFromOidc(String url)} with the
     * authentication result to retrieve the service entitlement configuration.
     *
     * @param appId an app ID string defined in TS.43 section 2.2
     * @param request contains parameters that can be used in the HTTP request
     * @param additionalHeaders additional headers to be set in the HTTP request
     */
    @NonNull
    public String acquireOidcAuthenticationEndpoint(
            String appId,
            ServiceEntitlementRequest request,
            ImmutableMap<String, String> additionalHeaders)
            throws ServiceEntitlementException {
        mOidcRequest = request;
        return eapAkaApi.acquireOidcAuthenticationEndpoint(
                appId, carrierConfig, request, additionalHeaders);
    }

    /**
     * Retrieves the service entitlement configuration from OIDC authentication result.
     *
     * <p>Implementation based on section 2.8.2 of TS.43.
     *
     * <p>{@link #acquireOidcAuthenticationEndpoint} must be called before calling this method.
     *
     * @param url the redirect url from OIDC authentication result.
     */
    @NonNull
    public String queryEntitlementStatusFromOidc(String url) throws ServiceEntitlementException {
        return getEntitlementStatusResponseFromOidc(url).body();
    }

    /**
     * Retrieves the HTTP response containing the service entitlement configuration from
     * OIDC authentication result.
     *
     * <p>Same as {@link #queryEntitlementStatusFromOidc(String)} except that it returns the
     * full HTTP response instead of just the body.
     *
     * @param url the redirect url from OIDC authentication result.
     */
    @NonNull
    public HttpResponse getEntitlementStatusResponseFromOidc(String url)
            throws ServiceEntitlementException {
        return getEntitlementStatusResponseFromOidc(url, ImmutableMap.of());
    }

    /**
     * Retrieves the HTTP response containing the service entitlement configuration from OIDC
     * authentication result.
     *
     * <p>Same as {@link #queryEntitlementStatusFromOidc(String)} except that it returns the full
     * HTTP response instead of just the body.
     *
     * @param url the redirect url from OIDC authentication result.
     * @param additionalHeaders additional headers to be set in the HTTP request
     */
    @NonNull
    public HttpResponse getEntitlementStatusResponseFromOidc(
            String url, ImmutableMap<String, String> additionalHeaders)
            throws ServiceEntitlementException {
        return eapAkaApi.queryEntitlementStatusFromOidc(
                url, carrierConfig, mOidcRequest, additionalHeaders);
    }

    /**
     * Retrieves the history of past HTTP request and responses if {@code saveHttpHistory} was set
     * in constructor.
     */
    @NonNull
    public List<String> getHistory() {
        return eapAkaApi.getHistory();
    }

    /**
     * Clears the history of past HTTP request and responses.
     */
    public void clearHistory() {
        eapAkaApi.clearHistory();
    }
}
