/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.firstparty.dataservice;

import org.microg.safeparcel.AutoSafeParcelable;

public class PACLConfig extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 1;
    @Field(2)
    public String visibleActions;
    @Field(3)
    public String data;

    public static final Creator<PACLConfig> CREATOR = new AutoCreator<PACLConfig>(PACLConfig.class);
}
