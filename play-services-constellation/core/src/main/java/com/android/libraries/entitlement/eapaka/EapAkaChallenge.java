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

import static java.nio.charset.StandardCharsets.UTF_8;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.libraries.entitlement.ServiceEntitlementException;

/**
 * Parses EAP-AKA challenge from server. Refer to RFC 4187 Section 8.1 Message
 * Format/RFC 3748 Session 4 EAP Packet Format.
 */
public class EapAkaChallenge {
    private static final String TAG = "ServiceEntitlement";

    private static final int EAP_AKA_HEADER_LENGTH = 8;
    private static final byte CODE_REQUEST = 0x01;
    static final byte TYPE_EAP_AKA = 0x17;
    static final byte SUBTYPE_AKA_CHALLENGE = 0x01;
    private static final byte ATTRIBUTE_RAND = 0x01;
    private static final byte ATTRIBUTE_AUTN = 0x02;
    private static final int ATTRIBUTE_LENGTH = 20;
    private static final int RAND_LENGTH = 16;
    private static final int AUTN_LENGTH = 16;

    // The identifier of Response must same as Request
    private byte mIdentifier = -1;
    // The value of AT_AUTN, network authentication token
    private byte[] mAutn;
    // The value of AT_RAND, random challenge
    private byte[] mRand;

    // Base64 encoded 3G security context for SIM Authentication request
    private String mSimAuthenticationRequest;

    private EapAkaChallenge() {}

    /** Parses a EAP-AKA challenge request message encoded in base64. */
    public static EapAkaChallenge parseEapAkaChallenge(String challenge)
            throws ServiceEntitlementException {
        byte[] data;
        try {
            data = Base64.decode(challenge.getBytes(UTF_8), Base64.DEFAULT);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new ServiceEntitlementException(
                    ERROR_ICC_AUTHENTICATION_NOT_AVAILABLE,
                    "EAP-AKA challenge is not a valid base64!");
        }
        EapAkaChallenge result = new EapAkaChallenge();
        if (result.parseEapAkaHeader(data) && result.parseRandAndAutn(data)) {
            result.mSimAuthenticationRequest = result.getSimAuthChallengeData();
            return result;
        } else {
            throw new ServiceEntitlementException(
                    ERROR_ICC_AUTHENTICATION_NOT_AVAILABLE,
                    "EAP-AKA challenge message is not valid");
        }
    }

    /**
     * Returns the base64 encoded 3G security context for SIM Authentication request,
     * or {@code null} if the EAP-AKA challenge is not valid.
     */
    @Nullable
    public String getSimAuthenticationRequest() {
        return mSimAuthenticationRequest;
    }

    /** Returns the EAP package identifier in the EAP-AKA challenge. */
    public byte getIdentifier() {
        return mIdentifier;
    }

    /**
     * Parses EAP-AKA header, 8 bytes including 2 reserved bytes.
     *
     * @return {@code true} if success to parse the header of request data.
     */
    private boolean parseEapAkaHeader(byte[] data) {
        if (data.length < EAP_AKA_HEADER_LENGTH) {
            return false;
        }
        // Code for EAP-request should be CODE_REQUEST
        byte code = data[0];
        // EAP package identifier
        mIdentifier = data[1];
        // Total length of full EAP-AKA message, include code, identifier, ...
        int length = ((data[2] & 0xff) << 8) | (data[3] & 0xff);
        // Type for EAP-AKA should be TYPE_EAP_AKA
        byte type = data[4];
        // SubType for AKA-Challenge should be SUBTYPE_AKA_CHALLENGE
        byte subType = data[5];

        // Validate header
        if (code != CODE_REQUEST
                || length != data.length
                || type != TYPE_EAP_AKA
                || subType != SUBTYPE_AKA_CHALLENGE) {
            Log.d(
                    TAG,
                    "Invalid EAP-AKA Header, code="
                            + code
                            + ", length="
                            + length
                            + ", real length="
                            + data.length
                            + ", type="
                            + type
                            + ", subType="
                            + subType);
            return false;
        }

        return true;
    }

    /**
     * Parses AT_RAND and AT_AUTN. Refer to RFC 4187 section 10.6 AT_RAND/section 10.7 AT_AUTN.
     *
     * @return {@code true} if success to parse the RAND and AUTN data.
     */
    private boolean parseRandAndAutn(byte[] data) {
        int index = EAP_AKA_HEADER_LENGTH;
        while (index < data.length) {
            int remainsLength = data.length - index;
            if (remainsLength <= 2) {
                Log.d(TAG, "Error! remainsLength = " + remainsLength);
                return false;
            }

            byte attributeType = data[index];

            // the length of this attribute in multiples of 4 bytes, include attribute type and
            // length
            int length = (data[index + 1] & 0xff) * 4;
            if (length > remainsLength) {
                Log.d(TAG,
                        "Length Error! length is " + length + " but only remains " + remainsLength);
                return false;
            }

            // see RFC 4187 section 11 for attribute type
            if (attributeType == ATTRIBUTE_RAND) {
                if (length != ATTRIBUTE_LENGTH) {
                    Log.d(TAG, "AT_RAND length is " + length);
                    return false;
                }
                mRand = new byte[RAND_LENGTH];
                System.arraycopy(data, index + 4, mRand, 0, RAND_LENGTH);
            } else if (attributeType == ATTRIBUTE_AUTN) {
                if (length != ATTRIBUTE_LENGTH) {
                    Log.d(TAG, "AT_AUTN length is " + length);
                    return false;
                }
                mAutn = new byte[AUTN_LENGTH];
                System.arraycopy(data, index + 4, mAutn, 0, AUTN_LENGTH);
            }

            index += length;
        }

        // check has AT_RAND and AT_AUTH
        if (mRand == null || mAutn == null) {
            Log.d(TAG, "Invalid Type Datas!");
            return false;
        }

        return true;
    }

    /**
     * Returns Base64 encoded 3G security context for SIM Authentication request.
     */
    @Nullable
    private String getSimAuthChallengeData() {
        byte[] challengeData = new byte[RAND_LENGTH + AUTN_LENGTH + 2];
        challengeData[0] = (byte) RAND_LENGTH;
        System.arraycopy(mRand, 0, challengeData, 1, RAND_LENGTH);
        challengeData[RAND_LENGTH + 1] = (byte) AUTN_LENGTH;
        System.arraycopy(mAutn, 0, challengeData, RAND_LENGTH + 2, AUTN_LENGTH);

        return Base64.encodeToString(challengeData, Base64.NO_WRAP).trim();
    }
}
