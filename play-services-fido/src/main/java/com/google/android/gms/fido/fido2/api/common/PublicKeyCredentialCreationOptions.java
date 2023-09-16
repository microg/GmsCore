/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
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
 * This class is used to supply options when creating a new credential.
 */
@PublicApi
@SafeParcelable.Class
public class PublicKeyCredentialCreationOptions extends RequestOptions {
    @Field(value = 2, getterName = "getRp")
    @NonNull
    private PublicKeyCredentialRpEntity rp;
    @Field(value = 3, getterName = "getUser")
    @NonNull
    private PublicKeyCredentialUserEntity user;
    @Field(value = 4, getterName = "getChallenge")
    @NonNull
    private byte[] challenge;
    @Field(value = 5, getterName = "getParameters")
    @NonNull
    private List<PublicKeyCredentialParameters> parameters;
    @Field(value = 6, getterName = "getTimeoutSeconds")
    @Nullable
    private Double timeoutSeconds;
    @Field(value = 7, getterName = "getExcludeList")
    @Nullable
    private List<PublicKeyCredentialDescriptor> excludeList;
    @Field(value = 8, getterName = "getAuthenticatorSelection")
    @Nullable
    private AuthenticatorSelectionCriteria authenticatorSelection;
    @Field(value = 9, getterName = "getRequestId")
    @Nullable
    private Integer requestId;
    @Field(value = 10, getterName = "getTokenBinding")
    @Nullable
    private TokenBinding tokenBinding;
    @Field(value = 11, getterName = "getAttestationConveyancePreference")
    @Nullable
    private AttestationConveyancePreference attestationConveyancePreference;
    @Field(value = 12, getterName = "getAuthenticationExtensions")
    @Nullable
    private AuthenticationExtensions authenticationExtensions;

    @Constructor
    PublicKeyCredentialCreationOptions(@Param(2) @NonNull PublicKeyCredentialRpEntity rp, @Param(3) @NonNull PublicKeyCredentialUserEntity user, @Param(4) @NonNull byte[] challenge, @Param(5) @NonNull List<PublicKeyCredentialParameters> parameters, @Param(6) @Nullable Double timeoutSeconds, @Param(7) @Nullable List<PublicKeyCredentialDescriptor> excludeList, @Param(8) @Nullable AuthenticatorSelectionCriteria authenticatorSelection, @Param(9) @Nullable Integer requestId, @Param(10) @Nullable TokenBinding tokenBinding, @Param(11) @Nullable AttestationConveyancePreference attestationConveyancePreference, @Param(12) @Nullable AuthenticationExtensions authenticationExtensions) {
        this.rp = rp;
        this.user = user;
        this.challenge = challenge;
        this.parameters = parameters;
        this.timeoutSeconds = timeoutSeconds;
        this.excludeList = excludeList;
        this.authenticatorSelection = authenticatorSelection;
        this.requestId = requestId;
        this.tokenBinding = tokenBinding;
        this.attestationConveyancePreference = attestationConveyancePreference;
        this.authenticationExtensions = authenticationExtensions;
    }

    @Nullable
    public AttestationConveyancePreference getAttestationConveyancePreference() {
        return attestationConveyancePreference;
    }

    @Nullable
    public String getAttestationConveyancePreferenceAsString() {
        if (attestationConveyancePreference == null) return null;
        return attestationConveyancePreference.toString();
    }

    @Nullable
    @Override
    public AuthenticationExtensions getAuthenticationExtensions() {
        return authenticationExtensions;
    }

    @Nullable
    public AuthenticatorSelectionCriteria getAuthenticatorSelection() {
        return authenticatorSelection;
    }

    @NonNull
    @Override
    public byte[] getChallenge() {
        return challenge;
    }

    @Nullable
    public List<PublicKeyCredentialDescriptor> getExcludeList() {
        return excludeList;
    }

    @NonNull
    public List<PublicKeyCredentialParameters> getParameters() {
        return parameters;
    }

    @Nullable
    @Override
    public Integer getRequestId() {
        return requestId;
    }

    @NonNull
    public PublicKeyCredentialRpEntity getRp() {
        return rp;
    }

