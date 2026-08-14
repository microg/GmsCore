/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.measurement.internal;

import androidx.annotation.Nullable;
import org.microg.safeparcel.AutoSafeParcelable;

public class UserAttributeParcel extends AutoSafeParcelable {
    @Field(1)
    public int versionCode = 2;
    @Field(2)
    public String name;
    @Field(3)
    public long timestamp;
    @Field(4)
    @Nullable
    public Long longValue;
    @Deprecated
    @Field(5)
    @Nullable
    public Float floatValue;
    @Field(6)
    @Nullable
    public String stringValue;
    @Field(7)
    public String field7;
    @Field(8)
    @Nullable
    public Double doubleValue;

    public static final Creator<UserAttributeParcel> CREATOR = new AutoCreator<>(UserAttributeParcel.class);
}
