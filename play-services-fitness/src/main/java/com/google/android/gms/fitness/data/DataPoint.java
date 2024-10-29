/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.data;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class DataPoint extends AbstractSafeParcelable {
    @Field(1)
    public DataSource dataSource;
    @Field(3)
    public long timestampStartNanos;
    @Field(4)
    public long timestampEndNanos;
    @Field(5)
    public Value[] values;
    @Field(6)
    public DataSource originalDataSource;
    @Field(7)
    public long originalDataSourceId;

    public DataPoint() {
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DataPoint> CREATOR = findCreator(DataPoint.class);

}
