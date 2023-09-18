/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth;

import android.accounts.Account;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class HasCapabilitiesRequest extends AutoSafeParcelable {
    @Field(1)
    public Account account;
    @Field(2)
    public String[] capabilities;
    public static final Creator<HasCapabilitiesRequest> CREATOR = new AutoCreator<>(HasCapabilitiesRequest.class);
}
