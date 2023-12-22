/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.identity;

import org.microg.safeparcel.AutoSafeParcelable;

public class SignInCredential extends AutoSafeParcelable {
    @Field(1)
    public String email;
    @Field(2)
    public String displayName;
    @Field(3)
    public String familyName;
    @Field(4)
    public String givenName;
    @Field(5)
    public String avatar;
    @Field(6)
    public String serverAuthCode;
    @Field(7)
    public String idToken;
    @Field(8)
    public String obfuscatedIdentifier;

    public static final Creator<SignInCredential> CREATOR = findCreator(SignInCredential.class);
}
