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

import org.microg.gms.common.Constants;
import org.microg.gms.common.Hide;

@SafeParcelable.Class
@Hide
public class DeviceRiskSignals extends AbstractSafeParcelable {
    @Field(value = 2)
    public int deviceType;
    @Field(value = 3)
    public long androidDeviceId;
    @Field(value = 4)
    public String deviceModel;
    @Field(value = 5)
    public String deviceManufacturer;
    @Field(value = 6)
    public long osMajorVersionCode;
    @Field(value = 7)
    public long softwareMajorVersionCode;
    @Field(value = 8)
    public ScreenlockState screenlockState;
    @Field(value = 9)
    public StarguardData starguardData;

    public DeviceRiskSignals() {
    }

    public DeviceRiskSignals(long androidDeviceId, String deviceModel,
                             String deviceManufacturer, long osMajorVersionCode,
                             ScreenlockState screenlockState, StarguardData starguardData) {
        this.deviceType = 1;
        this.androidDeviceId = androidDeviceId;
        this.deviceModel = deviceModel;
        this.deviceManufacturer = deviceManufacturer;
        this.osMajorVersionCode = osMajorVersionCode;
        this.softwareMajorVersionCode = Constants.GMS_VERSION_CODE;
        this.screenlockState = screenlockState;
        this.starguardData = starguardData;
    }

    @Constructor
    public DeviceRiskSignals(@Param(2) int deviceType,
                             @Param(3) long androidDeviceId,
                             @Param(4) String deviceModel,
                             @Param(5) String deviceManufacturer,
                             @Param(6) long osMajorVersionCode,
                             @Param(7) long softwareMajorVersionCode,
                             @Param(8) ScreenlockState screenlockState,
                             @Param(9) StarguardData starguardData) {
        this.deviceType = deviceType;
        this.androidDeviceId = androidDeviceId;
        this.deviceModel = deviceModel;
        this.deviceManufacturer = deviceManufacturer;
        this.osMajorVersionCode = osMajorVersionCode;
        this.softwareMajorVersionCode = softwareMajorVersionCode;
        this.screenlockState = screenlockState;
        this.starguardData = starguardData;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DeviceRiskSignals> CREATOR = findCreator(DeviceRiskSignals.class);
}
