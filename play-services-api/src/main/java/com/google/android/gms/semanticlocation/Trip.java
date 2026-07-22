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

import java.util.List;

@SafeParcelable.Class
public class Trip extends AbstractSafeParcelable {
    @Field(1)
    public final long distance;
    @Field(2)
    public final List<Destination> destinations;
    @Field(3)
    public final NameComponents nameComponents;
    @Field(4)
    public final Origin origin;
    @Field(5)
    boolean b5;

    @Constructor
    public Trip(@Param(1) long distance, @Param(2) List<Destination> destinations, @Param(3) NameComponents nameComponents, @Param(4) Origin origin) {
        this.distance = distance;
        this.destinations = destinations;
        this.nameComponents = nameComponents;
        this.origin = origin;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("Trip")
                .field("origin", origin)
                .field("distance", distance)
                .field("destinations", destinations)
                .field("nameComponents", nameComponents)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Trip> CREATOR = findCreator(Trip.class);

    @Class
    public static class Destination extends AbstractSafeParcelable {
        @Field(1)
        public final PlaceCandidate.Identifier identifier;

        @Constructor
        public Destination(@Param(1) PlaceCandidate.Identifier identifier) {
            this.identifier = identifier;
        }

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("Destination").value(identifier).end();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<Destination> CREATOR = findCreator(Destination.class);
    }

    @Class
    public static class NameComponents extends AbstractSafeParcelable {
        @Field(1)
        public final List<Destination> components;

        @Constructor
        public NameComponents(@Param(1) List<Destination> components) {
            this.components = components;
        }

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("NameComponents").value(components).end();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<NameComponents> CREATOR = findCreator(NameComponents.class);
    }

    @Class
    public static class Origin extends AbstractSafeParcelable {
        @Field(1)
        public final PlaceCandidate.Identifier identifier;
        @Field(2)
        public final PlaceCandidate.Point point;

        @Constructor
        public Origin(@Param(1) PlaceCandidate.Identifier identifier, @Param(2) PlaceCandidate.Point point) {
            this.identifier = identifier;
            this.point = point;
        }

        @NonNull
        @Override
        public String toString() {
            return ToStringHelper.name("Origin")
                    .field("identifier", identifier)
                    .field("point", point)
                    .end();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<Origin> CREATOR = findCreator(Origin.class);
    }
}
