/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class TimeRangeFilter extends AbstractSafeParcelable {

    @Field(1)
    public Long startTime;
    @Field(2)
    public Long endTime;

    public TimeRangeFilter() {
    }

    @Constructor
    public TimeRangeFilter(@Param(1) Long startTime, @Param(1) Long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<TimeRangeFilter> CREATOR = findCreator(TimeRangeFilter.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("TimeRangeFilter").field("startTime", startTime).field("endTime", endTime).end();
    }
}
