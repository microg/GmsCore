/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.data;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class Subscription extends AbstractSafeParcelable {
    @Field(1)
    public DataSource dataSource;
    @Field(2)
    public DataType dataType;
    @Field(3)
    public long samplingIntervalMicros;
    @Field(4)
    public int accuracyMode;
    @Field(5)
    public int subscriptionType;

    @Constructor
    public Subscription(@Param(1) DataSource dataSource, @Param(2) DataType dataType, @Param(3) long samplingIntervalMicros, @Param(4) int accuracyMode, @Param(5) int subscriptionType) {
        this.dataSource = dataSource;
        this.dataType = dataType;
        this.samplingIntervalMicros = samplingIntervalMicros;
        this.accuracyMode = accuracyMode;
        this.subscriptionType = subscriptionType;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Subscription> CREATOR = findCreator(Subscription.class);
}
