/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.request;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.fitness.internal.IDataTypeCallback;

@SafeParcelable.Class
public class ReadDataTypeRequest extends AbstractSafeParcelable {
    public static final SafeParcelableCreatorAndWriter<ReadDataTypeRequest> CREATOR = findCreator(ReadDataTypeRequest.class);

    @Field(1)
    public String name;
    @Field(3)
    public IDataTypeCallback callback;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

}
