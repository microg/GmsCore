/*
 * SPDX-FileCopyrightText: 2017 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.safetynet;

import org.microg.safeparcel.AutoSafeParcelable;

public class RemoveHarmfulAppData extends AutoSafeParcelable {
    @Field(2)
    public int field2;
    @Field(3)
    public boolean field3;

    public static final Creator<RemoveHarmfulAppData> CREATOR = new AutoCreator<RemoveHarmfulAppData>(RemoveHarmfulAppData.class);
}
