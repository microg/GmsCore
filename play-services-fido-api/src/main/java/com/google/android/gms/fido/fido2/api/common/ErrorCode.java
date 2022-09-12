/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;
import android.os.Parcelable;

import org.microg.gms.common.PublicApi;

/**
 * Error codes that are referenced by WebAuthn spec.
 */
@PublicApi
public enum ErrorCode implements Parcelable {
    /**
     * The operation is not supported.
     */
    NOT_SUPPORTED_ERR(9),
    /**
     * The object is in an invalid state.
     */
    INVALID_STATE_ERR(11),
    /**
     * The operation is insecure.
     */
    SECURITY_ERR(18),
    /**
     * A network error occurred.
     */
    NETWORK_ERR(19),
    /**
     * The operation was aborted.
     */
    ABORT_ERR(20),
    /**
     * The operation timed out.
     */
    TIMEOUT_ERR(23),
    /**
     * The encoding operation (either encoded or decoding) failed.
     */
    ENCODING_ERR(27),
    /**
     * The operation failed for an unknown transient reason.
     */
    UNKNOWN_ERR(28),
    /**
     * A mutation operation in a transaction failed because a constraint was not satisfied.
     */
    CONSTRAINT_ERR(29),
    /**
     * Provided data is inadequate.
     */
    DATA_ERR(30),
    /**
     * The request is not allowed by the user agent or the platform in the current context, possibly because the user
     * denied permission.
     */
    NOT_ALLOWED_ERR(35),
    /**
     * The authenticator violates the privacy requirements of the {@code AttestationStatementType} it is using.
     */
    ATTESTATION_NOT_PRIVATE_ERR(36);

    private int code;

    ErrorCode(int code) {
        this.code = code;
    }

    @PublicApi(exclude = true)
    public int getCode() {
        return code;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(code);
    }

    @PublicApi(exclude = true)
    public static ErrorCode toErrorCode(int errorCode) throws UnsupportedErrorCodeException {
        for (ErrorCode value : values()) {
            if (value.code == errorCode) return value;
        }
        throw new UnsupportedErrorCodeException(errorCode);
    }

    /**
     * Exception thrown when an unsupported or unrecognized error code is encountered.
     */
    public static class UnsupportedErrorCodeException extends Exception {
        /**
         * Constructor for the {@link ErrorCode.UnsupportedErrorCodeException}.
         */
        public UnsupportedErrorCodeException(int errorCode) {
            super("Error code " + errorCode + " is not supported");
        }
    }

    @PublicApi(exclude = true)
    public static final Creator<ErrorCode> CREATOR = new Creator<ErrorCode>() {
        @Override
        public ErrorCode createFromParcel(Parcel source) {
            try {
                return ErrorCode.toErrorCode(source.readInt());
            } catch (UnsupportedErrorCodeException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public ErrorCode[] newArray(int size) {
            return new ErrorCode[size];
        }
    };
}
