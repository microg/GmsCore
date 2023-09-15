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
import java.util.List;

/**
 * This class is used to supply an authentication request with the data it needs to generate an assertion.
 */
@PublicApi
public class PublicKeyCredentialRequestOptions extends RequestOptions {
    @Field(2)
    @NonNull
    private byte[] challenge;
    @Field(3)
    @Nullable
    private Double timeoutSeconds;
    @Field(4)
    @NonNull
    private String rpId;
    @Field(5)
    @Nullable
    private List<PublicKeyCredentialDescriptor> allowList;
    @Field(6)
    @Nullable
    private Integer requestId;
    @Field(7)
    @Nullable
    private TokenBinding tokenBinding;
    @Field(8)
    @Nullable
    private UserVerificationRequirement requireUserVerification;
    @Field(9)
    @Nullable
    private AuthenticationExtensions authenticationExtensions;
    @Field(10)
    @Nullable
    private Long longRequestId;

    @Nullable
    public List<PublicKeyCredentialDescriptor> getAllowList() {
        return allowList;
    }

    @Override
    @Nullable
    public AuthenticationExtensions getAuthenticationExtensions() {
        return authenticationExtensions;
    }

    @Hide
    @Nullable
    public UserVerificationRequirement getRequireUserVerification() {
        return requireUserVerification;
    }

    @Override
    @NonNull
    public byte[] getChallenge() {
        return challenge;
    }

    @Override
    @Nullable
    public Integer getRequestId() {
        return requestId;
    }

    @NonNull
    public String getRpId() {
        return rpId;
    }

    @Override
    @Nullable
    public Double getTimeoutSeconds() {
        return timeoutSeconds;
    }

    @Override
    @Nullable
    public TokenBinding getTokenBinding() {
        return tokenBinding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PublicKeyCredentialRequestOptions)) return false;

        PublicKeyCredentialRequestOptions that = (PublicKeyCredentialRequestOptions) o;

        if (!Arrays.equals(challenge, that.challenge)) return false;
        if (timeoutSeconds != null ? !timeoutSeconds.equals(that.timeoutSeconds) : that.timeoutSeconds != null)
            return false;
        if (rpId != null ? !rpId.equals(that.rpId) : that.rpId != null) return false;
        if (allowList != null ? !allowList.equals(that.allowList) : that.allowList != null) return false;
        if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) return false;
        if (tokenBinding != null ? !tokenBinding.equals(that.tokenBinding) : that.tokenBinding != null) return false;
        if (requireUserVerification != that.requireUserVerification) return false;
        return authenticationExtensions != null ? authenticationExtensions.equals(that.authenticationExtensions) : that.authenticationExtensions == null;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{Arrays.hashCode(challenge), timeoutSeconds, rpId, allowList, requestId, tokenBinding, requireUserVerification, authenticationExtensions});
    }

    @Override
    @NonNull
    public String toString() {
        return ToStringHelper.name("PublicKeyCredentialRequestOptions")
                .field("challenge", challenge)
                .field("timeoutSeconds", timeoutSeconds)
                .field("rpId", rpId)
                .field("allowList", allowList)
                .field("requestId", requestId)
                .field("tokenBinding", tokenBinding)
                .field("userVerificationRequirement", requireUserVerification)
                .field("authenticationExtensions", authenticationExtensions)
                .end();
    }

    /**
     * Builder for {@link PublicKeyCredentialRequestOptions}.
     */
    public static class Builder {
        @NonNull
        private byte[] challenge;
        @Nullable
        private Double timeoutSeconds;
        @NonNull
        private String rpId;
        @Nullable
        private List<PublicKeyCredentialDescriptor> allowList;
        @Nullable
        private Integer requestId;
        @Nullable
        private TokenBinding tokenBinding;
        @Nullable
        private AuthenticationExtensions authenticationExtensions;

        /**
         * The constructor of {@link PublicKeyCredentialRequestOptions.Builder}.
         */
        public Builder() {
        }

        /**
         * Sets a list of public key credentials which constrain authentication to authenticators that contain a
         * private key for at least one of the supplied public keys.
         */
        public Builder setAllowList(@Nullable List<PublicKeyCredentialDescriptor> allowList) {
            this.allowList = allowList;
            return this;
        }

        /**
         * Sets additional extensions that may dictate some client behavior during an exchange with a connected
         * authenticator.
         */
        public Builder setAuthenticationExtensions(@Nullable AuthenticationExtensions authenticationExtensions) {
            this.authenticationExtensions = authenticationExtensions;
            return this;
        }

        /**
         * Sets the nonce value that the authenticator should sign using a private key corresponding to a public key
         * credential that is acceptable for this authentication session.
         */
        public Builder setChallenge(@NonNull byte[] challenge) {
            this.challenge = challenge;
            return this;
        }

        /**
         * Sets the request id in order to link together events into a single session (the span of events between the
         * time that the server initiates a single FIDO2 request to the client and receives reply) on a single device.
         * This field is optional.
         */
        public Builder setRequestId(@Nullable Integer requestId) {
            this.requestId = requestId;
            return this;
        }

        /**
         * Sets identifier for a relying party, on whose behalf a given authentication operation is being performed.
         * A public key credential can only be used for authentication with the same replying party it was registered
         * with.
         * <p>
         * Note: the RpId should be an effective domain (aka, without scheme or port); and it should also be in secure
         * context (aka https connection). Apps-facing API needs to check the package signature against Digital Asset
         * Links, whose resource is the RP ID with prepended "//". Privileged (browser) API doesn't need the check.
         */
        public Builder setRpId(@NonNull String rpId) {
            this.rpId = rpId;
            return this;
        }

        public Builder setTimeoutSeconds(@Nullable Double timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        /**
         * Sets the {@link TokenBinding} associated with the calling origin.
         */
        public Builder setTokenBinding(@Nullable TokenBinding tokenBinding) {
            this.tokenBinding = tokenBinding;
            return this;
        }

        /**
         * Builds the {@link PublicKeyCredentialRequestOptions} object.
         */
        public PublicKeyCredentialRequestOptions build() {
            PublicKeyCredentialRequestOptions options = new PublicKeyCredentialRequestOptions();
            options.challenge = challenge;
            options.timeoutSeconds = timeoutSeconds;
            options.rpId = rpId;
            options.allowList = allowList;
            options.requestId = requestId;
            options.tokenBinding = tokenBinding;
            options.authenticationExtensions = authenticationExtensions;
            return options;
        }
    }

    /**
     * Deserializes the {@link PublicKeyCredentialRequestOptions} from bytes, reversing {@link #serializeToBytes()}.
     *
     * @param serializedBytes The serialized bytes.
     * @return The deserialized {@link PublicKeyCredentialRequestOptions}.
     */
    @NonNull
    public static PublicKeyCredentialRequestOptions deserializeFromBytes(byte[] serializedBytes) {
        return SafeParcelableSerializer.deserializeFromBytes(serializedBytes, CREATOR);
    }

    @Hide
    public static final Creator<PublicKeyCredentialRequestOptions> CREATOR = new AutoCreator<>(PublicKeyCredentialRequestOptions.class);
}
