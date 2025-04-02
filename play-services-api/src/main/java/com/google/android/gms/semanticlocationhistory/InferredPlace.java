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
import com.google.android.gms.semanticlocation.PlaceCandidate;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class InferredPlace extends AbstractSafeParcelable {

    @Field(1)
    public PlaceCandidate.Identifier identifier;
    @Field(2)
    public PlaceCandidate.Point point;
    @Field(3)
    public int inferredPlaceType;

    public InferredPlace() {
    }

    @Constructor
    public InferredPlace(@Param(1) PlaceCandidate.Identifier identifier, @Param(2) PlaceCandidate.Point point, @Param(3) int inferredPlaceType) {
        this.identifier = identifier;
        this.point = point;
        this.inferredPlaceType = inferredPlaceType;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<InferredPlace> CREATOR = findCreator(InferredPlace.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("InferredPlace")
                .field("identifier", identifier)
                .field("point", point)
                .field("inferredPlaceType", inferredPlaceType)
                .end();
    }
}
