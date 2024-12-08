/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.data;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.Map;

@SafeParcelable.Class
public final class Value extends AbstractSafeParcelable {
    public static final SafeParcelableCreatorAndWriter<Value> CREATOR = findCreator(Value.class);
    @Field(1)
    public int valueType;
    @Field(2)
    public boolean isSet;
    @Field(3)
    public float Value;
    @Field(4)
    public String StringValue;
    public Map MapValue;
    @Field(6)
    public int[] IntArrayValue;
    @Field(7)
    public float[] FloatArrayValue;
    @Field(8)
    public byte[] Blob;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

}