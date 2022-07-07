/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location.reporting;

import android.accounts.Account;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class OptInRequest extends AutoSafeParcelable {
    @Field(2)
    public Account account;
    @Field(3)
    public String tag;
    @Field(4)
    public String auditToken;

    public static final Creator<OptInRequest> CREATOR = new AutoCreator<OptInRequest>(OptInRequest.class);
}
