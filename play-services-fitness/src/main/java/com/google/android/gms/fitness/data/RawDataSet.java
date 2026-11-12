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

import java.util.ArrayList;
import java.util.List;

@SafeParcelable.Class
public final class RawDataSet extends AbstractSafeParcelable {
    @Field(1)
    final int dataSourceIndex;
    @Field(3)
    final List<RawDataPoint> dataPoints;

    @Constructor
    public RawDataSet(@Param(1) int dataSourceIndex, @Param(3) List<RawDataPoint> dataPoints) {
        this.dataSourceIndex = dataSourceIndex;
        this.dataPoints = dataPoints;
    }

    public RawDataSet(DataSet dataSet, List<DataSource> dataSources) {
        this.dataSourceIndex = RawDataPoint.indexOf(dataSet.getDataSource(), dataSources);
        List<DataPoint> points = dataSet.getRawDataPoints();
        this.dataPoints = new ArrayList<>(points.size());
        for (DataPoint point : points) dataPoints.add(new RawDataPoint(point, dataSources));
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<RawDataSet> CREATOR = findCreator(RawDataSet.class);
}
