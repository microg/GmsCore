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
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.internal.IStatusCallback;

import org.microg.gms.utils.ToStringHelper;

import java.util.List;

@SafeParcelable.Class
public class DataDeleteRequest extends AbstractSafeParcelable {
    @Field(1)
    public long startTimeMillis;
    @Field(2)
    public long endTimeMillis;
    @Field(3)
    public List<DataSource> dataSources;
    @Field(4)
    public List<DataType> dataTypes;
    @Field(5)
    public List<Session> sessions;
    @Field(6)
    public boolean deleteAllData;
    @Field(7)
    public boolean deleteAllSessions;
    @Field(8)
    public IStatusCallback callback;
    @Field(10)
    public boolean deleteByTimeRange;
    @Field(11)
    public boolean enableLocationCleanup;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("DataDeleteRequest")
                .field("startTimeMillis", startTimeMillis)
                .field("endTimeMillis", endTimeMillis)
                .field("dataSources", dataSources)
                .field("dataTypes", dataTypes)
                .field("sessions", sessions)
                .field("deleteAllData", deleteAllData)
                .field("deleteAllSessions", deleteAllSessions)
                .field("callback", callback)
                .field("deleteByTimeRange", deleteByTimeRange)
                .field("enableLocationCleanup", enableLocationCleanup)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DataDeleteRequest> CREATOR = findCreator(DataDeleteRequest.class);
}
