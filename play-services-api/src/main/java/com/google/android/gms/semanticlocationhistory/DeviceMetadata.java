/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.semanticlocationhistory;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public class DeviceMetadata extends AbstractSafeParcelable {
    @Field(1)
    public final List<String> deviceTags;
    @Field(2)
    public final boolean hasFineLocationPermission;
    @Field(3)
    public final boolean isTestAccount;
    @Field(4)
    public final List<DeletionRange> deletionRangeList;
    @Field(5)
    public final int accountDeviceTag;

    @Constructor
    public DeviceMetadata(@Param(1) List<String> deviceTags, @Param(2) boolean hasFineLocationPermission,
                          @Param(3) boolean isTestAccount, @Param(4) List<DeletionRange> deletionRangeList, @Param(5) int accountDeviceTag) {
        this.deviceTags = deviceTags;
        this.hasFineLocationPermission = hasFineLocationPermission;
        this.isTestAccount = isTestAccount;
        this.deletionRangeList = deletionRangeList;
        this.accountDeviceTag = accountDeviceTag;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DeviceMetadata> CREATOR = findCreator(DeviceMetadata.class);
}
