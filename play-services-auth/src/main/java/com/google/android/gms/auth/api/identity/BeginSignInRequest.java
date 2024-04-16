/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.identity;

import android.os.Parcel;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Configurations that can be used to filter acceptable types of credentials returned from a sign-in attempt.
 */
@SafeParcelable.Class
public class BeginSignInRequest extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getPasswordRequestOptions")
    private final PasswordRequestOptions passwordRequestOptions;
    @Field(value = 2, getterName = "getGoogleIdTokenRequestOptions")
    private final GoogleIdTokenRequestOptions googleIdTokenRequestOptions;
    @Field(value = 3, getterName = "getSessionId")
    private final String sessionId;
    @Field(value = 4, getterName = "isAutoSelectEnabled")
    private final boolean autoSelectEnabled;
    @Field(value = 5, getterName = "getTheme")
    private final int theme;
    @Field(value = 6, getterName = "getPasskeysRequestOptions")
    private final PasskeysRequestOptions passkeysRequestOptions;
    @Field(value = 7, getterName = "getPasskeyJsonRequestOptions")
    private final PasskeyJsonRequestOptions passkeyJsonRequestOptions;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("BeginSignInRequest")
                .field("PasswordRequestOptions", passwordRequestOptions)
                .field("GoogleIdTokenRequestOptions", googleIdTokenRequestOptions)
                .field("sessionId", sessionId)
                .field("autoSelectEnabled", autoSelectEnabled)
                .field("theme", theme)
                .field("PasskeysRequestOptions", passkeysRequestOptions)
                .field("PasskeyJsonRequestOptions", passkeyJsonRequestOptions)
                .end();
    }

    @Constructor
    BeginSignInRequest(@Param(1) PasswordRequestOptions passwordRequestOptions, @Param(2) GoogleIdTokenRequestOptions googleIdTokenRequestOptions, @Param(3) String sessionId, @Param(4) boolean autoSelectEnabled, @Param(5) int theme, @Param(6) PasskeysRequestOptions passkeysRequestOptions, @Param(7) PasskeyJsonRequestOptions passkeyJsonRequestOptions) {
        this.passwordRequestOptions = passwordRequestOptions;
        this.googleIdTokenRequestOptions = googleIdTokenRequestOptions;
        this.sessionId = sessionId;
        this.autoSelectEnabled = autoSelectEnabled;
        this.theme = theme;
        this.passkeysRequestOptions = passkeysRequestOptions;
        this.passkeyJsonRequestOptions = passkeyJsonRequestOptions;
    }

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    public GoogleIdTokenRequestOptions getGoogleIdTokenRequestOptions() {
        return googleIdTokenRequestOptions;
    }

    public PasskeyJsonRequestOptions getPasskeyJsonRequestOptions() {
        return passkeyJsonRequestOptions;
    }

    public PasskeysRequestOptions getPasskeysRequestOptions() {
        return passkeysRequestOptions;
    }

    public PasswordRequestOptions getPasswordRequestOptions() {
        return passwordRequestOptions;
    }

    @Hide
    public String getSessionId() {
        return sessionId;
    }

    @Hide
    public int getTheme() {
        return theme;
    }

    /**
     * Returns {@code true} if auto-select is requested, {@code false} otherwise.
     */
    public boolean isAutoSelectEnabled() {
        return autoSelectEnabled;
    }

    public static class Builder {

    }

    /**
     * Options for requesting Google ID token-backed credentials during sign-in.
     */
    @Class
    public static class GoogleIdTokenRequestOptions extends AbstractSafeParcelable {
        @Field(value = 1, getterName = "isSupported")
        private final boolean supported;
        @Field(value = 2, getterName = "getServerClientId")
        private final String serverClientId;
        @Field(value = 3, getterName = "getNonce")
        private final String nonce;
        @Field(value = 4, getterName = "filterByAuthorizedAccounts")
        private final boolean filterByAuthorizedAccounts;
        @Field(value = 5, getterName = "getLinkedServiceId")
        private final String linkedServiceId;
        @Field(value = 6, getterName = "getIdTokenDepositionScopes")
        private final List<String> idTokenDepositionScopes;
        @Field(value = 7, getterName = "requestVerifiedPhoneNumber")
        private final boolean requestVerifiedPhoneNumber;

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("GoogleIdTokenRequestOptions")
                    .field("supported", supported)
                    .field("serverClientId", serverClientId)
                    .field("nonce", nonce)
                    .field("filterByAuthorizedAccounts", filterByAuthorizedAccounts)
                    .field("linkedServiceId", linkedServiceId)
                    .field("idTokenDepositionScopes", idTokenDepositionScopes)
                    .field("requestVerifiedPhoneNumber", requestVerifiedPhoneNumber)
                    .end();
        }

        @Hide
        @Constructor
        public GoogleIdTokenRequestOptions(@Param(1) boolean supported, @Param(2) String serverClientId, @Param(3) String nonce, @Param(4) boolean filterByAuthorizedAccounts, @Param(5) String linkedServiceId, @Param(6) List<String> idTokenDepositionScopes, @Param(7) boolean requestVerifiedPhoneNumber) {
            this.supported = supported;
            this.serverClientId = serverClientId;
            this.nonce = nonce;
            this.filterByAuthorizedAccounts = filterByAuthorizedAccounts;
            this.linkedServiceId = linkedServiceId;
            this.idTokenDepositionScopes = idTokenDepositionScopes;
            this.requestVerifiedPhoneNumber = requestVerifiedPhoneNumber;
        }

        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        public boolean filterByAuthorizedAccounts() {
            return filterByAuthorizedAccounts;
        }

        public List<String> getIdTokenDepositionScopes() {
            return idTokenDepositionScopes;
        }

        public String getLinkedServiceId() {
            return linkedServiceId;
        }

        public String getNonce() {
            return nonce;
        }

        public String getServerClientId() {
            return serverClientId;
        }

        public boolean isSupported() {
            return supported;
        }

        /**
         * @deprecated No replacement.
         */
        @Deprecated
        public boolean requestVerifiedPhoneNumber() {
            return requestVerifiedPhoneNumber;
        }

        /**
         * Builder for {@link BeginSignInRequest.GoogleIdTokenRequestOptions}.
         */
        public static class Builder {
            private boolean supported;
            @Nullable
            private String serverClientId;
            @Nullable
            private String nonce;
            private boolean filterByAuthorizedAccounts = true;
            @Nullable
            private String linkedServiceId;
            @Nullable
            private List<String> idTokenDepositionScopes;
            private boolean requestVerifiedPhoneNumber;

            /**
             * Sets whether to support sign-in using Google accounts that are linked to your users' accounts.
             * <p>
             * When such a credential is selected, a Google ID token for the Google account that the selected account is linked to, will
             * first be deposited to your server and then returned to you. Similar to the regular sign-in, your backend could use the ID
             * token to sign the user in. Note that, the ID token deposition will only happen the first time signing in, using this credential.
             * Subsequent sign-ins will not require a deposition, since the ID token will already have been associated with your user's
             * account.
             *
             * @param linkedServiceId         service ID used when linking accounts to a Google account.
             * @param idTokenDepositionScopes Oauth scopes that Google will need to deposit a Google ID token to your application's backend server. This
             *                                is optional and can be set to null. Only required if Google requires particular scopes to be able to deposit
             *                                the ID token.
             */
            public Builder associateLinkedAccounts(@NonNull String linkedServiceId, @Nullable List<String> idTokenDepositionScopes) {
                this.linkedServiceId = linkedServiceId;
                this.idTokenDepositionScopes = idTokenDepositionScopes;
                return this;
            }

            /**
             * Returns the built {@link BeginSignInRequest.GoogleIdTokenRequestOptions}.
             */
            @NonNull
            public GoogleIdTokenRequestOptions build() {
                return new GoogleIdTokenRequestOptions(supported, serverClientId, nonce, filterByAuthorizedAccounts, linkedServiceId, idTokenDepositionScopes, requestVerifiedPhoneNumber);
            }

            /**
             * Sets whether to only allow the user to select from Google accounts that are already authorized to sign in to your
             * application. The default value is true.
             * <p>
             * If {@code true}, the user will not be able to select any Google account that would otherwise require explicit authorization to
             * share basic profile/email data with your application. This may reduce some friction in the sign-in user journey, and
             * guarantees that the returned credential is for a "returning user", but limits the user's freedom to choose among all the
             * Google accounts on the device.
             *
             * @param filterByAuthorizedAccounts whether to only allow the user to select from Google accounts that are already authorized to sign in to
             *                                   your application
             */
            @NonNull
            public Builder setFilterByAuthorizedAccounts(boolean filterByAuthorizedAccounts) {
                this.filterByAuthorizedAccounts = filterByAuthorizedAccounts;
                return this;
            }

            /**
             * Sets the nonce to use when generating a Google ID token.
             *
             * @param nonce the nonce to use during ID token generation
             */
            @NonNull
            public Builder setNonce(@Nullable String nonce) {
                this.nonce = nonce;
                return this;
            }

            /**
             * Sets whether to request for a verified phone number during sign-ups.
             * <p>
             * In order to use this feature, the
             * {@link BeginSignInRequest.GoogleIdTokenRequestOptions#filterByAuthorizedAccounts()} field must be explicitly set
             * to false, because this feature is only available during sign-ups.
             *
             * @deprecated No replacement.
             */
            @Deprecated
            @NonNull
            public Builder setRequestVerifiedPhoneNumber(boolean requestVerifiedPhoneNumber) {
                this.requestVerifiedPhoneNumber = requestVerifiedPhoneNumber;
                return this;
            }

            /**
             * Sets the server's client ID to use as the audience for Google ID tokens generated during the sign-in.
             *
             * @param serverClientId the client ID of the server to which the ID token will be issued
             */
            @NonNull
            public Builder setServerClientId(@NonNull String serverClientId) {
                this.serverClientId = serverClientId;
                return this;
            }

            /**
             * Sets whether Google ID token-backed credentials should be returned by the API.
             *
             * @param supported whether Google ID token-backed credentials should be returned
             */
            @NonNull
            public Builder setSupported(boolean supported) {
                this.supported = supported;
                return this;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GoogleIdTokenRequestOptions)) return false;

            GoogleIdTokenRequestOptions that = (GoogleIdTokenRequestOptions) o;

            if (supported != that.supported) return false;
            if (filterByAuthorizedAccounts != that.filterByAuthorizedAccounts) return false;
            if (requestVerifiedPhoneNumber != that.requestVerifiedPhoneNumber) return false;
            if (!Objects.equals(serverClientId, that.serverClientId)) return false;
            if (!Objects.equals(nonce, that.nonce)) return false;
            if (!Objects.equals(linkedServiceId, that.linkedServiceId)) return false;
            return Objects.equals(idTokenDepositionScopes, that.idTokenDepositionScopes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[]{supported, serverClientId, nonce, filterByAuthorizedAccounts, linkedServiceId, idTokenDepositionScopes, requestVerifiedPhoneNumber});
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<GoogleIdTokenRequestOptions> CREATOR = findCreator(GoogleIdTokenRequestOptions.class);
    }

    /**
     * Options for requesting passkeys during sign-in.
     */
    @Class
    public static class PasskeyJsonRequestOptions extends AbstractSafeParcelable {
        @Field(value = 1, getterName = "isSupported")
        private final boolean supported;
        @Field(value = 2, getterName = "getRequestJson")
        private final String requestJson;

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("PasskeyJsonRequestOptions")
                    .field("supported", supported)
                    .field("requestJson", requestJson)
                    .end();
        }

        @Constructor
        @Hide
        public PasskeyJsonRequestOptions(@Param(1) boolean supported, @Param(2) String requestJson) {
            this.supported = supported;
            this.requestJson = requestJson;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getRequestJson() {
            return requestJson;
        }

        public boolean isSupported() {
            return supported;
        }

        /**
         * Builder for {@link BeginSignInRequest.PasskeyJsonRequestOptions}.
         */
        public static class Builder {
            private boolean supported;
            private String requestJson;

            @NonNull
            public PasskeyJsonRequestOptions build() {
                return new PasskeyJsonRequestOptions(supported, requestJson);
            }

            /**
             * Sets the {@link PublicKeyCredentialRequestOptions} in JSON format.
             *
             * @param requestJson the JSON formatted representation of the WebAuthn request.
             */
            @NonNull
            public Builder setRequestJson(@NonNull String requestJson) {
                this.requestJson = requestJson;
                return this;
            }

            /**
             * Sets whether passkey credentials should be returned by this API.
             *
             * @param supported whether passkey credentials should be returned
             */
            @NonNull
            public Builder setSupported(boolean supported) {
                this.supported = supported;
                return this;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PasskeyJsonRequestOptions)) return false;

            PasskeyJsonRequestOptions that = (PasskeyJsonRequestOptions) o;

            if (supported != that.supported) return false;
            return Objects.equals(requestJson, that.requestJson);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[]{supported, requestJson});
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<PasskeyJsonRequestOptions> CREATOR = findCreator(PasskeyJsonRequestOptions.class);
    }

    /**
     * Options for requesting passkeys during sign-in.
     *
     * @deprecated Use {@link BeginSignInRequest.PasskeyJsonRequestOptions} instead.
     */
    @Deprecated
    @Class
    public static class PasskeysRequestOptions extends AbstractSafeParcelable {
        @Field(value = 1, getterName = "isSupported")
        private final boolean supported;
        @Field(value = 2, getterName = "getChallenge")
        private final byte[] challenge;
        @Field(value = 3, getterName = "getRpId")
        private final String rpId;

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("PasskeysRequestOptions")
                    .field("supported", supported)
                    .field("challenge", challenge)
                    .field("rpId", rpId)
                    .end();
        }

        @Constructor
        @Hide
        public PasskeysRequestOptions(@Param(1) boolean supported, @Param(2) byte[] challenge, @Param(3) String rpId) {
            this.supported = supported;
            this.challenge = challenge;
            this.rpId = rpId;
        }

        public static Builder builder() {
            return new Builder();
        }

        public byte[] getChallenge() {
            return challenge;
        }

        public String getRpId() {
            return rpId;
        }

        public boolean isSupported() {
            return supported;
        }

        /**
         * Builder for {@link BeginSignInRequest.PasskeysRequestOptions}.
         */
        public static class Builder {
            private boolean supported;
            private byte[] challenge;
            private String rpId;

            public PasskeysRequestOptions build() {
                return new PasskeysRequestOptions(supported, challenge, rpId);
            }

            /**
             * Sets the nonce value that the authenticator should sign using a private key corresponding to a public key credential that
             * is acceptable for this authentication session.
             *
             * @param challenge the challenge
             */
            @NonNull
            public Builder setChallenge(@NonNull byte[] challenge) {
                this.challenge = challenge;
                return this;
            }

            /**
             * Sets identifier for a relying party, on whose behalf a given authentication operation is being performed. A public key
             * credential can only be used for authentication with the same replying party it was registered with.
             * <p>
             * Note: the RpId should be an effective domain (without scheme or port).
             *
             * @param rpId identifier for a relying party
             */
            @NonNull
            public Builder setRpId(@NonNull String rpId) {
                this.rpId = rpId;
                return this;
            }

            /**
             * Sets whether passkey credentials should be returned by this API.
             *
             * @param supported whether passkey credentials should be returned
             */
            @NonNull
            public Builder setSupported(boolean supported) {
                this.supported = supported;
                return this;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PasskeysRequestOptions)) return false;

            PasskeysRequestOptions that = (PasskeysRequestOptions) o;

            if (supported != that.supported) return false;
            if (!Arrays.equals(challenge, that.challenge)) return false;
            return Objects.equals(rpId, that.rpId);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[]{supported, challenge, rpId});
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<PasskeysRequestOptions> CREATOR = findCreator(PasskeysRequestOptions.class);
    }

    /**
     * Options for requesting password-backed credentials during sign-in.
     */
    @Class
    public static class PasswordRequestOptions extends AbstractSafeParcelable {
        @Field(value = 1, getterName = "isSupported")
        public final boolean supported;

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("PasswordRequestOptions")
                    .field("supported", supported)
                    .end();
        }

        @Constructor
        @Hide
        public PasswordRequestOptions(@Param(1) boolean supported) {
            this.supported = supported;
        }

        public static Builder builder() {
            return new Builder();
        }

        public boolean isSupported() {
            return supported;
        }

        /**
         * Builder for {@link BeginSignInRequest.PasswordRequestOptions}.
         */
        public static class Builder {
            private boolean supported = false;

            /**
             * Returns the built {@link BeginSignInRequest.PasswordRequestOptions}.
             */
            @NonNull
            public PasswordRequestOptions build() {
                return new PasswordRequestOptions(supported);
            }

            /**
             * Sets whether password-backed credentials should be returned by the API.
             *
             * @param supported whether password-backed credentials should be returned
             */
            @NonNull
            public Builder setSupported(boolean supported) {
                this.supported = supported;
                return this;
            }
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<PasswordRequestOptions> CREATOR = findCreator(PasswordRequestOptions.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BeginSignInRequest)) return false;

        BeginSignInRequest that = (BeginSignInRequest) o;

        if (autoSelectEnabled != that.autoSelectEnabled) return false;
        if (theme != that.theme) return false;
        if (!Objects.equals(passwordRequestOptions, that.passwordRequestOptions)) return false;
        if (!Objects.equals(googleIdTokenRequestOptions, that.googleIdTokenRequestOptions))
            return false;
        if (!Objects.equals(sessionId, that.sessionId)) return false;
        if (!Objects.equals(passkeysRequestOptions, that.passkeysRequestOptions)) return false;
        return Objects.equals(passkeyJsonRequestOptions, that.passkeyJsonRequestOptions);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{passwordRequestOptions, googleIdTokenRequestOptions, sessionId, autoSelectEnabled, theme, passkeysRequestOptions, passkeyJsonRequestOptions});
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<BeginSignInRequest> CREATOR = findCreator(BeginSignInRequest.class);

}
