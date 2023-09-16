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
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;
import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;

import java.util.Arrays;

/**
 * This class is contains the attributes that are returned to the caller when a new credential is created, or a new
 * assertion is requested.
 */
@PublicApi
@SafeParcelable.Class
public class PublicKeyCredential extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getId")
    @NonNull
    private String id;
    @Field(value = 2, getterName = "getType")
    @NonNull
    private String type;
    @Field(value = 3, getterName = "getRawId")
    @NonNull
    private byte[] rawId;
    @Field(value = 4, getter = "$object.getResponse() instanceof $type ? ($type) $object.getResponse() : null")
    @Nullable
    private AuthenticatorAttestationResponse registerResponse;
    @Field(value = 5, getter = "$object.getResponse() instanceof $type ? ($type) $object.getResponse() : null")
    @Nullable
    private AuthenticatorAssertionResponse signResponse;
    @Field(value = 6, getter = "$object.getResponse() instanceof $type ? ($type) $object.getResponse() : null")
    @Nullable
    private AuthenticatorErrorResponse errorResponse;
    @Field(value = 7, getterName = "getClientExtensionResults")
    @Nullable
    private AuthenticationExtensionsClientOutputs clientExtensionResults;
    @Field(value = 8, getterName = "getAuthenticatorAttachment")
    @Nullable
    private String authenticatorAttachment;

    PublicKeyCredential(@NonNull String id, @NonNull String type, @NonNull byte[] rawId, @NonNull AuthenticatorResponse response, @Nullable AuthenticationExtensionsClientOutputs clientExtensionResults, @Nullable String authenticatorAttachment) {
        this(id, type, rawId, response instanceof AuthenticatorAttestationResponse ? (AuthenticatorAttestationResponse) response : null, response instanceof AuthenticatorAssertionResponse ? (AuthenticatorAssertionResponse) response : null, response instanceof AuthenticatorErrorResponse ? (AuthenticatorErrorResponse) response : null, clientExtensionResults, authenticatorAttachment);
    }

    @Constructor
    PublicKeyCredential(@Param(1) @NonNull String id, @Param(2) @NonNull String type, @Param(3) @NonNull byte[] rawId, @Param(4) @Nullable AuthenticatorAttestationResponse registerResponse, @Param(5) @Nullable AuthenticatorAssertionResponse signResponse, @Param(6) @Nullable AuthenticatorErrorResponse errorResponse, @Param(7) @Nullable AuthenticationExtensionsClientOutputs clientExtensionResults, @Param(8) @Nullable String authenticatorAttachment) {
        this.id = id;
        this.type = type;
        this.rawId = rawId;
        this.registerResponse = registerResponse;
        this.signResponse = signResponse;
        this.errorResponse = errorResponse;
        this.clientExtensionResults = clientExtensionResults;
        this.authenticatorAttachment = authenticatorAttachment;
    }

    /**
     * Returns the authenticator attachment of this credential.
     */
    @Nullable
    public String getAuthenticatorAttachment() {
        return authenticatorAttachment;
    }

    @Nullable
    public AuthenticationExtensionsClientOutputs getClientExtensionResults() {
        return clientExtensionResults;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public byte[] getRawId() {
        return rawId;
    }

    public AuthenticatorResponse getResponse() {
        if (registerResponse != null) return registerResponse;
        if (signResponse != null) return signResponse;
        if (errorResponse != null) return errorResponse;
        throw new IllegalStateException("No response set.");
    }

    @NonNull
    public String getType() {
        return type;
    }

    /**
     * Builder for {@link PublicKeyCredential}.
     */
    public static class Builder {
        @NonNull
        private String id;
        @NonNull
        private byte[] rawId;
        private AuthenticatorResponse response;
        @Nullable
        private AuthenticationExtensionsClientOutputs extensionsClientOutputs;
        @Nullable
        private String authenticatorAttachment;

        /**
         * The constructor of {@link PublicKeyCredential.Builder}.
         */
        public Builder() {
        }

        /**
         * Sets the output produced by the client's processing of the extensions requested by the relying party.
         */
        public Builder setAuthenticationExtensionsClientOutputs(@Nullable AuthenticationExtensionsClientOutputs extensionsClientOutputs) {
            this.extensionsClientOutputs = extensionsClientOutputs;
            return this;
        }

        /**
         * Sets the authenticator attachment of the credential.
         */
        public Builder setAuthenticatorAttachment(@NonNull String authenticatorAttachment) {
            this.authenticatorAttachment = authenticatorAttachment;
            return this;
        }

        /**
         * Sets the base64url encoding of the credential identifier.
         */
        public Builder setId(@NonNull String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the raw value of the credential identifier.
         */
        public Builder setRawId(@NonNull byte[] rawId) {
            this.rawId = rawId;
            return this;
        }

        /**
         * Sets the authenticator's response to the clients register or sign request.
         * <p>
         * This attribute contains the authenticator's response to the client’s request to either create a public key
         * credential, or generate an authentication assertion. If the {@link PublicKeyCredential} is created in
         * response a register request, this attribute’s value will be an {@link AuthenticatorAttestationResponse},
         * otherwise, the {@link PublicKeyCredential} was created in response to a sign request, and this attribute’s
         * value will be an {@link AuthenticatorAssertionResponse}.
         */
        public Builder setResponse(@NonNull AuthenticatorResponse response) {
            this.response = response;
            return this;
        }

        /**
         * Builds the {@link PublicKeyCredential} object.
         */
        public PublicKeyCredential build() {
            return new PublicKeyCredential(id, PublicKeyCredentialType.PUBLIC_KEY.toString(), rawId, response, extensionsClientOutputs, authenticatorAttachment);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PublicKeyCredential)) return false;

        PublicKeyCredential that = (PublicKeyCredential) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (!Arrays.equals(rawId, that.rawId)) return false;
        if (registerResponse != null ? !registerResponse.equals(that.registerResponse) : that.registerResponse != null) return false;
        if (signResponse != null ? !signResponse.equals(that.signResponse) : that.signResponse != null) return false;
        if (errorResponse != null ? !errorResponse.equals(that.errorResponse) : that.errorResponse != null) return false;
        if (clientExtensionResults != null ? !clientExtensionResults.equals(that.clientExtensionResults) : that.clientExtensionResults != null) return false;
        if (authenticatorAttachment != null ? !authenticatorAttachment.equals(that.authenticatorAttachment) : that.authenticatorAttachment != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{id, type, rawId, signResponse, registerResponse, errorResponse, clientExtensionResults, authenticatorAttachment});
    }

    /**
     * Serializes the {@link PublicKeyCredential} to bytes. Use {@link #deserializeFromBytes(byte[])} to deserialize.
     *
     * @return the serialized byte array.
     */
    @NonNull
    public byte[] serializeToBytes() {
        return SafeParcelableSerializer.serializeToBytes(this);
    }

    /**
     * Deserializes the {@link PublicKeyCredential} from bytes, reversing {@link #serializeToBytes()}.
     *
     * @param serializedBytes The serialized bytes.
     * @return The deserialized {@link PublicKeyCredential}.
     */
    @NonNull
    public static PublicKeyCredential deserializeFromBytes(byte[] serializedBytes) {
        return SafeParcelableSerializer.deserializeFromBytes(serializedBytes, CREATOR);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @Hide
    public static final SafeParcelableCreatorAndWriter<PublicKeyCredential> CREATOR = findCreator(PublicKeyCredential.class);
}
