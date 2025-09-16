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

import org.microg.gms.utils.ToStringHelper;

import java.util.List;

@SafeParcelable.Class
public class UserLocationProfile extends AbstractSafeParcelable {

    @Field(1)
    public List<FrequentPlace> frequentPlaceList;
    @Field(2)
    public List<FrequentTrip> frequentTripList;
    @Field(3)
    public Persona persona;
    @Field(4)
    public long timestamp;

    public UserLocationProfile() {
    }

    @Constructor
    public UserLocationProfile(@Param(1) List<FrequentPlace> frequentPlaceList, @Param(2) List<FrequentTrip> frequentTripList, @Param(3) Persona persona, @Param(4) long timestamp) {
        this.frequentPlaceList = frequentPlaceList;
        this.frequentTripList = frequentTripList;
        this.persona = persona;
        this.timestamp = timestamp;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<UserLocationProfile> CREATOR = findCreator(UserLocationProfile.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("UserLocationProfile")
                .field("frequentPlaceList", frequentPlaceList)
                .field("frequentTripList", frequentTripList)
                .field("persona", persona)
                .field("timestamp", timestamp)
                .end();
    }
}
