/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.aang;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class GoogleAccount extends AutoSafeParcelable {
    @Field(1)
    public String obfuscatedGaiaId;
    @Field(2)
    public String type;
    @Field(3)
    public String name;

    public static final Creator<GoogleAccount> CREATOR = new AutoCreator<>(GoogleAccount.class);
}
