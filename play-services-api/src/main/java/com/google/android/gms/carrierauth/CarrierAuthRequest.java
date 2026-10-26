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
public class CarrierAuthRequest extends AbstractSafeParcelable {
    @Field(1)
    public String imsi;

    @Field(2)
    public String carrierName;

    @Field(3)
    public int subId;

    @Field(4)
    public byte[] challengeData;

    public CarrierAuthRequest() {
    }

    public CarrierAuthRequest(String imsi, String carrierName, int subId, byte[] challengeData) {
        this.imsi = imsi;
        this.carrierName = carrierName;
        this.subId = subId;
        this.challengeData = challengeData;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("CarrierAuthRequest")
                .field("imsi", imsi)
                .field("carrierName", carrierName)
                .field("subId", subId)
                .field("challengeData", challengeData)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CarrierAuthRequest> CREATOR = findCreator(CarrierAuthRequest.class);
}
