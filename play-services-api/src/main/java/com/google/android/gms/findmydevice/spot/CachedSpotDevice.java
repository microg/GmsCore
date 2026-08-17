/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.findmydevice.spot;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class CachedSpotDevice extends AbstractSafeParcelable {
    @Field(1)
    public String deviceId;

    @Field(2)
    public String bluetoothAddress;

    @Constructor
    public CachedSpotDevice() {

    }

    @Constructor
    public CachedSpotDevice(@Param(1) String deviceId, @Param(2) String bluetoothAddress) {
        this.deviceId = deviceId;
        this.bluetoothAddress = bluetoothAddress;
    }

    public static final SafeParcelableCreatorAndWriter<CachedSpotDevice> CREATOR = findCreator(CachedSpotDevice.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("CachedSpotDevice")
                .field("deviceId", deviceId)
                .field("bluetoothAddress", bluetoothAddress)
                .end();
    }
}
