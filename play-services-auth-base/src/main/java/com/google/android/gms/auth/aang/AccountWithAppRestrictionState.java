/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.aang;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class AccountWithAppRestrictionState extends AutoSafeParcelable {
    @Field(1)
    public GoogleAccount account;
    @Field(2)
    public AppRestrictionState restrictionState;

    public static final Creator<AccountWithAppRestrictionState> CREATOR = new AutoCreator<>(AccountWithAppRestrictionState.class);
}
