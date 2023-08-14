/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;
import java.util.List;

/**
 * This class is used to supply options when creating a new credential.
 */
@PublicApi
public class PublicKeyCredentialCreationOptions extends RequestOptions {
    @Field(2)
    private PublicKeyCredentialRpEntity rp;
    @Field(3)
    private PublicKeyCredentialUserEntity user;
    @Field(4)
    private byte[] challenge;
    @Field(5)
    private List<PublicKeyCredentialParameters> parameters;
    @Field(6)
    private Double timeoutSeconds;
    @Field(7)
    private List<PublicKeyCredentialDescriptor> excludeList;
    @Field(8)
    private AuthenticatorSelectionCriteria authenticatorSelection;
    @Field(9)
    private Integer requestId;
    @Field(10)
    private TokenBinding tokenBinding;
    @Field(11)
    private AttestationConveyancePreference attestationConveyancePreference;
    @Field(12)
    private AuthenticationExtensions authenticationExtensions;

    public AttestationConveyancePreference getAttestationConveyancePreference() {
        return attestationConveyancePreference;
    }

    public String getAttestationConveyancePreferenceAsString() {
        return attestationConveyancePreference.toString();
    }

    @Override
    public AuthenticationExtensions getAuthenticationExtensions() {
        return authenticationExtensions;
    }

    public AuthenticatorSelectionCriteria getAuthenticatorSelection() {
        return authenticatorSelection;
    }

    @Override
    public byte[] getChallenge() {
        return challenge;
    }

    public List<PublicKeyCredentialDescriptor> getExcludeList() {
        return excludeList;
    }

    public List<PublicKeyCredentialParameters> getParameters() {
        return parameters;
    }

    @Override
    public Integer getRequestId() {
        return requestId;
    }

    public PublicKeyCredentialRpEntity getRp() {
        return rp;
    }

    @Override
    public Double getTimeoutSeconds() {
        return timeoutSeconds;
    }

    @Override
    public TokenBinding getTokenBinding() {
        return tokenBinding;
    }

