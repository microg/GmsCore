/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.result;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.fitness.data.DataType;

public class DataTypeResult extends AbstractSafeParcelable {
    public static final SafeParcelableCreatorAndWriter<DataTypeResult> CREATOR = findCreator(DataTypeResult.class);

    @Field(1)
    public Status status;
    @Field(3)
    public DataType dataType;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

}
