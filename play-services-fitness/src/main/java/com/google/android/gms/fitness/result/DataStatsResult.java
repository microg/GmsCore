/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.result;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.io.Closeable;
import java.util.List;

@SafeParcelable.Class
public class DataStatsResult extends AbstractSafeParcelable implements Closeable {
    @Field(1)
    public Status status;
    @Field(2)
    public List<DataSourceStatsResult> dataSourceStatsResultList;
    @Field(3)
    public long unknownLong3;
    @Field(4)
    public String unknownString4;
    @Field(5)
    public long unknownLong5;
    @Field(6)
    public DataHolder dataHolder;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DataStatsResult> CREATOR = findCreator(DataStatsResult.class);

    @Override
    public void close() {
        if (dataHolder != null) {
            dataHolder.close();
        }
    }
}
