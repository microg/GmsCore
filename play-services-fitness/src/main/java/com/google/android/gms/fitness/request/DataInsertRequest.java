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
public class DataInsertRequest extends AbstractSafeParcelable {
    @Field(1)
    public DataSet dataSet;
    @Field(2)
    public IStatusCallback callback;
    @Field(4)
    public boolean isPrimary;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DataInsertRequest> CREATOR = findCreator(DataInsertRequest.class);

}
