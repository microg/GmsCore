/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.credentials;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;

/**
 * Parameters for requesting the display of the hint picker, via {@link CredentalsApi#getHintPickerIntent()}.
 * Instances can be created using {@link HintRequest.Builder}.
 */
@PublicApi
public class HintRequest extends AutoSafeParcelable {
    @Field(1000)
    private final int versionCode = 2;

    @Field(1)
    private CredentialPickerConfig hintPickerConfig;
    @Field(2)
    private boolean emailAddressIdentifierSupported;
    @Field(3)
    private boolean phoneNumberIdentifierSupported;
    @Field(4)
    private String[] accountTypes;
    @Field(5)
    private boolean idTokenRequested = true;
    @Field(6)
    private String serverClientId;
    @Field(7)
    private String idTokenNonce;

    private HintRequest() {
    }

    public HintRequest(CredentialPickerConfig hintPickerConfig, boolean emailAddressIdentifierSupported, boolean phoneNumberIdentifierSupported, String[] accountTypes, boolean idTokenRequested, String serverClientId, String idTokenNonce) {
        this.hintPickerConfig = hintPickerConfig;
        this.emailAddressIdentifierSupported = emailAddressIdentifierSupported;
        this.phoneNumberIdentifierSupported = phoneNumberIdentifierSupported;
        this.accountTypes = accountTypes;
        this.idTokenRequested = idTokenRequested;
        this.serverClientId = serverClientId;
        this.idTokenNonce = idTokenNonce;
    }

    public String[] getAccountTypes() {
        return accountTypes;
    }

    public CredentialPickerConfig getHintPickerConfig() {
        return hintPickerConfig;
    }

    public String getIdTokenNonce() {
        return idTokenNonce;
    }

    public String getServerClientId() {
        return serverClientId;
    }

    public boolean isEmailAddressIdentifierSupported() {
        return emailAddressIdentifierSupported;
    }

    public boolean isPhoneNumberIdentifierSupported() {
        return phoneNumberIdentifierSupported;
    }

    public boolean isIdTokenRequested() {
        return idTokenRequested;
    }

    public static final Creator<HintRequest> CREATOR = new AutoCreator<>(HintRequest.class);

    @Override
    public String toString() {
        return "HintRequest{" +
                "hintPickerConfig=" + hintPickerConfig +
                ", emailAddressIdentifierSupported=" + emailAddressIdentifierSupported +
                ", phoneNumberIdentifierSupported=" + phoneNumberIdentifierSupported +
                ", accountTypes=" + Arrays.toString(accountTypes) +
                ", idTokenRequested=" + idTokenRequested +
                ", serverClientId='" + serverClientId + '\'' +
                ", idTokenNonce='" + idTokenNonce + '\'' +
                '}';
    }

    public static class Builder {
        private CredentialPickerConfig hintPickerConfig;
        private boolean emailAddressIdentifierSupported;
        private boolean phoneNumberIdentifierSupported;
        private String[] accountTypes;
        private boolean idTokenRequested = true;
        private String serverClientId;
        private String idTokenNonce;

        /**
         * Builds a {@link HintRequest}.
         */
        public HintRequest build() {
            return new HintRequest(hintPickerConfig, emailAddressIdentifierSupported, phoneNumberIdentifierSupported, accountTypes, idTokenRequested, serverClientId, idTokenNonce);
        }

        /**
         * Sets the account types (identity providers) that are accepted by this application.
         * It is strongly recommended that the strings listed in {@link IdentityProviders} be used for the most common
         * identity providers, and strings representing the login domain of the identity provider be used for any
         * others which are not listed.
         *
         * @param accountTypes The list of account types (identity providers) supported by the app.
         *                     typically in the form of the associated login domain for each identity provider.
         */
        public void setAccountTypes(String... accountTypes) {
            this.accountTypes = accountTypes.clone();
        }

        /**
         * Enables returning {@link Credential} hints where the identifier is an email address, intended for use with a password chosen by the user.
         */
        public void setEmailAddressIdentifierSupported(boolean emailAddressIdentifierSupported) {
            this.emailAddressIdentifierSupported = emailAddressIdentifierSupported;
        }

        /**
         * Sets the configuration for the hint picker dialog.
         */
        public void setHintPickerConfig(CredentialPickerConfig hintPickerConfig) {
            this.hintPickerConfig = hintPickerConfig;
        }

        /**
         * Specify a nonce value that should be included in any generated ID token for this request.
         */
        public void setIdTokenNonce(String idTokenNonce) {
            this.idTokenNonce = idTokenNonce;
        }

        /**
         * Specify whether an ID token should be acquired for hints, if available for the selected credential identifier.
         * This is enabled by default; disable this if your app does not use ID tokens as part of authentication to decrease latency in retrieving credentials and credential hints.
         */
        public void setIdTokenRequested(boolean idTokenRequested) {
            this.idTokenRequested = idTokenRequested;
        }

        /**
         * Enables returning {@link Credential} hints where the identifier is a phone number, intended for use with a password chosen by the user or SMS verification.
         */
        public void setPhoneNumberIdentifierSupported(boolean phoneNumberIdentifierSupported) {
            this.phoneNumberIdentifierSupported = phoneNumberIdentifierSupported;
        }

        /**
         * Specify the server client ID for the backend associated with this app.
         * If a Google ID token can be generated for a retrieved credential or hint, and the specified server client ID is correctly configured to be associated with the app, then it will be used as the audience of the generated token.
         * If a null value is specified, the default audience will be used for the generated ID token.
         */
        public void setServerClientId(String serverClientId) {
            this.serverClientId = serverClientId;
        }
    }
}
