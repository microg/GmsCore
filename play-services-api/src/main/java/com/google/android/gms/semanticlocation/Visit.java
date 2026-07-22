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
public class Visit extends AbstractSafeParcelable {
    @Field(1)
    public final int hierarchyLevel;
    @Field(2)
    public final float probability;
    @Field(3)
    @Deprecated
    float f3;
    @Field(4)
    public final PlaceCandidate place;
    @Field(5)
    public final AdditionalPlaceCandidates additionalPlaceCandidates;
    @Field(6)
    public final boolean isTimelessVisit;
    @Field(7)
    public final TemporarilyClosedPlaceCandidates temporarilyClosedPlaceCandidates;
    @Field(8)
    boolean b8;

    @Constructor
    public Visit(@Param(1) int hierarchyLevel, @Param(2) float probability, @Param(4) PlaceCandidate place, @Param(5) AdditionalPlaceCandidates additionalPlaceCandidates, @Param(6) boolean isTimelessVisit, @Param(7) TemporarilyClosedPlaceCandidates temporarilyClosedPlaceCandidates) {
        this.hierarchyLevel = hierarchyLevel;
        this.probability = probability;
        this.place = place;
        this.additionalPlaceCandidates = additionalPlaceCandidates;
        this.isTimelessVisit = isTimelessVisit;
        this.temporarilyClosedPlaceCandidates = temporarilyClosedPlaceCandidates;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("Visit")
                .field("hierarchyLevel", hierarchyLevel)
                .field("probability", probability)
                .field("place", place)
                .field("additionalPlaceCandidates", additionalPlaceCandidates)
                .field("isTimelessVisit", isTimelessVisit)
                .field("temporarilyClosedPlaceCandidates", temporarilyClosedPlaceCandidates)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Visit> CREATOR = findCreator(Visit.class);
}
