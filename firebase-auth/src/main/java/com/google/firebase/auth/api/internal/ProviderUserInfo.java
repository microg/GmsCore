/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class ProviderUserInfo extends AutoSafeParcelable {
    @Field(2)
    public String federatedId;
    @Field(3)
    public String displayName;
    @Field(4)
    public String photoUrl;
    @Field(5)
    public String providerId;
    @Field(6)
    public String rawUserInfo;
    @Field(7)
    public String phoneNumber;
    @Field(8)
    public String email;

    public static final Creator<ProviderUserInfo> CREATOR = new AutoCreator<>(ProviderUserInfo.class);
}