    @Nullable
    @Override
    public Double getTimeoutSeconds() {
        return timeoutSeconds;
    }

    @Nullable
    @Override
    public TokenBinding getTokenBinding() {
        return tokenBinding;
    }

    @NonNull
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
        @NonNull
        private PublicKeyCredentialRpEntity rp;
        @NonNull
        private PublicKeyCredentialUserEntity user;
        @NonNull
        private byte[] challenge;
        @NonNull
        private List<PublicKeyCredentialParameters> parameters;
        @Nullable
        private Double timeoutSeconds;
        @Nullable
        private List<PublicKeyCredentialDescriptor> excludeList;
        @Nullable
        private AuthenticatorSelectionCriteria authenticatorSelection;
        @Nullable
        private Integer requestId;
        @Nullable
        private TokenBinding tokenBinding;
        @Nullable
        private AttestationConveyancePreference attestationConveyancePreference;
        @Nullable
        private AuthenticationExtensions authenticationExtensions;

        /**
         * The constructor of {@link PublicKeyCredentialCreationOptions.Builder}.
         */
        public Builder() {
        }

        /**
         * Sets the preference for obfuscation level of the returned attestation data.
         */
        public Builder setAttestationConveyancePreference(@Nullable AttestationConveyancePreference attestationConveyancePreference) {
            this.attestationConveyancePreference = attestationConveyancePreference;
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
         * Sets constraints on the type of authenticator that is acceptable for this session.
         */
        public Builder setAuthenticatorSelection(@Nullable AuthenticatorSelectionCriteria authenticatorSelection) {
            this.authenticatorSelection = authenticatorSelection;
            return this;
        }

        /**
         * Sets the challenge to sign when generating the attestation for this request.
         */
        public Builder setChallenge(@NonNull byte[] challenge) {
            this.challenge = challenge;
            return this;
        }

        /**
         * Sets a list of credentials that, if found on a connected authenticator, will preclude registration of that
         * authenticator with the relying party. This is often set to prevent re-registration of authenticators that
         * the relying party has already registered on behalf of the user.
         */
        public Builder setExcludeList(@Nullable List<PublicKeyCredentialDescriptor> excludeList) {
            this.excludeList = excludeList;
            return this;
        }

        /**
         * Sets the {@link PublicKeyCredentialParameters} that constrain the type of credential to generate.
         */
        public Builder setParameters(@NonNull List<PublicKeyCredentialParameters> parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Sets the request id in order to link together events into a single session (the span of events between the
         * time that the server initiates a single FIDO2 request to the client and receives reply) on a single device.
         */
        public Builder setRequestId(@Nullable Integer requestId) {
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
        public Builder setRp(@NonNull PublicKeyCredentialRpEntity rp) {
            this.rp = rp;
            return this;
        }

        /**
         * Sets a timeout that limits the duration of the registration session provided to the user.
         */
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
         * Sets information about the user on whose behalf the relying party is registering a credential.
         */
        public Builder setUser(@NonNull PublicKeyCredentialUserEntity user) {
            this.user = user;
            return this;
        }

        /**
         * Builds the {@link PublicKeyCredentialCreationOptions} object.
         */
        public PublicKeyCredentialCreationOptions build() {
            return new PublicKeyCredentialCreationOptions(rp, user, challenge, parameters, timeoutSeconds, excludeList, authenticatorSelection, requestId, tokenBinding, attestationConveyancePreference, authenticationExtensions);
        }
    }

    /**
     * Deserializes the {@link PublicKeyCredentialCreationOptions} from bytes, reversing {@link #serializeToBytes()}.
     *
     * @param serializedBytes The serialized bytes.
     * @return The deserialized {@link PublicKeyCredentialCreationOptions}.
     */
    @NonNull
    public static PublicKeyCredentialCreationOptions deserializeFromBytes(byte[] serializedBytes) {
        return SafeParcelableSerializer.deserializeFromBytes(serializedBytes, CREATOR);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @Hide
    public static final SafeParcelableCreatorAndWriter<PublicKeyCredentialCreationOptions> CREATOR = findCreator(PublicKeyCredentialCreationOptions.class);
}
