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

@SafeParcelable.Class
public class GetCachedDevicesResponse extends AbstractSafeParcelable {
    @Field(1)
    public CachedSpotDevice[] devices;

    @Constructor
    public GetCachedDevicesResponse() {

    }

    @Constructor
    public GetCachedDevicesResponse(@Param(1) CachedSpotDevice[] cachedSpotDeviceArr) {
        this.devices = cachedSpotDeviceArr;
    }

    public static final SafeParcelableCreatorAndWriter<GetCachedDevicesResponse> CREATOR = findCreator(GetCachedDevicesResponse.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
