/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.data;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public final class RawDataPoint extends AbstractSafeParcelable {
    @Field(1)
    final long timestampNanos;
    @Field(2)
    final long startTimeNanos;
    @Field(3)
    final Value[] values;
    @Field(4)
    final int dataSourceIndex;
    @Field(5)
    final int originalDataSourceIndex;
    @Field(6)
    final long rawTimestamp;

    @Constructor
    public RawDataPoint(@Param(1) long timestampNanos, @Param(2) long startTimeNanos,
                        @Param(3) Value[] values, @Param(4) int dataSourceIndex,
                        @Param(5) int originalDataSourceIndex, @Param(6) long rawTimestamp) {
        this.timestampNanos = timestampNanos;
        this.startTimeNanos = startTimeNanos;
        this.values = values;
        this.dataSourceIndex = dataSourceIndex;
        this.originalDataSourceIndex = originalDataSourceIndex;
        this.rawTimestamp = rawTimestamp;
    }

    RawDataPoint(DataPoint dataPoint, List<DataSource> dataSources) {
        this(dataPoint.getTimestampNanos(), dataPoint.getStartTimeNanos(), dataPoint.getValues(),
                indexOf(dataPoint.getDataSource(), dataSources),
                indexOf(dataPoint.getOriginalDataSourceIfSet(), dataSources), dataPoint.getRawTimestamp());
    }

    static int indexOf(DataSource dataSource, List<DataSource> dataSources) {
        if (dataSource == null) return -1;
        int index = dataSources.indexOf(dataSource);
        if (index < 0) {
            dataSources.add(dataSource);
            index = dataSources.size() - 1;
        }
        return index;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<RawDataPoint> CREATOR = findCreator(RawDataPoint.class);
}
