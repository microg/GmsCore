/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;
import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;

/**
 * The response after an error occurred.
 */
@PublicApi
@SafeParcelable.Class
public class AuthenticatorErrorResponse extends AuthenticatorResponse {
    @Field(value = 2, getterName = "getErrorCode")
    @NonNull
    private ErrorCode errorCode;
    @Field(value = 3, getterName = "getErrorMessage")
    @Nullable
    private String errorMessage;
    @Field(value = 4, getterName = "getInternalErrorCode")
    private int internalErrorCode;

    private AuthenticatorErrorResponse() {
    }

    @Hide
    public AuthenticatorErrorResponse(@NonNull ErrorCode errorCode, @Nullable String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    @Constructor
    AuthenticatorErrorResponse(@Param(2) @NonNull ErrorCode errorCode, @Param(3) @Nullable String errorMessage, @Param(4) int internalErrorCode) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.internalErrorCode = internalErrorCode;
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

    @Hide
    public int getInternalErrorCode() {
        return internalErrorCode;
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

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AuthenticatorErrorResponse> CREATOR = findCreator(AuthenticatorErrorResponse.class);
}
