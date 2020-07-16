/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.auth.api.credentials;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

/**
 * Parameters for requesting a Credential, via Auth.CredentialsApi.request(). Instances can be
 * created using CredentialRequest.Builder.
 */
public class CredentialRequest extends AutoSafeParcelable {

    @SafeParceled(1000)
    private int versionCode = 1;

    @SafeParceled(1)
    private boolean passwordLoginSupported;

    @SafeParceled(2)
    private String[] accountTypes;

    @SafeParceled(3)
    private CredentialPickerConfig credentialPickerConfig;

    @SafeParceled(4)
    private CredentialPickerConfig credentialHintPickerConfig;

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
}
