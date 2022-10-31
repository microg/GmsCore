/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.measurement.api.internal;

import android.os.Bundle;

import org.microg.safeparcel.AutoSafeParcelable;

public class InitializationParams extends AutoSafeParcelable {
    @Field(1)
    public long field1;
    @Field(2)
    public long field2;
    @Field(3)
    public boolean field3;
    @Field(4)
    public String field4;
    @Field(5)
    public String field5;
    @Field(6)
    public String field6;
    @Field(7)
    public Bundle field7;
    @Field(8)
    public String field8;

    @Override
    public String toString() {
        return "InitializationParams{" +
                "field1=" + field1 +
                ", field2=" + field2 +
                ", field3=" + field3 +
                ", field4='" + field4 + '\'' +
                ", field5='" + field5 + '\'' +
                ", field6='" + field6 + '\'' +
                ", field7=" + field7 +
                ", field8='" + field8 + '\'' +
                '}';
    }

    public static final Creator<InitializationParams> CREATOR = new AutoCreator<>(InitializationParams.class);
}
