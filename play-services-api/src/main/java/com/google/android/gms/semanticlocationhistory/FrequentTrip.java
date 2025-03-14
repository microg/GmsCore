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
public class FrequentTrip extends AbstractSafeParcelable {

    @Field(1)
    public List<PlaceCandidate.Identifier> identifierList;
    @Field(2)
    public Metadata placeMetadata;
    @Field(3)
    public List<String> keys;
    @Field(4)
    public List<FrequentTrip.ModeDistribution> modeDistributionList;
    @Field(5)
    public int type;
    @Field(6)
    public int tripType;
    @Field(7)
    public int tripMode;
    @Field(8)
    public float distance;
    @Field(9)
    public int distanceUnit;

    public FrequentTrip() {
    }

    @Constructor
    public FrequentTrip(@Param(1) List<PlaceCandidate.Identifier> identifierList, @Param(2) Metadata placeMetadata, @Param(3) List<String> keys, @Param(4) List<FrequentTrip.ModeDistribution> modeDistributionList, @Param(5) int type, @Param(6) int tripType, @Param(7) int tripMode, @Param(8) float distance, @Param(9) int distanceUnit) {
        this.identifierList = identifierList;
        this.placeMetadata = placeMetadata;
        this.keys = keys;
        this.modeDistributionList = modeDistributionList;
        this.type = type;
        this.tripType = tripType;
        this.tripMode = tripMode;
        this.distance = distance;
        this.distanceUnit = distanceUnit;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<FrequentTrip> CREATOR = findCreator(FrequentTrip.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("FrequentTrip").field("identifierList", identifierList).field("placeMetadata", placeMetadata).field("keys", keys).field("modeDistributionList", modeDistributionList).field("type", type).field("tripType", tripType).field("tripMode", tripMode).field("distance", distance).field("distanceUnit", distanceUnit).end();
    }

    public static class Metadata extends AbstractSafeParcelable {
        @Field(1)
        public long timestamp;

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<FrequentTrip.Metadata> CREATOR = findCreator(FrequentTrip.Metadata.class);

        public Metadata() {
        }

        @Constructor
        public Metadata(@Param(1) long timestamp) {
            this.timestamp = timestamp;
        }

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("FrequentTrip.Metadata").field("timestamp", timestamp).end();
        }
    }

    public static class ModeDistribution extends AbstractSafeParcelable {
        @Field(1)
        public int distance;
        @Field(2)
        public float confidence;

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<FrequentTrip.ModeDistribution> CREATOR = findCreator(FrequentTrip.ModeDistribution.class);

        public ModeDistribution() {
        }

        @Constructor
        public ModeDistribution(@Param(1) int distance, @Param(2) float confidence) {
            this.distance = distance;
            this.confidence = confidence;
        }

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("FrequentTrip.Metadata").field("distance", distance).field("confidence", confidence).end();
        }
    }

}
