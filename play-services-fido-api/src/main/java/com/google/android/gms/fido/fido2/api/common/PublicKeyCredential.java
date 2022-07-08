/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParcelUtil;

import java.util.Arrays;

/**
 * This class is contains the attributes that are returned to the caller when a new credential is created, or a new
 * assertion is requested.
 */
@PublicApi
public class PublicKeyCredential extends AutoSafeParcelable {
    @Field(1)
    private String id;
    @Field(2)
    private String type;
    @Field(3)
    private byte[] rawId;
    @Field(4)
    private AuthenticatorAttestationResponse registerResponse;
    @Field(5)
    private AuthenticatorAssertionResponse signResponse;
    @Field(6)
    private AuthenticatorErrorResponse errorResponse;
    @Field(7)
    private AuthenticationExtensionsClientOutputs clientExtensionResults;

    public AuthenticationExtensionsClientOutputs getClientExtensionResults() {
        return clientExtensionResults;
    }

    public String getId() {
        return id;
    }

    public byte[] getRawId() {
        return rawId;
    }

    public AuthenticatorResponse getResponse() {
        if (registerResponse != null) return registerResponse;
        if (signResponse != null) return signResponse;
        if (errorResponse != null) return errorResponse;
        throw new IllegalStateException("No response set.");
    }

    public String getType() {
        return type;
    }

    /**
     * Builder for {@link PublicKeyCredential}.
     */
    public static class Builder {
        private String id;
        private byte[] rawId;
        private AuthenticatorResponse response;
        private AuthenticationExtensionsClientOutputs extensionsClientOutputs;

        /**
         * The constructor of {@link PublicKeyCredential.Builder}.
         */
        public Builder() {
        }

        /**
         * Sets the output produced by the client's processing of the extensions requested by the relying party.
         */
        public PublicKeyCredential.Builder setAuthenticationExtensionsClientOutputs(AuthenticationExtensionsClientOutputs extensionsClientOutputs) {
            this.extensionsClientOutputs = extensionsClientOutputs;
            return this;
        }

        /**
         * Sets the base64url encoding of the credential identifier.
         */
        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the raw value of the credential identifier.
         */
        public Builder setRawId(byte[] rawId) {
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
        public Builder setResponse(AuthenticatorResponse response) {
            this.response = response;
            return this;
        }

        /**
         * Builds the {@link PublicKeyCredential} object.
         */
        public PublicKeyCredential build() {
            PublicKeyCredential credential = new PublicKeyCredential();
            credential.id = id;
            credential.type = PublicKeyCredentialType.PUBLIC_KEY.toString();
            credential.rawId = rawId;
            credential.clientExtensionResults = extensionsClientOutputs;
            if (response instanceof AuthenticatorAttestationResponse) {
                credential.registerResponse = (AuthenticatorAttestationResponse) response;
            } else if (response instanceof AuthenticatorAssertionResponse) {
                credential.signResponse = (AuthenticatorAssertionResponse) response;
            } else if (response instanceof AuthenticatorErrorResponse) {
                credential.errorResponse = (AuthenticatorErrorResponse) response;
            }
            return credential;
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
        if (registerResponse != null ? !registerResponse.equals(that.registerResponse) : that.registerResponse != null)
            return false;
        if (signResponse != null ? !signResponse.equals(that.signResponse) : that.signResponse != null) return false;
        if (errorResponse != null ? !errorResponse.equals(that.errorResponse) : that.errorResponse != null)
            return false;
        return clientExtensionResults != null ? clientExtensionResults.equals(that.clientExtensionResults) : that.clientExtensionResults == null;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{id, type, rawId, signResponse, registerResponse, errorResponse, clientExtensionResults});
    }

    /**
     * Serializes the {@link PublicKeyCredential} to bytes. Use {@link #deserializeFromBytes(byte[])} to deserialize.
     *
     * @return the serialized byte array.
     */
    public byte[] serializeToBytes() {
        return SafeParcelUtil.asByteArray(this);
    }

    /**
     * Deserializes the {@link PublicKeyCredential} from bytes, reversing {@link #serializeToBytes()}.
     *
     * @param serializedBytes The serialized bytes.
     * @return The deserialized {@link PublicKeyCredential}.
     */
    public static PublicKeyCredential deserializeFromBytes(byte[] serializedBytes) {
        return SafeParcelUtil.fromByteArray(serializedBytes, CREATOR);
    }

    @PublicApi(exclude = true)
    public static final Creator<PublicKeyCredential> CREATOR = new AutoCreator<>(PublicKeyCredential.class);
}
