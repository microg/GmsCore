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

@SafeParcelable.Class
public class DataSourceQueryParams extends AbstractSafeParcelable {

    @Field(1)
    public DataSource dataSource;
    @Field(3)
    public long unknownLong3;
    @Field(4)
    public long unknownLong4;
    @Field(5)
    public int unknownInt5;
    @Field(6)
    public int unknownInt6;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DataSourceQueryParams> CREATOR = findCreator(DataSourceQueryParams.class);
}
