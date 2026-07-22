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
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataSet;

import java.util.List;

@SafeParcelable.Class
public class DataReadResult extends AbstractSafeParcelable {

    @Field(1)
    public List<DataSet> rawDataSets;
    @Field(2)
    public Status status;
    @Field(3)
    public List<Bucket> rawBuckets;
    @Field(5)
    public int batchCount;
    @Field(6)
    public List<DataSet> uniqueDataSources;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DataReadResult> CREATOR = findCreator(DataReadResult.class);

}
