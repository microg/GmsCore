/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.aang;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class AppRestrictionInfo extends AutoSafeParcelable {
    @Field(1)
    public String field1;
    @Field(3)
    public String field3;
    @Field(4)
    public String field4;
    @Field(5)
    public String field5;
    @Field(6)
    public byte[] field6;

    public static final Creator<AppRestrictionInfo> CREATOR = new AutoCreator<>(AppRestrictionInfo.class);
}