    public PublicKeyCredentialUserEntity getUser() {
        return user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PublicKeyCredentialCreationOptions)) return false;

        PublicKeyCredentialCreationOptions that = (PublicKeyCredentialCreationOptions) o;

        if (rp != null ? !rp.equals(that.rp) : that.rp != null) return false;
        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        if (!Arrays.equals(challenge, that.challenge)) return false;
        if (parameters != null ? !parameters.equals(that.parameters) : that.parameters != null) return false;
        if (timeoutSeconds != null ? !timeoutSeconds.equals(that.timeoutSeconds) : that.timeoutSeconds != null)
            return false;
        if (excludeList != null ? !excludeList.equals(that.excludeList) : that.excludeList != null) return false;
        if (authenticatorSelection != null ? !authenticatorSelection.equals(that.authenticatorSelection) : that.authenticatorSelection != null)
            return false;
        if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) return false;
        if (tokenBinding != null ? !tokenBinding.equals(that.tokenBinding) : that.tokenBinding != null) return false;
        if (attestationConveyancePreference != that.attestationConveyancePreference) return false;
        return authenticationExtensions != null ? authenticationExtensions.equals(that.authenticationExtensions) : that.authenticationExtensions == null;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{rp, user, Arrays.hashCode(challenge), parameters, timeoutSeconds, excludeList, authenticatorSelection, requestId, tokenBinding, attestationConveyancePreference, authenticationExtensions});
    }

    @Override
    public String toString() {
        return ToStringHelper.name("PublicKeyCredentialCreationOptions")
                .field("rp", rp)
                .field("user", user)
                .field("challenge", challenge)
                .field("parameters", parameters)
                .field("timeoutSeconds", timeoutSeconds)
                .field("excludeList", excludeList)
                .field("authenticatorSelection", authenticatorSelection)
                .field("requestId", requestId)
                .field("tokenBinding", tokenBinding)
                .field("attestationConveyancePreference", attestationConveyancePreference)
                .field("authenticationExtensions", authenticationExtensions)
                .end();
    }

    /**
     * Builder for {@link PublicKeyCredentialCreationOptions}.
     */
    public static class Builder {
        private PublicKeyCredentialRpEntity rp;
        private PublicKeyCredentialUserEntity user;
        private byte[] challenge;
        private List<PublicKeyCredentialParameters> parameters;
        private Double timeoutSeconds;
        private List<PublicKeyCredentialDescriptor> excludeList;
        private AuthenticatorSelectionCriteria authenticatorSelection;
        private Integer requestId;
        private TokenBinding tokenBinding;
        private AttestationConveyancePreference attestationConveyancePreference;
        private AuthenticationExtensions authenticationExtensions;

        /**
         * The constructor of {@link PublicKeyCredentialCreationOptions.Builder}.
         */
        public Builder() {
        }

        /**
         * Sets the preference for obfuscation level of the returned attestation data.
         */
        public Builder setAttestationConveyancePreference(AttestationConveyancePreference attestationConveyancePreference) {
            this.attestationConveyancePreference = attestationConveyancePreference;
            return this;
        }

        /**
         * Sets additional extensions that may dictate some client behavior during an exchange with a connected
         * authenticator.
         */
        public Builder setAuthenticationExtensions(AuthenticationExtensions authenticationExtensions) {
            this.authenticationExtensions = authenticationExtensions;
            return this;
        }

        /**
         * Sets constraints on the type of authenticator that is acceptable for this session.
         */
        public Builder setAuthenticatorSelection(AuthenticatorSelectionCriteria authenticatorSelection) {
            this.authenticatorSelection = authenticatorSelection;
            return this;
        }

        /**
         * Sets the challenge to sign when generating the attestation for this request.
         */
        public Builder setChallenge(byte[] challenge) {
            this.challenge = challenge;
            return this;
        }

        /**
         * Sets a list of credentials that, if found on a connected authenticator, will preclude registration of that
         * authenticator with the relying party. This is often set to prevent re-registration of authenticators that
         * the relying party has already registered on behalf of the user.
         */
        public Builder setExcludeList(List<PublicKeyCredentialDescriptor> excludeList) {
            this.excludeList = excludeList;
            return this;
        }

        /**
         * Sets the {@link PublicKeyCredentialParameters} that constrain the type of credential to generate.
         */
        public Builder setParameters(List<PublicKeyCredentialParameters> parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Sets the request id in order to link together events into a single session (the span of events between the
         * time that the server initiates a single FIDO2 request to the client and receives reply) on a single device.
         */
        public Builder setRequestId(Integer requestId) {
            this.requestId = requestId;
            return this;
        }

        /**
         * Sets information for a relying party, on whose behalf a given registration operation is being performed.
         * <p>
         * Note: the RpId should be an effective domain (aka, without scheme or port); and it should also be in secure
         * context (aka https connection). Apps-facing API needs to check the package signature against Digital Asset
         * Links, whose resource is the RP ID with prepended "//". Privileged (browser) API doesn't need the check.
         */
        public Builder setRp(PublicKeyCredentialRpEntity rp) {
            this.rp = rp;
            return this;
        }

        /**
         * Sets a timeout that limits the duration of the registration session provided to the user.
         */
        public Builder setTimeoutSeconds(Double timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        /**
         * Sets the {@link TokenBinding} associated with the calling origin.
         */
        public Builder setTokenBinding(TokenBinding tokenBinding) {
            this.tokenBinding = tokenBinding;
            return this;
        }

        /**
         * Sets information about the user on whose behalf the relying party is registering a credential.
         */
        public Builder setUser(PublicKeyCredentialUserEntity user) {
            this.user = user;
            return this;
        }

        /**
         * Builds the {@link PublicKeyCredentialCreationOptions} object.
         */
        public PublicKeyCredentialCreationOptions build() {
            PublicKeyCredentialCreationOptions options = new PublicKeyCredentialCreationOptions();
            options.challenge = challenge;
            options.timeoutSeconds = timeoutSeconds;
            options.requestId = requestId;
            options.tokenBinding = tokenBinding;
            options.authenticationExtensions = authenticationExtensions;
            return options;
        }
    }

    /**
     * Deserializes the {@link PublicKeyCredentialCreationOptions} from bytes, reversing {@link #serializeToBytes()}.
     *
     * @param serializedBytes The serialized bytes.
     * @return The deserialized {@link PublicKeyCredentialCreationOptions}.
     */
    public static PublicKeyCredentialCreationOptions deserializeFromBytes(byte[] serializedBytes) {
        return SafeParcelableSerializer.deserializeFromBytes(serializedBytes, CREATOR);
    }

    @PublicApi(exclude = true)
    public static final Creator<PublicKeyCredentialCreationOptions> CREATOR = new AutoCreator<>(PublicKeyCredentialCreationOptions.class);
}
