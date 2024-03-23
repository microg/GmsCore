/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.data;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

import java.util.List;

@SafeParcelable.Class
public class Bucket extends AbstractSafeParcelable {

    public static final int TYPE_TIME = 1;
    public static final int TYPE_SESSION = 2;
    public static final int TYPE_ACTIVITY_TYPE = 3;
    public static final int TYPE_ACTIVITY_SEGMENT = 4;

    @Field(1)
    public long startTimeMillis;
    @Field(2)
    public long endTimeMillis;
    @Field(3)
    public Session session;
    @Field(4)
    public int activityType;
    @Field(5)
    public List<DataSet> dataSets;
    @Field(6)
    public int bucketType;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Bucket> CREATOR = findCreator(Bucket.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("Bucket")
                .field("startTimeMillis", startTimeMillis)
                .field("endTimeMillis", endTimeMillis)
                .field("session", session)
                .field("activityType", activityType)
                .field("dataSets", dataSets)
                .field("bucketType", bucketType)
                .end();
    }
}
