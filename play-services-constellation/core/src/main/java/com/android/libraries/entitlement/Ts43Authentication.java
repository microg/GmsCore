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

import static com.google.common.base.Strings.nullToEmpty;

import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.libraries.entitlement.http.HttpResponse;
import com.android.libraries.entitlement.utils.Ts43Constants;
import com.android.libraries.entitlement.utils.Ts43Constants.AppId;
import com.android.libraries.entitlement.utils.Ts43XmlDoc;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.net.URL;
import java.util.Objects;

/**
 * The class responsible for TS.43 authentication process.
 */
public class Ts43Authentication {
    private static final String TAG = "Ts43Auth";

    /**
     * The authentication token for TS.43 operation.
     */
    @AutoValue
    public abstract static class Ts43AuthToken {
        /**
         * Indicating the validity of token is not available.
         */
        public static long VALIDITY_NOT_AVAILABLE = -1;

        /**
         * The authentication token for TS.43 operations.
         */
        @NonNull
        public abstract String token();

        /**
         * The list of cookies from the {@code Set-Cookie} header of the TS.43 response.
         */
        @NonNull
        public abstract ImmutableList<String> cookies();

        /**
         * Indicates the validity of the token. Note this value is server dependent. The client is
         * expected to interpret this value itself.
         */
        public abstract long validity();

        /**
         * Create the {@link Ts43AuthToken} object.
         *
         * @param token The authentication token for TS.43 operations.
         * @param cookie The list of cookies from the {@code Set-Cookie} header.
         * @param validity Indicates the validity of the token. Note this value is server
         * dependent. If not available, set to {@link #VALIDITY_NOT_AVAILABLE}.
         *
         * @return The {@link Ts43AuthToken} object.
         */
        public static Ts43AuthToken create(@NonNull String token,
                @NonNull ImmutableList<String> cookie, long validity) {
            return new AutoValue_Ts43Authentication_Ts43AuthToken(token, cookie, validity);
        }
    }

    /**
     * The application context.
     */
    @NonNull
    private final Context mContext;

    /**
     * The entitlement server address.
     */
    @NonNull
    private final URL mEntitlementServerAddress;

    /**
     * The TS.43 entitlement version to use. For example, {@code "9.0"}.
     */
    @NonNull
    private final String mEntitlementVersion;

    /**
     * For test mocking only.
     */
    @VisibleForTesting
    private ServiceEntitlement mServiceEntitlement;

    /**
     * Ts43Authentication constructor.
     *
     * @param context The application context.
     * @param entitlementServerAddress The entitlement server address.
     * @param entitlementVersion The TS.43 entitlement version to use. For example, {@code "9.0"}.
     * If {@code null}, version {@code "2.0"} will be used by default.
     *
     * @throws NullPointerException wWhen {@code context} or {@code entitlementServerAddress} is
     * {@code null}.
     */
    public Ts43Authentication(@NonNull Context context, @NonNull URL entitlementServerAddress,
            @Nullable String entitlementVersion) {
        Objects.requireNonNull(context, "context is null");
        Objects.requireNonNull(entitlementServerAddress, "entitlementServerAddress is null.");

        mContext = context;
        mEntitlementServerAddress = entitlementServerAddress;

        if (entitlementVersion != null) {
            mEntitlementVersion = entitlementVersion;
        } else {
            mEntitlementVersion = Ts43Constants.DEFAULT_ENTITLEMENT_VERSION;
        }
    }

