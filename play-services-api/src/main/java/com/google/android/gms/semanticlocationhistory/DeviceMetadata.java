/*
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
    public List<String> subIdentifiers;
    @Field(2)
    public boolean hasFineLocationPermission;
    @Field(3)
    public boolean b3;
    @Field(4)
    public List<DeletionRange> deletionRanges;
    @Field(5)
    public int deviceTag;

    @Constructor
    public DeviceMetadata(@Param(1) List<String> subIdentifiers, @Param(2) boolean hasFineLocationPermission, @Param(3) boolean b3, @Param(4) List<DeletionRange> deletionRanges, @Param(5) int deviceTag) {
        this.subIdentifiers = subIdentifiers;
        this.hasFineLocationPermission = hasFineLocationPermission;
        this.b3 = b3;
        this.deletionRanges = deletionRanges;
        this.deviceTag = deviceTag;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DeviceMetadata> CREATOR = findCreator(DeviceMetadata.class);
}
