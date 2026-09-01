/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.aang;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.List;

@Hide
public class GetAccountsResponse extends AutoSafeParcelable {
    @Field(1)
    public List<GoogleAccount> accounts;
    @Field(2)
    public List<AccountWithAppRestrictionState> restrictedAccounts;

    public static final Creator<GetAccountsResponse> CREATOR = new AutoCreator<>(GetAccountsResponse.class);
}
