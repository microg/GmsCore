/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.aang;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class AppRestrictionState extends AutoSafeParcelable {
    @Field(1)
    public boolean restricted;
    @Field(2)
    public boolean accountHidden;

    public static final Creator<AppRestrictionState> CREATOR = new AutoCreator<>(AppRestrictionState.class);
}
