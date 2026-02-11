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
public class FindMyDeviceNetworkSettings extends AbstractSafeParcelable {
    @Field(1)
    public int finderNetworkState;

    @Constructor
    public FindMyDeviceNetworkSettings() {

    }

    @Constructor
    public FindMyDeviceNetworkSettings(@Param(1) int finderNetworkState) {
        this.finderNetworkState = finderNetworkState;
    }

    public static final SafeParcelableCreatorAndWriter<FindMyDeviceNetworkSettings> CREATOR = findCreator(FindMyDeviceNetworkSettings.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
