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
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.internal.IDailyTotalCallback;

@SafeParcelable.Class
public class DailyTotalRequest extends AbstractSafeParcelable {

    @Field(1)
    public IDailyTotalCallback callback;
    @Field(2)
    public DataType dataType;
    @Field(4)
    public Boolean unknownBool4;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DailyTotalRequest> CREATOR = findCreator(DailyTotalRequest.class);
}
