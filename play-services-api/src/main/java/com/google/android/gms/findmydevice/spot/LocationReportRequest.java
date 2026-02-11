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

import java.util.Arrays;

@SafeParcelable.Class
public class LocationReportRequest extends AbstractSafeParcelable {
    public static final SafeParcelableCreatorAndWriter<LocationReportRequest> CREATOR = findCreator(LocationReportRequest.class);

    @Field(1)
    public ScanResult[] scanResults;

    @Field(2)
    public int type;

    @Constructor
    public LocationReportRequest() {
    }

    @Constructor
    public LocationReportRequest(@Param(1) ScanResult[] scanResults, @Param(2) int type) {
        this.scanResults = scanResults;
        this.type = type;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("LocationReportRequest")
                .field("scanResults", Arrays.toString(scanResults))
                .field("type", type)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}