    /**
     * Get the authentication token for TS.43 operations with EAP-AKA described in TS.43
     * Service Entitlement Configuration section 2.8.1.
     *
     * @param slotIndex The logical SIM slot index involved in ODSA operation.
     * See {@link SubscriptionInfo#getSubscriptionId()}.

     * @param appId Application id. For example, {@link Ts43Constants#APP_VOWIFI} for VoWifi,
     * {@link Ts43Constants#APP_ODSA_PRIMARY} for ODSA primary device. Refer GSMA to Service
     * Entitlement Configuration section 2.3.
     * @param appName The calling client's package name. Used for {@code app_name} in HTTP GET
     * request in GSMA TS.43 Service Entitlement Configuration section 2.3.
     * @param appVersion The calling client's version. Used for {@code app_version} in HTTP GET
     * request in GSMA TS.43 Service Entitlement Configuration section 2.3.
     *
     * @return The authentication token.
     *
     * @throws ServiceEntitlementException The exception for error case. If it's an HTTP response
     * error from the server, the error code can be retrieved by
     * {@link ServiceEntitlementException#getHttpStatus()}.
     * @throws IllegalArgumentException when {@code slotIndex} or {@code appId} is invalid.
     * @throws NullPointerException when {@code context}, {@code entitlementServerAddress}, or
     * {@code appId} is {@code null}.
     */
    @NonNull
    public Ts43AuthToken getAuthToken(int slotIndex, @NonNull @AppId String appId,
            @Nullable String appName, @Nullable String appVersion)
            throws ServiceEntitlementException {
        Objects.requireNonNull(appId, "appId is null");

        if (!Ts43Constants.isValidAppId(appId)) {
            throw new IllegalArgumentException("getAuthToken: invalid app id " + appId);
        }

        String imei = null;
        TelephonyManager telephonyManager = mContext.getSystemService(TelephonyManager.class);
        if (telephonyManager != null) {
            if (slotIndex < 0 || slotIndex >= telephonyManager.getActiveModemCount()) {
                throw new IllegalArgumentException("getAuthToken: invalid slot index " + slotIndex);
            }
            imei = telephonyManager.getImei(slotIndex);
        }

        // Build the HTTP request. The default params are specified in
        // ServiceEntitlementRequest.builder() already.
        ServiceEntitlementRequest request =
                ServiceEntitlementRequest.builder()
                        .setEntitlementVersion(mEntitlementVersion)
                        .setTerminalId(imei)
                        .setAppName(appName)
                        .setAppVersion(appVersion)
                        .build();
        CarrierConfig carrierConfig = CarrierConfig.builder()
                .setServerUrl(mEntitlementServerAddress.toString())
                .build();

        if (mServiceEntitlement == null) {
            int subId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
            if (Build.VERSION.SDK_INT < 34) {
                SubscriptionManager subscriptionManager =
                        mContext.getSystemService(SubscriptionManager.class);
                int[] subIds = subscriptionManager.getSubscriptionIds(slotIndex);
                if (subIds != null && subIds.length > 0) {
                    subId = subIds[0];
                }
            } else {
                subId = SubscriptionManager.getSubscriptionId(slotIndex);
            }
            mServiceEntitlement = new ServiceEntitlement(mContext, carrierConfig, subId);
        }

        // Get the full HTTP response instead of just the body so we can reuse the same cookies.
        HttpResponse response;
        String rawXml;
        try {
            response = mServiceEntitlement.getEntitlementStatusResponse(
                    ImmutableList.of(appId), request);
            rawXml = response == null ? null : response.body();
            Log.d(TAG, "getAuthToken: rawXml=" + rawXml);
        } catch (ServiceEntitlementException e) {
            Log.w(TAG, "Failed to get authentication token. e=" + e);
            throw e;
        }

        ImmutableList<String> cookies = response == null ? ImmutableList.of() : response.cookies();

        Ts43XmlDoc ts43XmlDoc = new Ts43XmlDoc(rawXml);
        String authToken = ts43XmlDoc.get(
                ImmutableList.of(Ts43XmlDoc.CharacteristicType.TOKEN), Ts43XmlDoc.Parm.TOKEN);
        if (TextUtils.isEmpty(authToken)) {
            Log.w(TAG, "Failed to parse authentication token");
            throw new ServiceEntitlementException(
                    ServiceEntitlementException.ERROR_TOKEN_NOT_AVAILABLE,
                    "Failed to parse authentication token");
        }

        String validityString = nullToEmpty(ts43XmlDoc.get(ImmutableList.of(
                Ts43XmlDoc.CharacteristicType.TOKEN), Ts43XmlDoc.Parm.VALIDITY));
        long validity;
        try {
            validity = Long.parseLong(validityString);
        } catch (NumberFormatException e) {
            validity = Ts43AuthToken.VALIDITY_NOT_AVAILABLE;
        }

        return Ts43AuthToken.create(authToken, cookies, validity);
    }

    /**
     * Get the URL of OIDC (OpenID Connect) server as described in TS.43 Service Entitlement
     * Configuration section 2.8.2.
     *
     * The caller is expected to present the content of the URL to the user to proceed the
     * authentication process. After that the caller can call {@link #getAuthToken(URL)}
     * to get the authentication token.
     *
     * @param slotIndex The logical SIM slot index involved in ODSA operation.
     * @param entitlementServerAddress The entitlement server address.
     * @param entitlementVersion The TS.43 entitlement version to use. For example, {@code "9.0"}.
     * @param appId Application id. For example, {@link Ts43Constants#APP_VOWIFI} for VoWifi,
     * {@link Ts43Constants#APP_ODSA_PRIMARY} for ODSA primary device. Refer GSMA to Service
     * Entitlement Configuration section 2.3.
     * @param appName The calling client's package name. Used for {@code app_name} in HTTP GET
     * request in GSMA TS.43 Service Entitlement Configuration section 2.3.
     * @param appVersion The calling client's version. Used for {@code app_version} in HTTP GET
     * request in GSMA TS.43 Service Entitlement Configuration section 2.3.
     *
     * @return The URL of OIDC server with all the required parameters for client to launch a
     * user interface for users to interact with the authentication process. The parameters in URL
     * include {@code client_id}, {@code redirect_uri}, {@code state}, and {@code nonce}.
     *
     * @throws ServiceEntitlementException The exception for error case. If it's an HTTP response
     * error from the server, the error code can be retrieved by
     * {@link ServiceEntitlementException#getHttpStatus()}
     */
    @NonNull
    public URL getOidcAuthServer(@NonNull Context context, int slotIndex,
            @NonNull URL entitlementServerAddress, @Nullable String entitlementVersion,
            @NonNull @AppId String appId, @Nullable String appName, @Nullable String appVersion)
            throws ServiceEntitlementException {
        return null;
    }

    /**
     * Get the authentication token for TS.43 operations with OIDC (OpenID Connect) described in
     * TS.43 Service Entitlement Configuration section 2.8.2.
     *
     * @param aesUrl The AES URL used to retrieve auth token. The parameters in the URL include
     * the OIDC auth code {@code code} and {@code state}.
     *
     * @return The authentication token.
     *
     * @throws ServiceEntitlementException The exception for error case. If it's an HTTP response
     * error from the server, the error code can be retrieved by
     * {@link ServiceEntitlementException#getHttpStatus()}
     */
    @NonNull
    public Ts43AuthToken getAuthToken(@NonNull URL aesUrl)
            throws ServiceEntitlementException {
        return null;
    }
}
