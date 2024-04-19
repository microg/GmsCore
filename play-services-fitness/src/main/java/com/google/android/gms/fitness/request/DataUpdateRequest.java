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
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.internal.IStatusCallback;

@SafeParcelable.Class
public class DataUpdateRequest extends AbstractSafeParcelable {

    @Field(1)
    public Long startTimeMillis;
    @Field(2)
    public Long endTimeMillis;
    @Field(3)
    public DataSet dataSet;
    @Field(4)
    public IStatusCallback callback;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DataUpdateRequest> CREATOR = findCreator(DataUpdateRequest.class);
}
