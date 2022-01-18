/*
 * SPDX-FileCopyrightText: 2017 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.safetynet;

import org.microg.safeparcel.AutoSafeParcelable;

public class HarmfulAppsInfo extends AutoSafeParcelable {
    @Field(2)
    public long field2;
    @Field(3)
    public HarmfulAppsData[] data;
    @Field(4)
    public int field4;
    @Field(5)
    public boolean field5;

    public static final Creator<HarmfulAppsInfo> CREATOR = new AutoCreator<HarmfulAppsInfo>(HarmfulAppsInfo.class);
}
