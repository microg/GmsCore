/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.data;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SafeParcelable.Class
public final class RawBucket extends AbstractSafeParcelable {
    @Field(1)
    final long startTimeMillis;
    @Field(2)
    final long endTimeMillis;
    @Field(3)
    @Nullable
    final Session session;
    @Field(4)
    final int activityType;
    @Field(5)
    final List<RawDataSet> dataSets;
    @Field(6)
    final int bucketType;

    @Constructor
    public RawBucket(@Param(1) long startTimeMillis, @Param(2) long endTimeMillis,
                     @Nullable @Param(3) Session session, @Param(4) int activityType,
                     @Param(5) List<RawDataSet> dataSets, @Param(6) int bucketType) {
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.session = session;
        this.activityType = activityType;
        this.dataSets = dataSets;
        this.bucketType = bucketType;
    }

    public RawBucket(Bucket bucket, List<DataSource> dataSources) {
        this.startTimeMillis = bucket.getStartTime(TimeUnit.MILLISECONDS);
        this.endTimeMillis = bucket.getEndTime(TimeUnit.MILLISECONDS);
        this.session = bucket.getSession();
        this.activityType = bucket.getActivityType();
        this.bucketType = bucket.getBucketType();
        List<DataSet> sets = bucket.getDataSets();
        this.dataSets = new ArrayList<>(sets.size());
        for (DataSet set : sets) dataSets.add(new RawDataSet(set, dataSources));
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<RawBucket> CREATOR = findCreator(RawBucket.class);
}
