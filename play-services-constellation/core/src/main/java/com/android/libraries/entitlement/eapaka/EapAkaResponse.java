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

package com.android.libraries.entitlement.eapaka;

import static com.android.libraries.entitlement.ServiceEntitlementException.ERROR_ICC_AUTHENTICATION_NOT_AVAILABLE;
import static com.android.libraries.entitlement.eapaka.EapAkaChallenge.SUBTYPE_AKA_CHALLENGE;
import static com.android.libraries.entitlement.eapaka.EapAkaChallenge.TYPE_EAP_AKA;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.libraries.entitlement.ServiceEntitlementException;
import com.android.libraries.entitlement.utils.BytesConverter;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Generates the response of EAP-AKA challenge. Refer to RFC 4187 Section 8.1 Message
 * Format/RFC 3748 Session 4 EAP Packet Format.
 */
public class EapAkaResponse {
    private static final String TAG = "ServiceEntitlement";

    private static final byte CODE_RESPONSE = 0x02;
    private static final byte SUBTYPE_SYNC_FAILURE = 0x04;
    private static final byte ATTRIBUTE_RES = 0x03;
    private static final byte ATTRIBUTE_AUTS = 0x04;
    private static final byte ATTRIBUTE_MAC = 0x0B;
    private static final String ALGORITHM_HMAC_SHA1 = "HmacSHA1";
    private static final int SHA1_OUTPUT_LENGTH = 20;
    private static final int MAC_LENGTH = 16;

    // RFC 4187 Section 9.4 EAP-Response/AKA-Challenge
    private String mResponse;
    // RFC 4187 Section 9.6 EAP-Response/AKA-Synchronization-Failure
    private String mSynchronizationFailureResponse;

    private EapAkaResponse() {}

    /** Returns EAP-Response/AKA-Challenge, if authentication success. Otherwise {@code null}. */
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

    /**
     * Returns EAP-AKA challenge response message which generated with SIM EAP-AKA authentication
     * with network provided EAP-AKA challenge request message.
     */
    public static EapAkaResponse respondToEapAkaChallenge(
            Context context,
            int simSubscriptionId,
            EapAkaChallenge eapAkaChallenge,
            String eapAkaRealm)
            throws ServiceEntitlementException {
        TelephonyManager telephonyManager =
                context.getSystemService(TelephonyManager.class)
                        .createForSubscriptionId(simSubscriptionId);

        // process EAP-AKA authentication with SIM
        String response = null;
        try {
            response = telephonyManager.getIccAuthentication(TelephonyManager.APPTYPE_USIM,
                TelephonyManager.AUTHTYPE_EAP_AKA,
                eapAkaChallenge.getSimAuthenticationRequest());
        } catch (UnsupportedOperationException e) {
            throw new ServiceEntitlementException(
                ERROR_ICC_AUTHENTICATION_NOT_AVAILABLE,
                "UnsupportedOperationException" + e.toString());
        }
        if (response == null) {
            throw new ServiceEntitlementException(
                    ERROR_ICC_AUTHENTICATION_NOT_AVAILABLE, "EAP-AKA response is null!");
        }

        EapAkaSecurityContext securityContext = EapAkaSecurityContext.from(response);
        EapAkaResponse result = new EapAkaResponse();

        if (securityContext.getRes() != null
                && securityContext.getIk() != null
                && securityContext.getCk() != null) { // Success authentication

            // generate master key - refer to RFC 4187, section 7. Key Generation
            MasterKey mk =
                    MasterKey.create(
                            EapAkaApi.getImsiEap(
                                    telephonyManager.getSimOperator(),
                                    telephonyManager.getSubscriberId(),
                                    eapAkaRealm),
                            securityContext.getIk(),
                            securityContext.getCk());
            // K_aut is the key used to calculate MAC
            if (mk == null || mk.getAut() == null) {
                throw new ServiceEntitlementException(
                        ERROR_ICC_AUTHENTICATION_NOT_AVAILABLE, "Can't generate K_Aut!");
            }

            // generate EAP-AKA challenge response message
            byte[] challengeResponse =
                    generateEapAkaChallengeResponse(
                            securityContext.getRes(), eapAkaChallenge.getIdentifier(), mk.getAut());
            if (challengeResponse == null) {
                throw new ServiceEntitlementException(
                        ERROR_ICC_AUTHENTICATION_NOT_AVAILABLE,
                        "Failed to generate EAP-AKA Challenge Response data!");
            }
            // base64 encoding
            result.mResponse = Base64.encodeToString(challengeResponse, Base64.NO_WRAP).trim();

        } else if (securityContext.getAuts() != null) {

            byte[] syncFailure =
                    generateEapAkaSynchronizationFailureResponse(
                            securityContext.getAuts(), eapAkaChallenge.getIdentifier());
            result.mSynchronizationFailureResponse =
                    Base64.encodeToString(syncFailure, Base64.NO_WRAP).trim();

        } else {
            throw new ServiceEntitlementException(
                ERROR_ICC_AUTHENTICATION_NOT_AVAILABLE,
                "Invalid SIM EAP-AKA authentication response!");
        }

        return result;
    }

    /**
     * Returns EAP-Response/AKA-Challenge message, or {@code null} if failed to generate.
     * Refer to RFC 4187 section 9.4 EAP-Response/AKA-Challenge.
     */
    @VisibleForTesting
    @Nullable
    static byte[] generateEapAkaChallengeResponse(
            @Nullable byte[] res, byte identifier, @Nullable byte[] aut) {
        if (res == null || aut == null) {
            return null;
        }

        byte[] message = createEapAkaChallengeResponse(res, identifier);

        // use K_aut as key to calculate mac
        byte[] mac = calculateMac(aut, message);
        if (mac == null) {
            return null;
        }

        // fill MAC value to the message
        // The value start index is 8 + AT_RES (4 + res.length) + header of AT_MAC (4)
        int index = 8 + 4 + res.length + 4;
        System.arraycopy(mac, 0, message, index, mac.length);

        return message;
    }

