/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.phenotype;

import org.microg.safeparcel.AutoSafeParcelable;

public class Configurations extends AutoSafeParcelable {
    @Field(2)
    public String snapshotToken;
    @Field(3)
    public String serverToken;
    @Field(4)
    public Configuration[] field4;
    @Field(5)
    public boolean field5;
    @Field(6)
    public byte[] field6;
    @Field(7)
    public long version;

    public static final Creator<Configurations> CREATOR = new AutoCreator<>(Configurations.class);
}
