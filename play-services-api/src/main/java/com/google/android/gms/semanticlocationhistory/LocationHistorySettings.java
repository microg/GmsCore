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
import com.google.android.gms.location.reporting.ReportingState;

@SafeParcelable.Class
public class LocationHistorySettings extends AbstractSafeParcelable {
    @Field(1)
    public final boolean historyEnabled;
    @Field(2)
    public final int deviceTag;
    @Field(3)
    public final ReportingState reportingState;

    @Constructor
    public LocationHistorySettings(@Param(1) boolean historyEnabled, @Param(2) int deviceTag, @Param(3) ReportingState reportingState) {
        this.historyEnabled = historyEnabled;
        this.deviceTag = deviceTag;
        this.reportingState = reportingState;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<LocationHistorySettings> CREATOR = findCreator(LocationHistorySettings.class);
}
