/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.carrierauth;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class CarrierAuthResult extends AbstractSafeParcelable {
    @Field(1)
    public int statusCode;

    @Field(2)
    public String authToken;

    @Field(3)
    public byte[] carrierData;

    public CarrierAuthResult() {
    }

    public CarrierAuthResult(int statusCode, String authToken, byte[] carrierData) {
        this.statusCode = statusCode;
        this.authToken = authToken;
        this.carrierData = carrierData;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("CarrierAuthResult")
                .field("statusCode", statusCode)
                .field("authToken", authToken)
                .field("carrierData", carrierData)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CarrierAuthResult> CREATOR = findCreator(CarrierAuthResult.class);
}
