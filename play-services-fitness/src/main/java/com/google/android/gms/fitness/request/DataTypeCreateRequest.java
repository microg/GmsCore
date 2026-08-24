/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.request;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.fitness.internal.IDataTypeCallback;

import java.util.List;

@SafeParcelable.Class
public class DataTypeCreateRequest extends AbstractSafeParcelable {
    public static final SafeParcelableCreatorAndWriter<DataTypeCreateRequest> CREATOR = findCreator(DataTypeCreateRequest.class);

    @Field(1)
    public String name;
    @Field(2)
    public List<com.google.android.gms.fitness.data.Field> fields;
    @Field(3)
    public IDataTypeCallback callback;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

}
