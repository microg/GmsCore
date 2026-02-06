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

import static com.android.libraries.entitlement.eapaka.EapAkaResponse.respondToEapAkaChallenge;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.libraries.entitlement.eapaka.EapAkaApi;
import com.android.libraries.entitlement.eapaka.EapAkaChallenge;

/**
 * Some utility methods used in EAP-AKA authentication in service entitlement, and could be
 * helpful to other apps.
 */
public class EapAkaHelper {
    private static final String TAG = "ServiceEntitlement";

    private final Context mContext;
    private final int mSimSubscriptionId;

    EapAkaHelper(Context context, int simSubscriptionId) {
        mContext = context;
        mSimSubscriptionId = simSubscriptionId;
    }

    /**
     * Factory method.
     *
     * @param context           context of application
     * @param simSubscriptionId the subscroption ID of the carrier's SIM on device. This indicates
     *                          which SIM to retrieve IMEI/IMSI from and perform EAP-AKA
     *                          authentication with. See
     *                          {@link android.telephony.SubscriptionManager}
     *                          for how to get the subscroption ID.
     */
    public static EapAkaHelper getInstance(Context context, int simSubscriptionId) {
        return new EapAkaHelper(context, simSubscriptionId);
    }

    /**
     * Returns the root NAI for EAP-AKA authentication as per 3GPP TS 23.003 19.3.2, or
     * {@code null} if failed. The result will be in the form:
     *
     * <p>{@code 0<IMSI>@nai.epc.mnc<MNC>.mcc<MCC>.3gppnetwork.org}
     */
    @Nullable
    public String getEapAkaRootNai() {
        TelephonyManager telephonyManager =
                mContext.getSystemService(TelephonyManager.class)
                        .createForSubscriptionId(mSimSubscriptionId);
        return EapAkaApi.getImsiEap(
                telephonyManager.getSimOperator(), telephonyManager.getSubscriberId(), "nai.epc");
    }

    /**
     * Returns the EAP-AKA challenge response to the given EAP-AKA {@code challenge}, or
     * {@code null} if failed.
     *
     * <p>Both the challange and response are base-64 encoded EAP-AKA message: refer to
     * RFC 4187 Section 8.1 Message Format/RFC 3748 Session 4 EAP Packet Format.
     *
     * @deprecated use {@link getEapAkaResponse(String)} which additionally supports
     * Synchronization-Failure case.
     */
    @Deprecated
    @Nullable
    public String getEapAkaChallengeResponse(String challenge) {
        EapAkaResponse eapAkaResponse = getEapAkaResponse(challenge);
        return (eapAkaResponse == null)
                ? null
                : eapAkaResponse.response(); // Would be null on synchronization failure
    }

    /**
     * Returns the {@link EapAkaResponse} to the given EAP-AKA {@code challenge}, or
     * {@code null} if failed.
     *
     * <p>Both the challange and response are base-64 encoded EAP-AKA message: refer to
     * RFC 4187 Section 8.1 Message Format/RFC 3748 Session 4 EAP Packet Format.
     */
    @Nullable
    public EapAkaResponse getEapAkaResponse(String challenge) {
        try {
            EapAkaChallenge eapAkaChallenge = EapAkaChallenge.parseEapAkaChallenge(challenge);
            com.android.libraries.entitlement.eapaka.EapAkaResponse eapAkaResponse =
                    respondToEapAkaChallenge(
                            mContext, mSimSubscriptionId, eapAkaChallenge, "nai.epc");
            return new EapAkaResponse(
                    eapAkaResponse.response(), eapAkaResponse.synchronizationFailureResponse());
        } catch (ServiceEntitlementException e) {
            Log.i(TAG, "Failed to generate EAP-AKA response", e);
            return null;
        }
    }

    // Similar to .eapaka.EapAkaResponse but with simplfied API surface for external usage.
    /** EAP-AKA response */
    public static class EapAkaResponse {
        // RFC 4187 Section 9.4 EAP-Response/AKA-Challenge
        @Nullable private final String mResponse;
        // RFC 4187 Section 9.6 EAP-Response/AKA-Synchronization-Failure
        @Nullable private final String mSynchronizationFailureResponse;

        private EapAkaResponse(
                @Nullable String response, @Nullable String synchronizationFailureResponse) {
            mResponse = response;
            mSynchronizationFailureResponse = synchronizationFailureResponse;
        }

        /**
         * Returns EAP-Response/AKA-Challenge, if authentication success.
         * Otherwise {@code null}.
         */
        @Nullable
        public String response() {
            return mResponse;
        }

        /**
         * Returns EAP-Response/AKA-Synchronization-Failure, if synchronization failure detected.
         * Otherwise {@code null}.
         */
        @Nullable
        public String synchronizationFailureResponse() {
            return mSynchronizationFailureResponse;
        }
    }
}
