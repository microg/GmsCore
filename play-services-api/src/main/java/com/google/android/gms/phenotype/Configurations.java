/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.phenotype;

import org.microg.safeparcel.AutoSafeParcelable;

public class Configurations extends AutoSafeParcelable {
    @Field(2)
    public String field2;
    @Field(3)
    public String field3;
    @Field(4)
    public Configuration[] field4;
    @Field(5)
    public boolean field5;
    @Field(6)
    public byte[] field6;
    @Field(7)
    public long field7;

    public static final Creator<Configurations> CREATOR = new AutoCreator<>(Configurations.class);
}
