/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;
import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;

/**
 * The response after an error occurred.
 */
@PublicApi
public class AuthenticatorErrorResponse extends AuthenticatorResponse {
    @Field(2)
    @NonNull
    private ErrorCode errorCode;
    @Field(3)
    @Nullable
    private String errorMessage;
    @Field(4)
    private int internalErrorCode;

    private AuthenticatorErrorResponse() {
    }

    @Hide
    public AuthenticatorErrorResponse(@NonNull ErrorCode errorCode, @Nullable String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    @Override
    @NonNull
    public byte[] getClientDataJSON() {
        throw new UnsupportedOperationException();
    }

    @NonNull
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getErrorCodeAsInt() {
        return errorCode.getCode();
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    @NonNull
    public byte[] serializeToBytes() {
        return SafeParcelableSerializer.serializeToBytes(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthenticatorErrorResponse)) return false;

        AuthenticatorErrorResponse that = (AuthenticatorErrorResponse) o;

        if (errorCode != null ? !errorCode.equals(that.errorCode) : that.errorCode != null) return false;
        if (errorMessage != null ? !errorMessage.equals(that.errorMessage) : that.errorMessage != null) return false;
        if (internalErrorCode != that.internalErrorCode) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{errorCode, errorMessage, internalErrorCode});
    }

    @Override
    @NonNull
    public String toString() {
        return ToStringHelper.name("AuthenticatorErrorResponse")
                .value(errorCode.name())
                .value(errorMessage)
                .end();
    }

    @NonNull
    public static AuthenticatorErrorResponse deserializeFromBytes(byte[] serializedBytes) {
        return SafeParcelableSerializer.deserializeFromBytes(serializedBytes, CREATOR);
    }

    public static final Creator<AuthenticatorErrorResponse> CREATOR = new AutoCreator<>(AuthenticatorErrorResponse.class);
}
