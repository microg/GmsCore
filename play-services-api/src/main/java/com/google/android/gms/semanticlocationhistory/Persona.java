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
public class Persona extends AbstractSafeParcelable {

    @Field(1)
    public PersonaMetadata personaMetadata;
    @Field(2)
    public List<LocationAffinity> locationAffinityList;
    @Field(3)
    public List<TravelModeAffinity> travelModeAffinityList;

    public Persona() {
    }

    @Constructor
    public Persona(@Param(1) PersonaMetadata personaMetadata, @Param(2) List<LocationAffinity> locationAffinityList, @Param(3) List<TravelModeAffinity> travelModeAffinityList){
        this.personaMetadata = personaMetadata;
        this.locationAffinityList = locationAffinityList;
        this.travelModeAffinityList = travelModeAffinityList;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Persona> CREATOR = findCreator(Persona.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("Persona")
                .field("personaMetadata", personaMetadata)
                .field("locationAffinityList", locationAffinityList)
                .field("travelModeAffinityList", travelModeAffinityList)
                .end();
    }

    public static class LocationAffinity extends AbstractSafeParcelable {
        @Field(1)
        public PlaceCandidate.Identifier identifier;
        @Field(2)
        public float confidence;
        @Field(3)
        public long timestamp;
        @Field(4)
        public float distance;
        @Field(5)
        public float accuracy;
        @Field(6)
        public float distanceToLocation;

        public LocationAffinity() {}

        @Constructor
        public LocationAffinity(@Param(1) PlaceCandidate.Identifier identifier, @Param(2) float confidence, @Param(3) long timestamp, @Param(4) float distance, @Param(5) float accuracy, @Param(6) float distanceToLocation) {
            this.identifier = identifier;
            this.confidence = confidence;
            this.timestamp = timestamp;
            this.distance = distance;
            this.accuracy = accuracy;
            this.distanceToLocation = distanceToLocation;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<LocationAffinity> CREATOR = findCreator(LocationAffinity.class);

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("Persona.LocationAffinity")
                    .field("identifier", identifier)
                    .field("confidence", confidence)
                    .field("timestamp", timestamp)
                    .field("distance", distance)
                    .field("accuracy", accuracy)
                    .field("distanceToLocation", distanceToLocation)
                    .end();
        }
    }

    public static class PersonaMetadata extends AbstractSafeParcelable {
        @Field(1)
        public long timestamp;

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<PersonaMetadata> CREATOR = findCreator(PersonaMetadata.class);

        public PersonaMetadata() {}

        @Constructor
        public PersonaMetadata(@Param(1) long timestamp) {
            this.timestamp = timestamp;
        }

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("Persona.PersonaMetadata")
                    .field("timestamp", timestamp)
                    .end();
        }
    }

    public static class TripSummary extends AbstractSafeParcelable {
        @Field(1)
        public float distance;
        @Field(2)
        public float duration;
        @Field(3)
        public long timestamp;
        @Field(4)
        public long endTime;
        @Field(5)
        public int tripType;
        @Field(6)
        public int tripMode;

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<TripSummary> CREATOR = findCreator(TripSummary.class);

        public TripSummary() {}

        @Constructor
        public TripSummary(@Param(1) float distance, @Param(2) float duration, @Param(3) long timestamp, @Param(4) long endTime, @Param(5) int tripType, @Param(6) int tripMode) {
            this.distance = distance;
            this.duration = duration;
            this.timestamp = timestamp;
            this.endTime = endTime;
            this.tripType = tripType;
            this.tripMode = tripMode;
        }

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("Persona.TripSummary")
                    .field("distance", distance)
                    .field("duration", duration)
                    .field("timestamp", timestamp)
                    .field("endTime", endTime)
                    .field("tripType", tripType)
                    .field("tripMode", tripMode)
                    .end();
        }
    }

    public static class TravelModeAffinity extends AbstractSafeParcelable {
        @Field(1)
        public int distance;
        @Field(2)
        public float confidence;
        @Field(3)
        public int travelMode;
        @Field(4)
        public int travelModeAffinity;
        @Field(5)
        public int tripType;
        @Field(6)
        public int tripMode;
        @Field(7)
        public TripSummary tripSummary;
        @Field(8)
        public TripSummary tripSummary2;
        @Field(9)
        public TripSummary tripSummary3;

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<TravelModeAffinity> CREATOR = findCreator(TravelModeAffinity.class);

        public TravelModeAffinity() {}

        @Constructor
        public TravelModeAffinity(@Param(1) int distance, @Param(2) float confidence, @Param(3) int travelMode, @Param(4) int travelModeAffinity, @Param(5) int tripType, @Param(6) int tripMode, @Param(7) TripSummary tripSummary, @Param(8) TripSummary tripSummary2, @Param(9) TripSummary tripSummary3) {
            this.distance = distance;
            this.confidence = confidence;
            this.travelMode = travelMode;
            this.travelModeAffinity = travelModeAffinity;
            this.tripType = tripType;
            this.tripMode = tripMode;
            this.tripSummary = tripSummary;
            this.tripSummary2 = tripSummary2;
            this.tripSummary3 = tripSummary3;
        }

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("Persona.TravelModeAffinity")
                    .field("distance", distance)
                    .field("confidence", confidence)
                    .field("travelMode", travelMode)
                    .field("travelModeAffinity", travelModeAffinity)
                    .field("tripType", tripType)
                    .field("tripMode", tripMode)
                    .field("tripSummary", tripSummary)
                    .field("tripSummary2", tripSummary2)
                    .field("tripSummary3", tripSummary3)
                    .end();
        }
    }
}
