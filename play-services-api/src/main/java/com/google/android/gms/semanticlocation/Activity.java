/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocation;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class Activity extends AbstractSafeParcelable {
    @Field(1)
    public final PlaceCandidate.Point start;
    @Field(2)
    public final PlaceCandidate.Point end;
    @Field(3)
    public final float distanceMeters;
    @Field(4)
    public final float probability;
    @Field(5)
    @Deprecated
    float f5;
    @Field(6)
    public final ActivityCandidate activityCandidate;
    @Field(7)
    public final AdditionalActivityCandidates additionalActivityCandidates;
    @Field(8)
    public final Parking parking;

    @Constructor
    public Activity(@Param(1) PlaceCandidate.Point start, @Param(2) PlaceCandidate.Point end, @Param(3) float distanceMeters, @Param(4) float probability, @Param(6) ActivityCandidate activityCandidate, @Param(7) AdditionalActivityCandidates additionalActivityCandidates, @Param(8) Parking parking) {
        this.start = start;
        this.end = end;
        this.distanceMeters = distanceMeters;
        this.probability = probability;
        this.activityCandidate = activityCandidate;
        this.additionalActivityCandidates = additionalActivityCandidates;
        this.parking = parking;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("Activity")
                .field("start", start)
                .field("end", end)
                .field("distanceMeters", distanceMeters)
                .field("probability", probability)
                .field("activityCandidate", activityCandidate)
                .field("additionalActivityCandidates", additionalActivityCandidates)
                .field("parking", parking)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Activity> CREATOR = findCreator(Activity.class);
}
