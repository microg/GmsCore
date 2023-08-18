/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;

/**
 * This structure contains cryptographic signatures produced by scoped credentials that provides proof of possession
 * of a private key as well as evidence of user consent to a specific transaction.
 */
@PublicApi
public class AuthenticatorAssertionResponse extends AuthenticatorResponse {
    @Field(2)
    private byte[] keyHandle;
    @Field(3)
    private byte[] clientDataJSON;
    @Field(4)
    private byte[] authenticatorData;
    @Field(5)
    private byte[] signature;
    @Field(6)
    private byte[] userHandle;

    private AuthenticatorAssertionResponse() {}

    public AuthenticatorAssertionResponse(byte[] keyHandle, byte[] clientDataJSON, byte[] authenticatorData, byte[] signature, byte[] userHandle) {
        this.keyHandle = keyHandle;
        this.clientDataJSON = clientDataJSON;
        this.authenticatorData = authenticatorData;
        this.signature = signature;
        this.userHandle = userHandle;
    }

    public byte[] getAuthenticatorData() {
        return authenticatorData;
    }

    @Override
    public byte[] getClientDataJSON() {
        return clientDataJSON;
    }

    /**
     * @deprecated use {@link PublicKeyCredential#getRawId()} instead
     */
    @Deprecated
    public byte[] getKeyHandle() {
        return keyHandle;
    }

    public byte[] getSignature() {
        return signature;
    }

    public byte[] getUserHandle() {
        return userHandle;
    }

    @Override
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
    public String toString() {
        return ToStringHelper.name("AuthenticatorAssertionResponse")
                .field("keyHandle", keyHandle)
                .field("clientDataJSON", clientDataJSON)
                .field("authenticatorData", authenticatorData)
                .field("signature", signature)
                .field("userHandle", userHandle)
                .end();
    }

    public static AuthenticatorAssertionResponse deserializeFromBytes(byte[] serializedBytes) {
        return SafeParcelableSerializer.deserializeFromBytes(serializedBytes, CREATOR);
    }

    public static final Creator<AuthenticatorAssertionResponse> CREATOR = new AutoCreator<>(AuthenticatorAssertionResponse.class);
}
