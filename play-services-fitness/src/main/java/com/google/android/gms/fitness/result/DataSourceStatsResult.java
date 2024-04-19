/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.result;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.fitness.data.DataSource;

@SafeParcelable.Class
public class DataSourceStatsResult extends AbstractSafeParcelable {
    @Field(1)
    public DataSource dataSource;
    @Field(2)
    public long id;
    @Field(3)
    public boolean isRemote;
    @Field(4)
    public long minEndTimeNanos;
    @Field(5)
    public long maxEndTimeNanos;
    @Field(6)
    public long minContiguousTimeNanos;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DataSourceStatsResult> CREATOR = findCreator(DataSourceStatsResult.class);
}
