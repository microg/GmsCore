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
import java.util.List;

/**
 * This class is used to supply an authentication request with the data it needs to generate an assertion.
 */
@PublicApi
@SafeParcelable.Class
public class PublicKeyCredentialRequestOptions extends RequestOptions {
    @Field(value = 2, getterName = "getChallenge")
    @NonNull
    private byte[] challenge;
    @Field(value = 3, getterName = "getTimeoutSeconds")
    @Nullable
    private Double timeoutSeconds;
    @Field(value = 4, getterName = "getRpId")
    @NonNull
    private String rpId;
    @Field(value = 5, getterName = "getAllowList")
    @Nullable
    private List<PublicKeyCredentialDescriptor> allowList;
    @Field(value = 6, getterName = "getRequestId")
    @Nullable
    private Integer requestId;
    @Field(value = 7, getterName = "getTokenBinding")
    @Nullable
    private TokenBinding tokenBinding;
    @Field(value = 8, getterName = "getRequireUserVerification")
    @Nullable
    private UserVerificationRequirement requireUserVerification;
    @Field(value = 9, getterName = "getAuthenticationExtensions")
    @Nullable
    private AuthenticationExtensions authenticationExtensions;
    @Field(10)
    @Nullable
    Long longRequestId;

    @Constructor
    public PublicKeyCredentialRequestOptions(@Param(2)@NonNull byte[] challenge,@Param(3) @Nullable Double timeoutSeconds, @Param(4)@NonNull String rpId, @Param(5)@Nullable List<PublicKeyCredentialDescriptor> allowList,@Param(6) @Nullable Integer requestId,@Param(7) @Nullable TokenBinding tokenBinding,@Param(8) @Nullable UserVerificationRequirement requireUserVerification, @Param(9)@Nullable AuthenticationExtensions authenticationExtensions) {
        this.challenge = challenge;
        this.timeoutSeconds = timeoutSeconds;
        this.rpId = rpId;
        this.allowList = allowList;
        this.requestId = requestId;
        this.tokenBinding = tokenBinding;
        this.requireUserVerification = requireUserVerification;
        this.authenticationExtensions = authenticationExtensions;
    }

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
        @Nullable
        private UserVerificationRequirement requireUserVerification;

        /**
         * The constructor of {@link PublicKeyCredentialRequestOptions.Builder}.
         */
        public Builder() {
        }

        /**
         * Sets a list of public key credentials which constrain authentication to authenticators that contain a
         * private key for at least one of the supplied public keys.
         */
        @NonNull
        public Builder setAllowList(@Nullable List<PublicKeyCredentialDescriptor> allowList) {
            this.allowList = allowList;
            return this;
        }

        /**
         * Sets additional extensions that may dictate some client behavior during an exchange with a connected
         * authenticator.
         */
        @NonNull
        public Builder setAuthenticationExtensions(@Nullable AuthenticationExtensions authenticationExtensions) {
            this.authenticationExtensions = authenticationExtensions;
            return this;
        }

        /**
         * Sets the nonce value that the authenticator should sign using a private key corresponding to a public key
         * credential that is acceptable for this authentication session.
         */
        @NonNull
        public Builder setChallenge(@NonNull byte[] challenge) {
            this.challenge = challenge;
            return this;
        }

        /**
         * Sets the request id in order to link together events into a single session (the span of events between the
         * time that the server initiates a single FIDO2 request to the client and receives reply) on a single device.
         * This field is optional.
         */
        @NonNull
        public Builder setRequestId(@Nullable Integer requestId) {
            this.requestId = requestId;
            return this;
        }

        @Hide
        @NonNull
        public Builder setRequireUserVerification(@Nullable UserVerificationRequirement requireUserVerification) {
            this.requireUserVerification = requireUserVerification;
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
        @NonNull
        public Builder setRpId(@NonNull String rpId) {
            this.rpId = rpId;
            return this;
        }

        @NonNull
        public Builder setTimeoutSeconds(@Nullable Double timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        /**
         * Sets the {@link TokenBinding} associated with the calling origin.
         */
        @NonNull
        public Builder setTokenBinding(@Nullable TokenBinding tokenBinding) {
            this.tokenBinding = tokenBinding;
            return this;
        }

        /**
         * Builds the {@link PublicKeyCredentialRequestOptions} object.
         */
        @NonNull
        public PublicKeyCredentialRequestOptions build() {
            return new PublicKeyCredentialRequestOptions(challenge, timeoutSeconds, rpId, allowList, requestId, tokenBinding, requireUserVerification, authenticationExtensions);
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

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @Hide
    public static final SafeParcelableCreatorAndWriter<PublicKeyCredentialRequestOptions> CREATOR = findCreator(PublicKeyCredentialRequestOptions.class);
}
