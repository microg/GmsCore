/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.measurement.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class UserAttributeParcel extends AutoSafeParcelable {
    @Field(1)
    public int field1;
    @Field(2)
    public String name;
    @Field(3)
    public long timestamp;
    @Field(4)
    public Long field4;
    @Field(6)
    public String field6;
    @Field(7)
    public String field7;
    @Field(8)
    public Double field8;

    public static final Creator<UserAttributeParcel> CREATOR = new AutoCreator<>(UserAttributeParcel.class);
}
