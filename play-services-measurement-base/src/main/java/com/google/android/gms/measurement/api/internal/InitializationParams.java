/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.measurement.api.internal;

import android.os.Bundle;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class InitializationParams extends AbstractSafeParcelable {
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
    @NonNull
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

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<InitializationParams> CREATOR = findCreator(InitializationParams.class);
}
