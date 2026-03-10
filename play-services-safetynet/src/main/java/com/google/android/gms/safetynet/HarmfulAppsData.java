/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.safetynet;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * APK information pertaining to one potentially harmful app.
 */
@PublicApi
public class HarmfulAppsData extends AutoSafeParcelable {
    /**
     * The package name of the potentially harmful app.
     */
    @Field(2)
    public final String apkPackageName;
    /**
     * The SHA-256 of the potentially harmful app APK file.
     */
    @Field(3)
    public final byte[] apkSha256;
    /**
     * The potentially harmful app category defined in {@link VerifyAppsConstants}.
     */
    @Field(4)
    public final int apkCategory;

    private HarmfulAppsData() {
        apkPackageName = null;
        apkSha256 = null;
        apkCategory = 0;
    }

    @PublicApi(exclude = true)
    public HarmfulAppsData(String apkPackageName, byte[] apkSha256, int apkCategory) {
        this.apkPackageName = apkPackageName;
        this.apkSha256 = apkSha256;
        this.apkCategory = apkCategory;
    }

    public static final Creator<HarmfulAppsData> CREATOR = new AutoCreator<HarmfulAppsData>(HarmfulAppsData.class);
}
