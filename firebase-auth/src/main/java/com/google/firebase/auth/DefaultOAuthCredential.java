/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.firebase.auth;

import com.google.firebase.auth.api.internal.VerifyAssertionRequest;

import org.microg.gms.common.PublicApi;

@PublicApi
public class DefaultOAuthCredential extends OAuthCredential {
    @Field(1)
    @PublicApi(exclude = true)
    public String provider;
    @Field(2)
    @PublicApi(exclude = true)
    public String idToken;
    @Field(3)
    @PublicApi(exclude = true)
    public String accessToken;
    @Field(4)
    @PublicApi(exclude = true)
    public VerifyAssertionRequest webSignInToken;
    @Field(5)
    @PublicApi(exclude = true)
    public String pendingToken;
    @Field(6)
    @PublicApi(exclude = true)
    public String secret;
    @Field(7)
    @PublicApi(exclude = true)
    public String rawNonce;


    @Override
    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public String getIdToken() {
        return idToken;
    }

    @Override
    public String getSecret() {
        return secret;
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public String getSignInMethod() {
        return provider;
    }

    public static final Creator<DefaultOAuthCredential> CREATOR = new AutoCreator<>(DefaultOAuthCredential.class);
}
