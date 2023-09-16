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
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;
import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;

/**
 * Represents a newly-created scoped credential, aka the response from a registration request.
 */
@PublicApi
@SafeParcelable.Class
public class AuthenticatorAttestationResponse extends AuthenticatorResponse {
    @Field(value = 2, getterName = "getKeyHandle")
    @NonNull
    private byte[] keyHandle;
    @Field(value = 3, getterName = "getClientDataJSON")
    @NonNull
    private byte[] clientDataJSON;
    @Field(value = 4, getterName = "getAttestationObject")
    @NonNull
    private byte[] attestationObject;
    @Field(value = 5, getterName = "getTransports")
    @NonNull
    private String[] transports;

    private AuthenticatorAttestationResponse() {
    }

    @Hide
    @Constructor
    public AuthenticatorAttestationResponse(@Param(2) @NonNull byte[] keyHandle, @Param(3) @NonNull byte[] clientDataJSON, @Param(4) @NonNull byte[] attestationObject, @Param(5) @NonNull String[] transports) {
        this.keyHandle = keyHandle;
        this.clientDataJSON = clientDataJSON;
        this.attestationObject = attestationObject;
        this.transports = transports;
    }

    @NonNull
    public byte[] getAttestationObject() {
        return attestationObject;
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
    public String[] getTransports() {
        return transports;
    }

    @Override
    @NonNull
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
        if (!Arrays.equals(attestationObject, that.attestationObject)) return false;
        if (!Arrays.equals(transports, that.transports)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{Arrays.hashCode(keyHandle), Arrays.hashCode(clientDataJSON), Arrays.hashCode(attestationObject), Arrays.hashCode(transports)});
    }

    @Override
    @NonNull
    public String toString() {
        return ToStringHelper.name("AuthenticatorAttestationResponse")
                .field("keyHandle", keyHandle)
                .field("clientDataJSON", clientDataJSON)
                .field("attestationObject", attestationObject)
                .field("transports", transports)
                .end();
    }

    @NonNull
    public static AuthenticatorAttestationResponse deserializeFromBytes(byte[] serializedBytes) {
        return SafeParcelableSerializer.deserializeFromBytes(serializedBytes, CREATOR);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AuthenticatorAttestationResponse> CREATOR = findCreator(AuthenticatorAttestationResponse.class);
}
