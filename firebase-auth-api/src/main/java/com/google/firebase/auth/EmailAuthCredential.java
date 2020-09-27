/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.firebase.auth;

import org.microg.gms.common.PublicApi;

/**
 * Wraps an email and password tuple for authentication purposes.
 */
@PublicApi
public class EmailAuthCredential extends AuthCredential {
    @Field(1)
    @PublicApi(exclude = true)
    public String email;
    @Field(2)
    @PublicApi(exclude = true)
    public String password;
    @Field(3)
    @PublicApi(exclude = true)
    public String signInLink;
    @Field(4)
    @PublicApi(exclude = true)
    public String cachedState;
    @Field(5)
    @PublicApi(exclude = true)
    public boolean isForLinking;

    /**
     * Returns the unique string identifier for the provider type with which the credential is associated.
     */
    @Override
    public String getProvider() {
        return "password";
    }

    /**
     * Returns either {@link EmailAuthProvider#EMAIL_LINK_SIGN_IN_METHOD} for a credential generated with {@link EmailAuthProvider#getCredentialWithLink(String, String)} or {@link EmailAuthProvider#EMAIL_PASSWORD_SIGN_IN_METHOD} for a credential generated with {@link EmailAuthProvider#getCredential(String, String)}.
     */
    @Override
    public String getSignInMethod() {
        if (password != null && !password.isEmpty()) {
            return "password";
        }
        return "emailLink";
    }

    public static final Creator<EmailAuthCredential> CREATOR = new AutoCreator<>(EmailAuthCredential.class);
}
