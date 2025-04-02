/**
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
    public Identifier identifier;
    @Field(2)
    public int confidence;
    @Field(3)
    public float distance;
    @Field(4)
    public float accuracy;
    @Field(5)
    public Point point;
    @Field(6)
    public boolean isLocationCandidate;
    @Field(7)
    public boolean isLocationCandidateVerified;
    @Field(8)
    public double latitude;

    public PlaceCandidate() {
    }

    @Constructor
    public PlaceCandidate(@Param(1) Identifier identifier, @Param(2) int confidence, @Param(3) float distance, @Param(4) float accuracy, @Param(5) Point point, @Param(6) boolean isLocationCandidate, @Param(7) boolean isLocationCandidateVerified, @Param(8) double latitude){
        this.identifier = identifier;
        this.confidence = confidence;
        this.distance = distance;
        this.accuracy = accuracy;
        this.point = point;
        this.isLocationCandidate = isLocationCandidate;
        this.isLocationCandidateVerified = isLocationCandidateVerified;
        this.latitude = latitude;
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
                .field("identifier", identifier)
                .field("confidence", confidence)
                .field("distance", distance)
                .field("accuracy", accuracy)
                .field("point", point)
                .field("isLocationCandidate", isLocationCandidate)
                .field("isLocationCandidateVerified", isLocationCandidateVerified)
                .field("latitude", latitude)
                .end();
    }

    public static class Identifier extends AbstractSafeParcelable {
        @Field(1)
        public Long start;
        @Field(2)
        public Long end;

        public Identifier() {
        }

        @Constructor
        public Identifier(@Param(1) Long start, @Param(1) Long end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<Identifier> CREATOR = findCreator(Identifier.class);

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("PlaceCandidate.Identifier")
                    .field("start", start)
                    .field("end", end)
                    .end();
        }
    }

    public static class Point extends AbstractSafeParcelable {
        @Field(1)
        public int pointX;
        @Field(2)
        public int pointY;

        public Point() {
        }

        @Constructor
        public Point(@Param(1) int pointX, @Param(1) int pointY) {
            this.pointX = pointX;
            this.pointY = pointY;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<Point> CREATOR = findCreator(Point.class);

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("PlaceCandidate.Point")
                    .field("pointX", pointX)
                    .field("pointY", pointY)
                    .end();
        }
    }
}
