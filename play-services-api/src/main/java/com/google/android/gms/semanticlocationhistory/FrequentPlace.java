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
public class FrequentPlace extends AbstractSafeParcelable {

    @Field(1)
    public PlaceCandidate.Identifier identifier;
    @Field(2)
    public PlaceCandidate.Point placeLocation;
    @Field(3)
    public int type;
    @Field(4)
    @Deprecated
    FrequentPlaceMetadata metadata;
    @Field(5)
    @Deprecated
    List<String> keys;
    @Field(6)
    public final List<Float> weeklyOccupancy;
    @Field(7)
    public final int numWeeksSinceLastVisit;

    @Constructor
    public FrequentPlace(@Param(1) PlaceCandidate.Identifier identifier, @Param(2) PlaceCandidate.Point placeLocation, @Param(3) int type, @Param(6) List<Float> weeklyOccupancy, @Param(7) int numWeeksSinceLastVisit) {
        this.identifier = identifier;
        this.placeLocation = placeLocation;
        this.type = type;
        this.weeklyOccupancy = weeklyOccupancy;
        this.numWeeksSinceLastVisit = numWeeksSinceLastVisit;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("FrequentPlace")
                .field("identifier", identifier)
                .field("placeLocation", placeLocation)
                .field("type", type)
                .field("weeklyOccupancy", weeklyOccupancy)
                .field("numWeeksSinceLastVisit", numWeeksSinceLastVisit)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<FrequentPlace> CREATOR = findCreator(FrequentPlace.class);

    @Class
    public static class FrequentPlaceMetadata extends AbstractSafeParcelable {
        @Field(1)
        public long timestamp;

        @Constructor
        public FrequentPlaceMetadata(@Param(1) long timestamp) {
            this.timestamp = timestamp;
        }

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("FrequentPlaceMetadata")
                    .field("creationTime", timestamp)
                    .end();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<FrequentPlace.FrequentPlaceMetadata> CREATOR = findCreator(FrequentPlace.FrequentPlaceMetadata.class);
    }

}
