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

import java.util.List;

@SafeParcelable.Class
public class FrequentPlace extends AbstractSafeParcelable {

    @Field(1)
    public PlaceCandidate.Identifier identifier;
    @Field(2)
    public PlaceCandidate.Point point;
    @Field(3)
    public int type;
    @Field(4)
    public FrequentPlaceMetadata placeMetadata;
    @Field(5)
    public List<String> keys;

    public FrequentPlace() {
    }

    @Constructor
    public FrequentPlace(@Param(1) PlaceCandidate.Identifier identifier, @Param(2) PlaceCandidate.Point point, @Param(3) int type, @Param(4) FrequentPlaceMetadata placeMetadata, @Param(5) List<String> keys){
        this.identifier = identifier;
        this.point = point;
        this.type = type;
        this.placeMetadata = placeMetadata;
        this.keys = keys;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<FrequentPlace> CREATOR = findCreator(FrequentPlace.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("FrequentPlace")
                .field("identifier", identifier)
                .field("point", point)
                .field("type", type)
                .field("placeMetadata", placeMetadata)
                .field("keys", keys)
                .end();
    }

    public static class FrequentPlaceMetadata extends AbstractSafeParcelable {
        @Field(1)
        public long timestamp;

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<FrequentPlace.FrequentPlaceMetadata> CREATOR = findCreator(FrequentPlace.FrequentPlaceMetadata.class);

        public FrequentPlaceMetadata() {}

        @Constructor
        public FrequentPlaceMetadata(@Param(1) long timestamp) {
            this.timestamp = timestamp;
        }

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("FrequentPlace.FrequentPlaceMetadata")
                    .field("timestamp", timestamp)
                    .end();
        }
    }

}
