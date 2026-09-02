/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.aang;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.List;

@Hide
public class GetAccountsRequest extends AutoSafeParcelable {
    @Field(1)
    public String accountType;
    @Field(2)
    public List<String> field2;
    @Field(3)
    public List<String> field3;
    @Field(4)
    public boolean includeRestrictedAccounts;

    public static final Creator<GetAccountsRequest> CREATOR = new AutoCreator<>(GetAccountsRequest.class);
}
