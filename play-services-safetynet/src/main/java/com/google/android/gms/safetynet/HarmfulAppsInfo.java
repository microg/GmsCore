/*
 * SPDX-FileCopyrightText: 2017 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.safetynet;

import org.microg.safeparcel.AutoSafeParcelable;

public class HarmfulAppsInfo extends AutoSafeParcelable {
    @Field(2)
    public long lastScanTime;
    @Field(3)
    public HarmfulAppsData[] harmfulApps = new HarmfulAppsData[0];
    @Field(4)
    public int hoursSinceLastScanWithHarmfulApp = -1;
    @Field(5)
    public boolean harmfulAppInOtherProfile = false;

    public static final Creator<HarmfulAppsInfo> CREATOR = new AutoCreator<HarmfulAppsInfo>(HarmfulAppsInfo.class);
}
