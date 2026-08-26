/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.request;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.RawBucket;
import com.google.android.gms.fitness.data.RawDataSet;

import java.util.List;

@SafeParcelable.Class
public class DataReadResult extends AbstractSafeParcelable {

    @Field(value = 1, useValueParcel = true)
    public List<RawDataSet> rawDataSets;
    @Field(2)
    public Status status;
    @Field(value = 3, useValueParcel = true)
    public List<RawBucket> rawBuckets;
    @Field(5)
    public int batchCount;
    @Field(6)
    public List<DataSource> uniqueDataSources;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DataReadResult> CREATOR = findCreator(DataReadResult.class);

}
