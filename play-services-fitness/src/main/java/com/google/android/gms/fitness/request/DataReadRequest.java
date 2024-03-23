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
import com.google.android.gms.fitness.data.DataSource;

import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.internal.IDataReadCallback;

import java.util.List;

@SafeParcelable.Class
public class DataReadRequest extends AbstractSafeParcelable {

    @Field(1)
    public List<DataType> dataTypes;
    @Field(2)
    public List<DataSource> dataSources;
    @Field(3)
    public long startTimeMillis;
    @Field(4)
    public long endTimeMillis;
    @Field(5)
    public List<DataType> aggregatedDataTypes;
    @Field(6)
    public List<DataSource> aggregatedDataSources;
    @Field(7)
    public int bucketType;
    @Field(8)
    public long bucketDurationMillis;
    @Field(9)
    public DataSource activityDataSource;
    @Field(10)
    public int limit;
    @Field(12)
    public boolean flushBufferBeforeRead;
    @Field(13)
    public boolean areServerQueriesEnabled;
    @Field(14)
    public IDataReadCallback callback;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DataReadRequest> CREATOR = findCreator(DataReadRequest.class);

}
