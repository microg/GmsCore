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
 * Provides format to handle request/response SIM Authentication with GSM/3G security context.
 *
 * <p>Reference ETSI TS 131 102, Section 7.1.2.1 GSM/3G security context.
 */
class EapAkaSecurityContext {
    private static final String TAG = "ServiceEntitlement";

    private static final byte RESPONSE_TAG_SUCCESS = (byte) 0xDB;
    private static final byte RESPONSE_TAG_SYNC_FAILURE = (byte) 0xDC;

    private boolean mValid;

    // User response, populated on successful authentication
    private byte[] mRes;
    // Cipher Key, populated on successful authentication
    private byte[] mCk;
    // Integrity Key, populated on successful authentication
    private byte[] mIk;
    // AUTS, populated on synchronization failure
    private byte[] mAuts;

    private EapAkaSecurityContext() {}

    /**
     * Provide {@link EapAkaSecurityContext} from response data.
     */
    public static EapAkaSecurityContext from(String response)
            throws ServiceEntitlementException {
        EapAkaSecurityContext securityContext = new EapAkaSecurityContext();
        securityContext.parseResponseData(response);
        if (!securityContext.isValid()) {
            throw new ServiceEntitlementException(
                ERROR_ICC_AUTHENTICATION_NOT_AVAILABLE,
                "Invalid SIM EAP-AKA authentication response!");
        }
        return securityContext;
    }

    /**
     * Parses SIM EAP-AKA Authentication responded data.
     */
    private void parseResponseData(String response) {
        byte[] data = null;

        try {
            data = Base64.decode(response.getBytes(UTF_8), Base64.DEFAULT);
            Log.d(TAG, "Decoded response data length = " + data.length + " bytes");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Response is not a valid base-64 content");
            return;
        }
        if (data.length == 0) {
            return;
        }

        // Check tag, the initial byte
        int index = 0;
        if (data[index] == RESPONSE_TAG_SUCCESS) {
            // Parse RES
            index++; // move to RES length byte
            mRes = parseTag(index, data);
            if (mRes == null) {
                Log.d(TAG, "Invalid data: can't parse RES!");
                return;
            }
            // Parse CK
            index += mRes.length + 1; // move to CK length byte
            mCk = parseTag(index, data);
            if (mCk == null) {
                Log.d(TAG, "Invalid data: can't parse CK!");
                return;
            }
            // Parse IK
            index += mCk.length + 1; // move to IK length byte
            mIk = parseTag(index, data);
            if (mIk == null) {
                Log.d(TAG, "Invalid data: can't parse IK!");
                return;
            }
            mValid = true;
        } else if (data[index] == RESPONSE_TAG_SYNC_FAILURE) {
            // Parse AUTS
            index++; // move to AUTS length byte
            mAuts = parseTag(index, data);
            if (mAuts == null) {
                Log.d(TAG, "Invalid data: can't parse AUTS!");
                return;
            }
            mValid = true;
        } else {
            Log.d(TAG, "Not a valid tag, tag=" + data[index]);
            return;
        }
    }

    private byte[] parseTag(int index, byte[] src) {
        // index at the length byte
        if (index >= src.length) {
            Log.d(TAG, "No length byte!");
            return null;
        }
        int length = src[index] & 0xff;
        if (index + length >= src.length) {
            Log.d(TAG, "Invalid data length!");
            return null;
        }
        index++; // move to first byte of tag value
        byte[] dest = new byte[length];
        System.arraycopy(src, index, dest, 0, length);

        return dest;
    }

    private boolean isValid() {
        return mValid;
    }

    /**
     * Returns RES, or {@code null} for a synchronization failure.
     */
    @Nullable
    public byte[] getRes() {
        return mRes;
    }

    /**
     * Returns CK, or {@code null} for a synchronization failure.
     */
    @Nullable
    public byte[] getCk() {
        return mCk;
    }

    /**
     * Returns IK, or {@code null} for a synchronization failure.
     */
    @Nullable
    public byte[] getIk() {
        return mIk;
    }

    /**
     * Returns AUTS, or {@code null} for a successful authentication.
     */
    @Nullable
    public byte[] getAuts() {
        return mAuts;
    }
}
