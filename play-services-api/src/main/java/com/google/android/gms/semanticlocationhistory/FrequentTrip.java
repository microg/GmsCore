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
import com.google.android.gms.semanticlocation.PlaceCandidate;

import org.microg.gms.utils.ToStringHelper;

import java.util.List;

@SafeParcelable.Class
public class FrequentTrip extends AbstractSafeParcelable {

    @Field(1)
    public final List<PlaceCandidate.Identifier> waypointIds;
    @Field(2)
    public final Metadata metadata;
    @Field(3)
    public final List<FrequentTrip.ModeDistribution> modeDistributionList;
    @Field(4)
    public final int startTimeMinutes;
    @Field(5)
    public final int endTimeMinutes;
    @Field(6)
    public final int durationMinutes;
    @Field(7)
    public final float confidence;
    @Field(8)
    public final int commuteDirection;
    @Field(9)
    public final int fromType;
    @Field(10)
    public final int toType;

    @Constructor
    public FrequentTrip(@Param(1) List<PlaceCandidate.Identifier> waypointIds, @Param(2) Metadata metadata, @Param(3) List<FrequentTrip.ModeDistribution> modeDistributionList, @Param(4) int startTimeMinutes, @Param(5) int endTimeMinutes, @Param(6) int durationMinutes, @Param(7) float confidence, @Param(8) int commuteDirection, @Param(9) int fromType, @Param(10) int toType) {
        this.waypointIds = waypointIds;
        this.metadata = metadata;
        this.modeDistributionList = modeDistributionList;
        this.startTimeMinutes = startTimeMinutes;
        this.endTimeMinutes = endTimeMinutes;
        this.durationMinutes = durationMinutes;
        this.confidence = confidence;
        this.commuteDirection = commuteDirection;
        this.fromType = fromType;
        this.toType = toType;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("FrequentTrip")
                .field("waypointIds", waypointIds)
                .field("metadata", metadata)
                .field("modeDistributionList", modeDistributionList)
                .field("startTimeMinutes", startTimeMinutes)
                .field("endTimeMinutes", endTimeMinutes)
                .field("durationMinutes", durationMinutes)
                .field("confidence", confidence)
                .field("commuteDirection", commuteDirection)
                .field("fromType", fromType)
                .field("toType", toType)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<FrequentTrip> CREATOR = findCreator(FrequentTrip.class);

    @Class
    public static class Metadata extends AbstractSafeParcelable {
        @Field(1)
        public final long creationTime;

        @Constructor
        public Metadata(@Param(1) long creationTime) {
            this.creationTime = creationTime;
        }

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("Metadata")
                    .field("creationTime", creationTime)
                    .end();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<FrequentTrip.Metadata> CREATOR = findCreator(FrequentTrip.Metadata.class);
    }

    @Class
    public static class ModeDistribution extends AbstractSafeParcelable {
        @Field(1)
        public final int mode;
        @Field(2)
        public final float rate;

        @Constructor
        public ModeDistribution(@Param(1) int mode, @Param(2) float rate) {
            this.mode = mode;
            this.rate = rate;
        }

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("ModeDistribution")
                    .field("distance", mode)
                    .field("confidence", rate)
                    .end();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<FrequentTrip.ModeDistribution> CREATOR = findCreator(FrequentTrip.ModeDistribution.class);
    }

}
