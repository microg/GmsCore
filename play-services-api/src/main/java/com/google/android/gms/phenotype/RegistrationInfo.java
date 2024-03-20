/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.phenotype;

import org.microg.safeparcel.AutoSafeParcelable;

public class RegistrationInfo extends AutoSafeParcelable {
    @Field(1)
    public String packageName;
    @Field(2)
    public int version;
    @Field(3)
    public String[] field3;
    @Field(4)
    public byte[] field4;
    @Field(5)
    public boolean weak;
    @Field(6)
    public int[] field6;
    @Field(7)
    public String clientPackageName;

    public static final Creator<RegistrationInfo> CREATOR = new AutoCreator<>(RegistrationInfo.class);
}
