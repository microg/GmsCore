/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import com.google.firebase.auth.DefaultOAuthCredential;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.List;

public class GetAccountInfoUser extends AutoSafeParcelable {
    @Field(2)
    public String localId;
    @Field(3)
    public String email;
    @Field(4)
    public boolean isEmailVerified;
    @Field(5)
    public String displayName;
    @Field(6)
    public String photoUrl;
    @Field(7)
    public ProviderUserInfoList providerInfoList = new ProviderUserInfoList();
    @Field(8)
    public String password;
    @Field(9)
    public String phoneNumber;
    @Field(10)
    public long creationTimestamp;
    @Field(11)
    public long lastSignInTimestamp;
    @Field(12)
    public boolean isNewUser;
    @Field(13)
    public DefaultOAuthCredential defaultOAuthCredential;
    @Field(14)
    public List<MfaInfo> mfaInfoList;

    public static final Creator<GetAccountInfoUser> CREATOR = new AutoCreator<>(GetAccountInfoUser.class);
}
