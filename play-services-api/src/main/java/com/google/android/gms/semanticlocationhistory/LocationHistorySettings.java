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
import com.google.android.gms.location.reporting.ReportingState;

@SafeParcelable.Class
public class LocationHistorySettings extends AbstractSafeParcelable {
    @Field(1)
    public final boolean enabled;
    @Field(2)
    public final int deviceId;
    @Field(3)
    public final ReportingState reportingState;

    @Constructor
    public LocationHistorySettings(@Param(1) boolean enabled, @Param(2) int deviceId, @Param(3) ReportingState reportingState) {
        this.enabled = enabled;
        this.deviceId = deviceId;
        this.reportingState = reportingState;
    }

    public static final SafeParcelableCreatorAndWriter<LocationHistorySettings> CREATOR = findCreator(LocationHistorySettings.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
