/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import com.google.android.gms.semanticlocation.*;
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class LocationHistorySegment extends AbstractSafeParcelable {

    @Field(1)
    public final long startTimestamp;
    @Field(2)
    public final long endTimestamp;
    @Field(3)
    public final int startTimeTimezoneUtcOffsetMinutes;
    @Field(4)
    public final int endTimeTimezoneUtcOffsetMinutes;
    @Field(7)
    public final String segmentId;
    @Field(8)
    public final int type;
    @Field(9)
    public final Visit visit;
    @Field(10)
    public final Activity activity;
    @Field(11)
    public final TimelinePath timelinePath;
    @Field(12)
    public final int displayMode;
    @Field(13)
    public final int finalizationStatus;
    @Field(14)
    public final TimelineMemory timelineMemory;
    @Field(15)
    public final PeriodSummary periodSummary;

    @Constructor
    public LocationHistorySegment(@Param(1) long startTimestamp, @Param(2) long endTimestamp, @Param(3) int startTimeTimezoneUtcOffsetMinutes, @Param(4) int endTimeTimezoneUtcOffsetMinutes, @Param(7) String segmentId, @Param(8) int type, @Param(9) Visit visit, @Param(10) Activity activity, @Param(11) TimelinePath timelinePath, @Param(12) int displayMode, @Param(13) int finalizationStatus, @Param(14) TimelineMemory timelineMemory, @Param(15) PeriodSummary periodSummary) {
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.startTimeTimezoneUtcOffsetMinutes = startTimeTimezoneUtcOffsetMinutes;
        this.endTimeTimezoneUtcOffsetMinutes = endTimeTimezoneUtcOffsetMinutes;
        this.segmentId = segmentId;
        this.type = type;
        this.visit = visit;
        this.activity = activity;
        this.timelinePath = timelinePath;
        this.displayMode = displayMode;
        this.finalizationStatus = finalizationStatus;
        this.timelineMemory = timelineMemory;
        this.periodSummary = periodSummary;
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
                .field("startTime", startTimestamp)
                .field("endTime", endTimestamp)
                .field("startTimeTimezoneUtcOffsetMinutes", startTimeTimezoneUtcOffsetMinutes)
                .field("endTimeTimezoneUtcOffsetMinutes", endTimeTimezoneUtcOffsetMinutes)
                .field("segmentId", segmentId)
                .field("type", type)
                .field("visit", visit)
                .field("activity", activity)
                .field("timelinePath", timelinePath)
                .field("timelineMemory", timelineMemory)
                .field("periodSummary", periodSummary)
                .field("displayMode", displayMode)
                .field("finalizationStatus", finalizationStatus)
                .end();
    }
}
