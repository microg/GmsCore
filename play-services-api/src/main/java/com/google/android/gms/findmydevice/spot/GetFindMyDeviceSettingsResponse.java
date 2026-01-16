/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.findmydevice.spot;

import android.accounts.Account;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class GetFindMyDeviceSettingsResponse extends AbstractSafeParcelable {

    @Field(5)
    public long lastEnableAllLocationTime;

    @Field(6)
    public boolean isFmdEnable;

    @Field(7)
    public boolean isNetworkLocationEnabled;

    @Field(8)
    public boolean isLklFullyEnabled;

    @Field(9)
    public boolean hasOwnerKey;

    @Field(10)
    public FindMyDeviceNetworkSettings networkSettings;

    @Field(11)
    public long lastEnableHighTrafficTime;

    @Field(12)
    public Account account;

    @Field(13)
    public boolean isLocationReportingEnabled;

    @Field(14)
    public int deviceStatus;

    @Field(15)
    public Long unknownTime;

    @Field(16)
    public FindMyDeviceNetworkSettings findMyDeviceNetworkSettings;

    @Field(17)
    public boolean requiresUserConsent;

    @Constructor
    public GetFindMyDeviceSettingsResponse() {

    }

    @Constructor
    public GetFindMyDeviceSettingsResponse(@Param(5) long lastEnableAllLocationTime, @Param(6) boolean IsFmdEnable, @Param(7) boolean isNetworkLocationEnabled,
                                           @Param(8) boolean isLklFullyEnabled, @Param(9) boolean hasOwnerKey, @Param(10) FindMyDeviceNetworkSettings networkSettings,
                                           @Param(11) long lastEnableHigh, @Param(12) Account account, @Param(13) boolean isLocationReportingEnabled,
                                           @Param(14) int deviceStatus, @Param(15) Long unknownTime, @Param(16) FindMyDeviceNetworkSettings findMyDeviceNetworkSettings,
                                           @Param(17) boolean requiresUserConsent) {
        this.lastEnableAllLocationTime = lastEnableAllLocationTime;
        this.isFmdEnable = IsFmdEnable;
        this.isNetworkLocationEnabled = isNetworkLocationEnabled;
        this.isLklFullyEnabled = isLklFullyEnabled;
        this.hasOwnerKey = hasOwnerKey;
        this.networkSettings = networkSettings;
        this.lastEnableHighTrafficTime = lastEnableHigh;
        this.account = account;
        this.isLocationReportingEnabled = isLocationReportingEnabled;
        this.deviceStatus = deviceStatus;
        this.unknownTime = unknownTime;
        this.findMyDeviceNetworkSettings = findMyDeviceNetworkSettings;
        this.requiresUserConsent = requiresUserConsent;
    }

    public static final SafeParcelableCreatorAndWriter<GetFindMyDeviceSettingsResponse> CREATOR = findCreator(GetFindMyDeviceSettingsResponse.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @Override
    public String toString() {
        return "GetFindMyDeviceSettingsResponse{" +
                "isEnabled=" + isFmdEnable +
                ", isNetworkLocationEnabled=" + isNetworkLocationEnabled +
                ", networkSettings=" + (networkSettings != null ? "mode=" + networkSettings.finderNetworkState : "null") +
                ", lastUpdateTime=" + lastEnableAllLocationTime +
                ", lastEnableHighTrafficTime=" + lastEnableHighTrafficTime +
                ", isLklFullyEnabled=" + isLklFullyEnabled +
                ", hasOwnerKey=" + hasOwnerKey +
                ", account=" + (account != null ? account.name : "null") +
                ", isLocationReportingEnabled=" + isLocationReportingEnabled +
                ", deviceStatus=" + deviceStatus +
                ", unknownTime=" + unknownTime +
                ", findMyDeviceNetworkSettings=" + (findMyDeviceNetworkSettings != null ? "mode=" + findMyDeviceNetworkSettings.finderNetworkState : "null") +
                ", requiresUserConsent=" + requiresUserConsent +
                '}';
    }

}