    /**
     * Returns EAP-Response/AKA-Synchronization-Failure, or {@code null} if failed to generate.
     * Refer to RFC 4187 section 9.6 EAP-Response/AKA-Synchronization-Failure.
     */
    @VisibleForTesting
    @Nullable
    static byte[] generateEapAkaSynchronizationFailureResponse(
            @Nullable byte[] auts, byte identifier) {
        // size = 8 (header) + 2 (attribute & length) + AUTS
        byte[] message = new byte[10 + auts.length];

        // set up header
        message[0] = CODE_RESPONSE;
        // identifier: same as request
        message[1] = identifier;
        // length: include entire EAP-AKA message
        byte[] lengthBytes = BytesConverter.convertIntegerTo4Bytes(message.length);
        message[2] = lengthBytes[2];
        message[3] = lengthBytes[3];
        message[4] = TYPE_EAP_AKA;
        message[5] = SUBTYPE_SYNC_FAILURE;
        // reserved: 2 bytes
        message[6] = 0x00;
        message[7] = 0x00;

        // set up AT_AUTS. RFC 4187, Section 10.9 AT_AUTS
        message[8] = ATTRIBUTE_AUTS;
        // length (in 4-bytes): 4, because AUTS is 14 bytes, plus the attribute (1 byte) and
        // the length (1 byte).
        message[9] = 0x04;
        System.arraycopy(auts, 0, message, 10, auts.length);
        return message;
    }

    // AT_MAC/AT_RES are must included in response message
    //
    // Reference RFC 4187 Section 8.1 Message Format
    //           RFC 4187 Section 9.4 EAP-Response/AKA-Challenge
    //           RFC 3748 Section 4.1 Request and Response
    private static byte[] createEapAkaChallengeResponse(byte[] res, byte identifier) {
        // size = 8 (header) + resHeader (4) + res.length + AT_MAC (20 bytes)
        byte[] message = new byte[32 + res.length];

        // set up header
        message[0] = CODE_RESPONSE;
        // identifier: same as request
        message[1] = identifier;
        // length: include entire EAP-AKA message
        byte[] lengthBytes = BytesConverter.convertIntegerTo4Bytes(message.length);
        message[2] = lengthBytes[2];
        message[3] = lengthBytes[3];
        message[4] = TYPE_EAP_AKA;
        message[5] = SUBTYPE_AKA_CHALLENGE;
        // reserved: 2 bytes
        message[6] = 0x00;
        message[7] = 0x00;

        int index = 8;

        // set up AT_RES, RFC 4187, Section 10.8 AT_RES
        message[index++] = ATTRIBUTE_RES;
        // Length (in 4-bytes):
        // The length of the RES should already be a multiple of 4 bytes.
        // Add 4 to the attribute length to account for the attribute (1 byte), the length (1 byte),
        // and the length of the RES in bits (2 bytes).
        int resLength = (res.length + 4) / 4;
        message[index++] = (byte) (resLength & 0xff);
        // The value field of this attribute begins with the 2-byte RES Length, which identifies
        // the exact length of the RES in bits.
        byte[] resBitLength = BytesConverter.convertIntegerTo4Bytes(res.length * 8);
        message[index++] = resBitLength[2];
        message[index++] = resBitLength[3];
        System.arraycopy(res, 0, message, index, res.length);
        index += res.length;

        // set up AT_MAC, RFC 4187, Section 10.15 AT_MAC
        message[index++] = ATTRIBUTE_MAC;
        // length (in 4-bytes): 5, because MAC is 16 bytes, plus the attribute (1 byte),
        // the length (1 byte), and reserved bytes (2 bytes).
        message[index++] = 0x05;
        // With two bytes reserved
        message[index++] = 0x00;
        message[index++] = 0x00;

        // The MAC is calculated over the whole EAP packet and concatenated with optional
        // message-specific data, with the exception that the value field of the
        // MAC attribute is set to zero when calculating the MAC.
        Arrays.fill(message, index, index + 16, (byte) 0x00);

        return message;
    }

    // See RFC 4187, 10.15 AT_MAC, snippet as below, the key must be k_aut
    //
    // The MAC algorithm is HMAC-SHA1-128 [RFC2104] keyed hash value.  (The
    // HMAC-SHA1-128 value is obtained from the 20-byte HMAC-SHA1 value by
    // truncating the output to 16 bytes.  Hence, the length of the MAC is
    // 16 bytes.)  The derivation of the authentication key (K_aut) used in
    // the calculation of the MAC is specified in Section 7.
    @Nullable
    private static byte[] calculateMac(byte[] key, byte[] message) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM_HMAC_SHA1);
            SecretKeySpec secret = new SecretKeySpec(key, ALGORITHM_HMAC_SHA1);
            mac.init(secret);
            byte[] output = mac.doFinal(message);

            if (output == null || output.length != SHA1_OUTPUT_LENGTH) {
                Log.e(TAG, "Invalid result! length should be 20, but " + output.length);
                return null;
            }

            byte[] macValue = new byte[MAC_LENGTH];
            System.arraycopy(output, 0, macValue, 0, MAC_LENGTH);
            return macValue;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Log.e(TAG, "calculateMac failed!", e);
        }

        return null;
    }
}
