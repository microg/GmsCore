/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.aang;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class AppRestriction extends AutoSafeParcelable {
    @Field(1)
    public AppRestrictionState restrictionState;
    @Field(2)
    public AppRestrictionInfo restrictionInfo;

    public static final Creator<AppRestriction> CREATOR = new AutoCreator<>(AppRestriction.class);
}
