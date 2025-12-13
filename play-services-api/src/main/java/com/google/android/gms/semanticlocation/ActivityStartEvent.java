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
public class ActivityStartEvent extends AbstractSafeParcelable {
    @Field(1)
    public final float probability;
    @Field(2)
    @Deprecated
    float f2;
    @Field(3)
    public final ActivityCandidate topCandidate;
    @Field(4)
    public final AdditionalActivityCandidates additionalActivityCandidates;

    @Constructor
    public ActivityStartEvent(@Param(1) float probability, @Param(3) ActivityCandidate topCandidate, @Param(4) AdditionalActivityCandidates additionalActivityCandidates) {
        this.probability = probability;
        this.topCandidate = topCandidate;
        this.additionalActivityCandidates = additionalActivityCandidates;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("ActivityStartEvent")
                .field("probability", probability)
                .field("topCandidate", topCandidate)
                .field("additionalActivityCandidates", additionalActivityCandidates)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ActivityStartEvent> CREATOR = findCreator(ActivityStartEvent.class);
}
