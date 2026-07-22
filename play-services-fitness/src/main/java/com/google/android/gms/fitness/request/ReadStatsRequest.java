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
import com.google.android.gms.fitness.internal.IReadStatsCallback;

import java.util.List;

@SafeParcelable.Class
public class ReadStatsRequest extends AbstractSafeParcelable {

    @Field(1000)
    public int versionCode;
    @Field(1)
    public IReadStatsCallback callback;
    @Field(3)
    public List<DataSource> dataSources;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ReadStatsRequest> CREATOR = findCreator(ReadStatsRequest.class);
}
