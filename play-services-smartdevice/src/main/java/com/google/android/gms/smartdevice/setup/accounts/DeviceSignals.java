/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.smartdevice.setup.accounts;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;

@SafeParcelable.Class
@Hide
public class DeviceSignals extends AbstractSafeParcelable {
    @Field(value = 2)
    public String deviceId;
    @Field(value = 3)
    public String deviceName;
    @Field(value = 4)
    public String deviceModel;
    @Field(value = 5)
    public String sdkVersion;
    @Field(value = 6)
    public String googlePlayServicesVersion;
    @Field(value = 7)
    public String droidGuardResults;
    @Field(value = 8)
    public UserPresence userPresence;

    public DeviceSignals() {
    }

    @Constructor
    public DeviceSignals(@Param(2) String deviceId, @Param(3) String deviceName,
                         @Param(4) String deviceModel, @Param(5) String sdkVersion,
                         @Param(6) String googlePlayServicesVersion,
                         @Param(7) String droidGuardResults, @Param(8) UserPresence userPresence) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceModel = deviceModel;
        this.sdkVersion = sdkVersion;
        this.googlePlayServicesVersion = googlePlayServicesVersion;
        this.droidGuardResults = droidGuardResults;
        this.userPresence = userPresence;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DeviceSignals> CREATOR = findCreator(DeviceSignals.class);
}
