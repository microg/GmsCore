/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.semanticlocationhistory;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public class ExperimentVisitsResponse extends AbstractSafeParcelable {
    @Field(1)
    public final List<LocationHistorySegment> locationHistorySegmentList;
    @Field(2)
    public final int unknownFiled2;
    @Field(3)
    public final DeviceMetadata deviceMetadata;

    @Constructor
    public ExperimentVisitsResponse(@Param(1) List<LocationHistorySegment> locationHistorySegmentList, @Param(2) int unknownFiled2, @Param(3) DeviceMetadata deviceMetadata) {
        this.locationHistorySegmentList = locationHistorySegmentList;
        this.unknownFiled2 = unknownFiled2;
        this.deviceMetadata = deviceMetadata;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ExperimentVisitsResponse> CREATOR = findCreator(ExperimentVisitsResponse.class);
}
