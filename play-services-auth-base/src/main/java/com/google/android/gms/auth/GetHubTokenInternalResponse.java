/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth;

import android.accounts.Account;
import android.content.Intent;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class GetHubTokenInternalResponse extends AutoSafeParcelable {
    @Field(1)
    public TokenData tokenData;
    @Field(2)
    public String status;
    @Field(3)
    public Intent recoveryIntent;
    public static final Creator<GetHubTokenInternalResponse> CREATOR = new AutoCreator<>(GetHubTokenInternalResponse.class);
}
