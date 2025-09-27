/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.locationsharingreporter;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;


@SafeParcelable.Class
public class LocationCollectionReason extends AbstractSafeParcelable {
    @Field(1)
    public final int locationCollectionReason;

    @Constructor
    public LocationCollectionReason(@Param(1) int locationCollectionReason) {
        this.locationCollectionReason = locationCollectionReason;
    }

    @NonNull
    @Override
    public String toString() {
        return "LocationCollectionReason{" +
                "locationCollectionReason=" + locationCollectionReason +
                '}';
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<LocationCollectionReason> CREATOR = findCreator(LocationCollectionReason.class);
}
