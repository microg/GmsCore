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
public class LocationShare extends AbstractSafeParcelable {
    @Field(1)
    public final int locationShareType;
    @Field(2)
    public final String tokenId;
    @Field(3)
    public final String obfuscatedGaiaId;

    @Constructor
    public LocationShare(@Param(1) int locationShareType, @Param(2) String tokenId, @Param(3) String obfuscatedGaiaId) {
        this.locationShareType = locationShareType;
        this.tokenId = tokenId;
        this.obfuscatedGaiaId = obfuscatedGaiaId;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<LocationShare> CREATOR = findCreator(LocationShare.class);

    @Override
    public String toString() {
        return "LocationShare{" +
                "locationShareType=" + locationShareType +
                ", tokenId=" + (tokenId != null ? "\"" + tokenId + "\"" : "null") +
                ", obfuscatedGaiaId=" + (obfuscatedGaiaId != null ? "\"" + obfuscatedGaiaId + "\"" : "null") +
                '}';
    }
}
