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
public class ChangeFindMyDeviceSettingsRequest extends AbstractSafeParcelable {
    public static final SafeParcelableCreatorAndWriter<ChangeFindMyDeviceSettingsRequest> CREATOR = findCreator(ChangeFindMyDeviceSettingsRequest.class);
    @Field(1)
    public Boolean isEnabled;
    @Field(4)
    public Boolean isLocationReportingEnabled;
    @Field(5)
    public FindMyDeviceNetworkSettings networkSettings;
    @Field(3)
    public boolean isAsync;

    @Constructor
    public ChangeFindMyDeviceSettingsRequest() {

    }

    @Constructor
    public ChangeFindMyDeviceSettingsRequest(@Param(1) Boolean isEnabled, @Param(4) Boolean isLocationReportingEnabled, @Param(5) FindMyDeviceNetworkSettings networkSettings, @Param(3) boolean isAsync) {
        this.isEnabled = isEnabled;
        this.isLocationReportingEnabled = isLocationReportingEnabled;
        this.networkSettings = networkSettings;
        this.isAsync = isAsync;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("ChangeFindMyDeviceSettingsRequest").field("isEnabled", isEnabled).field("isLocationReportingEnabled", isLocationReportingEnabled).field("networkSettings", networkSettings).field("isAsync", isAsync).end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}