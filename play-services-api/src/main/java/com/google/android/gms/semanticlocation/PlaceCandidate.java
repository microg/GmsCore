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
public class PlaceCandidate extends AbstractSafeParcelable {

    @Field(1)
    public final Identifier identifier;
    @Field(2)
    public final int semanticType;
    @Field(3)
    public final float probability;
    @Field(4)
    @Deprecated
    float f4;
    @Field(5)
    public final Point placeLocation;
    @Field(6)
    public final boolean isSensitiveForGorUsage;
    @Field(7)
    public final boolean isEligibleForGorUsage;
    @Field(8)
    public final double semanticTypeConfidenceScore;

    @Constructor
    public PlaceCandidate(@Param(1) Identifier identifier, @Param(2) int semanticType, @Param(3) float probability, @Param(5) Point placeLocation, @Param(6) boolean isSensitiveForGorUsage, @Param(7) boolean isEligibleForGorUsage, @Param(8) double semanticTypeConfidenceScore){
        this.identifier = identifier;
        this.semanticType = semanticType;
        this.probability = probability;
        this.placeLocation = placeLocation;
        this.isSensitiveForGorUsage = isSensitiveForGorUsage;
        this.isEligibleForGorUsage = isEligibleForGorUsage;
        this.semanticTypeConfidenceScore = semanticTypeConfidenceScore;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PlaceCandidate> CREATOR = findCreator(PlaceCandidate.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("PlaceCandidate")
                .field("placeLocation", placeLocation)
                .field("isSensitiveForGorUsage", isSensitiveForGorUsage)
                .field("isEligibleForGorUsage", isEligibleForGorUsage)
                .field("semanticTypeConfidenceScore", semanticTypeConfidenceScore)
                .field("probability", probability)
                .field("identifier", identifier)
                .field("semanticType", semanticType)
                .end();
    }

    public static class Identifier extends AbstractSafeParcelable {
        @Field(1)
        public final long fprint;
        @Field(2)
        public final long cellId;

        @Constructor
        public Identifier(@Param(1) long fprint, @Param(1) long cellId) {
            this.fprint = fprint;
            this.cellId = cellId;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<Identifier> CREATOR = findCreator(Identifier.class);

        @NonNull
        @Override
        public String toString() {
            return "0x" + Long.toHexString(cellId) + ":0x" + Long.toHexString(fprint);
        }
    }

    public static class Point extends AbstractSafeParcelable {
        @Field(1)
        public final int latE7;
        @Field(2)
        public final int lngE7;

        @Constructor
        public Point(@Param(1) int latE7, @Param(1) int lngE7) {
            this.latE7 = latE7;
            this.lngE7 = lngE7;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<Point> CREATOR = findCreator(Point.class);

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("Point")
                    .field("latE7", latE7)
                    .field("lngE7", lngE7)
                    .end();
        }
    }
}
