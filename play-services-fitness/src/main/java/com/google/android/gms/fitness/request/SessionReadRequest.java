/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
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
import com.google.android.gms.fitness.internal.ISessionReadCallback;

import java.util.List;

@SafeParcelable.Class
public class SessionReadRequest extends AbstractSafeParcelable {
    @Field(1)
    public String sessionName;
    @Field(2)
    public String sessionId;
    @Field(3)
    public long StartTimeMillis;
    @Field(4)
    public long EndTimeMillis;
    @Field(5)
    public List<DataType> dataTypes;
    @Field(6)
    public List<DataSource> dataSources;
    @Field(7)
    public boolean includeSessionsFromAllApps;
    @Field(8)
    public boolean areServerQueriesEnabled;
    @Field(9)
    public List<String> excludedPackages;
    @Field(10)
    public ISessionReadCallback callback;
    @Field(value = 12, defaultValue = "true")
    public boolean areActivitySessionsIncluded;
    @Field(value = 13, defaultValue = "false")
    public boolean areSleepSessionsIncluded;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SessionReadRequest> CREATOR = findCreator(SessionReadRequest.class);
}
