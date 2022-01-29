/*
 * SPDX-FileCopyrightText: 2016, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.credentials;

import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Parameters for requesting a Credential, via Auth.CredentialsApi.request(). Instances can be
 * created using CredentialRequest.Builder.
 */
public class CredentialRequest extends AutoSafeParcelable {

    @Field(1000)
    private final int versionCode = 1;

    @Field(1)
    private boolean passwordLoginSupported;
    @Field(2)
    private String[] accountTypes;
    @Field(3)
    private CredentialPickerConfig credentialPickerConfig;
    @Field(4)
    private CredentialPickerConfig credentialHintPickerConfig;

    private CredentialRequest() { }

    public CredentialRequest(boolean passwordLoginSupported, String[] accountTypes, CredentialPickerConfig credentialPickerConfig, CredentialPickerConfig credentialHintPickerConfig) {
        this.passwordLoginSupported = passwordLoginSupported;
        this.accountTypes = accountTypes;
        this.credentialPickerConfig = credentialPickerConfig;
        this.credentialHintPickerConfig = credentialHintPickerConfig;
    }

    public String[] getAccountTypes() {
        return accountTypes;
    }

    public CredentialPickerConfig getCredentialHintPickerConfig() {
        return credentialHintPickerConfig;
    }

    public CredentialPickerConfig getCredentialPickerConfig() {
        return credentialPickerConfig;
    }

    /**
     * @deprecated Use {@link #isPasswordLoginSupported()}
     */
    @Deprecated
    public boolean getSupportsPasswordLogin() {
        return isPasswordLoginSupported();
    }

    public boolean isPasswordLoginSupported() {
        return passwordLoginSupported;
    }

    public static final Creator<CredentialRequest> CREATOR = new AutoCreator<CredentialRequest>(CredentialRequest.class);

    public static class Builder {
        private boolean passwordLoginSupported;
        private String[] accountTypes;
        private CredentialPickerConfig credentialPickerConfig;
        private CredentialPickerConfig credentialHintPickerConfig;

        public void setAccountTypes(String... accountTypes) {
            this.accountTypes = accountTypes.clone();
        }
    }
}
