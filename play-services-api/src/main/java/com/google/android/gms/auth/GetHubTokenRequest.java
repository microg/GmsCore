/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetHubTokenRequest extends AutoSafeParcelable {
    @Field(1)
    public String accountName;
    @Field(2)
    public String service;
    @Field(3)
    public String packageName;
    @Field(4)
    public int callerUid;
    public static final Creator<GetHubTokenRequest> CREATOR = new AutoCreator<>(GetHubTokenRequest.class);
}
