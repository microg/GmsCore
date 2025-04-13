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
public class LocationHistorySegment extends AbstractSafeParcelable {

    @Field(1)
    public long startTime;
    @Field(2)
    public long endTime;
    @Field(3)
    public int unknownFiled3;
    @Field(4)
    public int unknownFiled4;
    @Field(7)
    public String function;
    @Field(8)
    public int unknownFiled8;
    @Field(12)
    public int unknownFiled12;
    @Field(13)
    public int unknownFiled13;

    public LocationHistorySegment() {
    }

    @Constructor
    public LocationHistorySegment(@Param(1) long startTime, @Param(2) long endTime, @Param(3) int unknownFiled3, @Param(4) int unknownFiled4, @Param(7) String function, @Param(8) int unknownFiled8 , @Param(12) int unknownFiled12, @Param(13) int unknownFiled13){
        this.startTime = startTime;
        this.endTime = endTime;
        this.unknownFiled3 = unknownFiled3;
        this.unknownFiled4 = unknownFiled4;
        this.function = function;
        this.unknownFiled8 = unknownFiled8;
        this.unknownFiled12 = unknownFiled12;
        this.unknownFiled13 = unknownFiled13;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<LocationHistorySegment> CREATOR = findCreator(LocationHistorySegment.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("LocationHistorySegment")
                .field("startTime", startTime)
                .field("endTime", endTime)
                .field("unknownFiled3", unknownFiled3)
                .field("unknownFiled4", unknownFiled4)
                .field("unknownFiled7", function)
                .field("unknownFiled8", unknownFiled8)
                .field("unknownFiled12", unknownFiled12)
                .field("unknownFiled13", unknownFiled13)
                .end();
    }
}
