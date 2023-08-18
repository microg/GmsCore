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
 * Represents a newly-created scoped credential, aka the response from a registration request.
 */
@PublicApi
public class AuthenticatorAttestationResponse extends AuthenticatorResponse {
    @Field(2)
    private byte[] keyHandle;
    @Field(3)
    private byte[] clientDataJSON;
    @Field(4)
    private byte[] attestationObject;

    private AuthenticatorAttestationResponse() {}

    @PublicApi(exclude = true)
    public AuthenticatorAttestationResponse(byte[] keyHandle, byte[] clientDataJSON, byte[] attestationObject) {
        this.keyHandle = keyHandle;
        this.clientDataJSON = clientDataJSON;
        this.attestationObject = attestationObject;
    }

    public byte[] getAttestationObject() {
        return attestationObject;
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

    @Override
    public byte[] serializeToBytes() {
        return SafeParcelableSerializer.serializeToBytes(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthenticatorAttestationResponse)) return false;

        AuthenticatorAttestationResponse that = (AuthenticatorAttestationResponse) o;

        if (!Arrays.equals(keyHandle, that.keyHandle)) return false;
        if (!Arrays.equals(clientDataJSON, that.clientDataJSON)) return false;
        return Arrays.equals(attestationObject, that.attestationObject);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{Arrays.hashCode(keyHandle), Arrays.hashCode(clientDataJSON), Arrays.hashCode(attestationObject)});
    }

    @Override
    public String toString() {
        return ToStringHelper.name("AuthenticatorAttestationResponse")
                .field("keyHandle", keyHandle)
                .field("clientDataJSON", clientDataJSON)
                .field("attestationObject", attestationObject)
                .end();
    }

    public static AuthenticatorAttestationResponse deserializeFromBytes(byte[] serializedBytes) {
        return SafeParcelableSerializer.deserializeFromBytes(serializedBytes, CREATOR);
    }

    public static final Creator<AuthenticatorAttestationResponse> CREATOR = new AutoCreator<>(AuthenticatorAttestationResponse.class);
}
