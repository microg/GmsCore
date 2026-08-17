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
public class PlaceEnterEvent extends AbstractSafeParcelable {
    @Field(1)
    public final int hierarchyLevel;
    @Field(2)
    public final float probability;
    @Field(3)
    @Deprecated
    float f3;
    @Field(4)
    public final PlaceCandidate topCandidate;
    @Field(5)
    public final AdditionalPlaceCandidates additionalPlaceCandidates;

    @Constructor
    public PlaceEnterEvent(@Param(1) int hierarchyLevel, @Param(2) float probability, @Param(4) PlaceCandidate topCandidate, @Param(5) AdditionalPlaceCandidates additionalPlaceCandidates) {
        this.hierarchyLevel = hierarchyLevel;
        this.probability = probability;
        this.topCandidate = topCandidate;
        this.additionalPlaceCandidates = additionalPlaceCandidates;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("PlaceEnterEvent")
                .field("hierarchyLevel", hierarchyLevel)
                .field("probability", probability)
                .field("topCandidate", topCandidate)
                .field("additionalPlaceCandidates", additionalPlaceCandidates)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PlaceEnterEvent> CREATOR = findCreator(PlaceEnterEvent.class);
}
