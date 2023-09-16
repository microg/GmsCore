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
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;

/**
 * This structure contains cryptographic signatures produced by scoped credentials that provides proof of possession
 * of a private key as well as evidence of user consent to a specific transaction.
 */
@PublicApi
@SafeParcelable.Class
public class AuthenticatorAssertionResponse extends AuthenticatorResponse {
    @Field(value = 2, getterName = "getKeyHandle")
    @NonNull
    private byte[] keyHandle;
    @Field(value = 3, getterName = "getClientDataJSON")
    @NonNull
    private byte[] clientDataJSON;
    @Field(value = 4, getterName = "getAuthenticatorData")
    @NonNull
    private byte[] authenticatorData;
    @Field(value = 5, getterName = "getSignature")
    @NonNull
    private byte[] signature;
    @Field(value = 6, getterName = "getUserHandle")
    @Nullable
    private byte[] userHandle;

    private AuthenticatorAssertionResponse() {
    }

    @Constructor
    public AuthenticatorAssertionResponse(@Param(2) @NonNull byte[] keyHandle, @Param(3) @NonNull byte[] clientDataJSON, @Param(4) @NonNull byte[] authenticatorData, @Param(5) @NonNull byte[] signature, @Param(6) @Nullable byte[] userHandle) {
        this.keyHandle = keyHandle;
        this.clientDataJSON = clientDataJSON;
        this.authenticatorData = authenticatorData;
        this.signature = signature;
        this.userHandle = userHandle;
    }

    @NonNull
    public byte[] getAuthenticatorData() {
        return authenticatorData;
    }

    @Override
    @NonNull
    public byte[] getClientDataJSON() {
        return clientDataJSON;
    }

    /**
     * @deprecated use {@link PublicKeyCredential#getRawId()} instead
     */
    @Deprecated
    @NonNull
    public byte[] getKeyHandle() {
        return keyHandle;
    }

    @NonNull
    public byte[] getSignature() {
        return signature;
    }

    @Nullable
    public byte[] getUserHandle() {
        return userHandle;
    }

    @Override
    @NonNull
    public byte[] serializeToBytes() {
        return SafeParcelableSerializer.serializeToBytes(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthenticatorAssertionResponse)) return false;

        AuthenticatorAssertionResponse that = (AuthenticatorAssertionResponse) o;

        if (!Arrays.equals(keyHandle, that.keyHandle)) return false;
        if (!Arrays.equals(clientDataJSON, that.clientDataJSON)) return false;
        if (!Arrays.equals(authenticatorData, that.authenticatorData)) return false;
        if (!Arrays.equals(signature, that.signature)) return false;
        return Arrays.equals(userHandle, that.userHandle);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{Arrays.hashCode(keyHandle), Arrays.hashCode(clientDataJSON), Arrays.hashCode(authenticatorData), Arrays.hashCode(signature), Arrays.hashCode(userHandle)});
    }

    @Override
    @NonNull
    public String toString() {
        return ToStringHelper.name("AuthenticatorAssertionResponse")
                .field("keyHandle", keyHandle)
                .field("clientDataJSON", clientDataJSON)
                .field("authenticatorData", authenticatorData)
                .field("signature", signature)
                .field("userHandle", userHandle)
                .end();
    }

    @NonNull
    public static AuthenticatorAssertionResponse deserializeFromBytes(byte[] serializedBytes) {
        return SafeParcelableSerializer.deserializeFromBytes(serializedBytes, CREATOR);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AuthenticatorAssertionResponse> CREATOR = findCreator(AuthenticatorAssertionResponse.class);
}
