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
public class Persona extends AbstractSafeParcelable {
    @Field(1)
    public final PersonaMetadata metadata;
    @Field(2)
    public final List<LocationAffinity> locationAffinities;
    @Field(3)
    public final List<TravelModeAffinity> travelModeAffinities;

    @Constructor
    public Persona(@Param(1) PersonaMetadata metadata, @Param(2) List<LocationAffinity> locationAffinities, @Param(3) List<TravelModeAffinity> travelModeAffinities){
        this.metadata = metadata;
        this.locationAffinities = locationAffinities;
        this.travelModeAffinities = travelModeAffinities;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("Persona")
                .field("metadata", metadata)
                .field("locationAffinities", locationAffinities)
                .field("travelModeAffinities", travelModeAffinities)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Persona> CREATOR = findCreator(Persona.class);

    @Class
    public static class LocationAffinity extends AbstractSafeParcelable {
        @Field(1)
        public final PlaceCandidate.Identifier identifier;
        @Field(2)
        public final float averageNumVisitsPerMonth;
        @Field(3)
        public final long latestVisitTime;
        @Field(4)
        public final float distanceToInferredHomeMeters;
        @Field(5)
        public final float distanceToInferredWorkMeters;
        @Field(6)
        public final float fractionOfPoiVisits;

        @Constructor
        public LocationAffinity(@Param(1) PlaceCandidate.Identifier identifier, @Param(2) float averageNumVisitsPerMonth, @Param(3) long latestVisitTime, @Param(4) float distanceToInferredHomeMeters, @Param(5) float distanceToInferredWorkMeters, @Param(6) float fractionOfPoiVisits) {
            this.identifier = identifier;
            this.averageNumVisitsPerMonth = averageNumVisitsPerMonth;
            this.latestVisitTime = latestVisitTime;
            this.distanceToInferredHomeMeters = distanceToInferredHomeMeters;
            this.distanceToInferredWorkMeters = distanceToInferredWorkMeters;
            this.fractionOfPoiVisits = fractionOfPoiVisits;
        }

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("LocationAffinity")
                    .field("identifier", identifier)
                    .field("averageNumVisitsPerMonth", averageNumVisitsPerMonth)
                    .field("latestVisitTime", latestVisitTime)
                    .field("distanceToInferredHomeMeters", distanceToInferredHomeMeters)
                    .field("distanceToInferredWorkMeters", distanceToInferredWorkMeters)
                    .field("fractionOfPoiVisits", fractionOfPoiVisits)
                    .end();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<LocationAffinity> CREATOR = findCreator(LocationAffinity.class);
    }

    @Class
    public static class PersonaMetadata extends AbstractSafeParcelable {
        @Field(1)
        public final long creationTime;

        @Constructor
        public PersonaMetadata(@Param(1) long creationTime) {
            this.creationTime = creationTime;
        }

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("PersonaMetadata")
                    .field("creationTime", creationTime)
                    .end();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<PersonaMetadata> CREATOR = findCreator(PersonaMetadata.class);
    }

    @Class
    public static class TravelModeAffinity extends AbstractSafeParcelable {
        @Field(1)
        public final int travelMode;
        @Field(2)
        public final float affinityScore;
        @Field(3)
        public final int totalNumTrips;
        @Field(4)
        public final int numTripsPastWeek;
        @Field(5)
        public final int numTripsPast4Weeks;
        @Field(6)
        public final int numTripsPast12Weeks;
        @Field(7)
        public final TripSummary tripSummaryPastWeek;
        @Field(8)
        public final TripSummary tripSummaryPast4Weeks;
        @Field(9)
        public final TripSummary tripSummaryPast12Weeks;

        @Constructor
        public TravelModeAffinity(@Param(1) int travelMode, @Param(2) float affinityScore, @Param(3) int totalNumTrips, @Param(4) int numTripsPastWeek, @Param(5) int numTripsPast4Weeks, @Param(6) int numTripsPast12Weeks, @Param(7) TripSummary tripSummaryPastWeek, @Param(8) TripSummary tripSummaryPast4Weeks, @Param(9) TripSummary tripSummaryPast12Weeks) {
            this.travelMode = travelMode;
            this.affinityScore = affinityScore;
            this.totalNumTrips = totalNumTrips;
            this.numTripsPastWeek = numTripsPastWeek;
            this.numTripsPast4Weeks = numTripsPast4Weeks;
            this.numTripsPast12Weeks = numTripsPast12Weeks;
            this.tripSummaryPastWeek = tripSummaryPastWeek;
            this.tripSummaryPast4Weeks = tripSummaryPast4Weeks;
            this.tripSummaryPast12Weeks = tripSummaryPast12Weeks;
        }

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("TravelModeAffinity")
                    .field("travelMode", travelMode)
                    .field("affinityScore", affinityScore)
                    .field("totalNumTrips", totalNumTrips)
                    .field("numTripsPastWeek", numTripsPastWeek)
                    .field("numTripsPast4Weeks", numTripsPast4Weeks)
                    .field("numTripsPast12Weeks", numTripsPast12Weeks)
                    .field("tripSummaryPastWeek", tripSummaryPastWeek)
                    .field("tripSummaryPast4Weeks", tripSummaryPast4Weeks)
                    .field("tripSummaryPast12Weeks", tripSummaryPast12Weeks)
                    .end();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<TravelModeAffinity> CREATOR = findCreator(TravelModeAffinity.class);
    }

    @Class
    public static class TripSummary extends AbstractSafeParcelable {
        @Field(1)
        public final float avgSpeedMetersPerSecond;
        @Field(2)
        public final float medianSpeedMetersPerSecond;
        @Field(3)
        public final long avgDurationSeconds;
        @Field(4)
        public final long medianDurationSeconds;
        @Field(5)
        public final int avgDistanceMeters;
        @Field(6)
        public final int medianDistanceMeters;

        @Constructor
        public TripSummary(@Param(1) float avgSpeedMetersPerSecond, @Param(2) float medianSpeedMetersPerSecond, @Param(3) long avgDurationSeconds, @Param(4) long medianDurationSeconds, @Param(5) int avgDistanceMeters, @Param(6) int medianDistanceMeters) {
            this.avgSpeedMetersPerSecond = avgSpeedMetersPerSecond;
            this.medianSpeedMetersPerSecond = medianSpeedMetersPerSecond;
            this.avgDurationSeconds = avgDurationSeconds;
            this.medianDurationSeconds = medianDurationSeconds;
            this.avgDistanceMeters = avgDistanceMeters;
            this.medianDistanceMeters = medianDistanceMeters;
        }

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("TripSummary")
                    .field("avgSpeedMetersPerSecond", avgSpeedMetersPerSecond)
                    .field("medianSpeedMetersPerSecond", medianSpeedMetersPerSecond)
                    .field("avgDurationSeconds", avgDurationSeconds)
                    .field("medianDurationSeconds", medianDurationSeconds)
                    .field("avgDistanceMeters", avgDistanceMeters)
                    .field("medianDistanceMeters", medianDistanceMeters)
                    .end();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<TripSummary> CREATOR = findCreator(TripSummary.class);
    }
}
