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
public class GetLocationReportingStateResponse extends AbstractSafeParcelable {
    public static final SafeParcelableCreatorAndWriter<GetLocationReportingStateResponse> CREATOR = findCreator(GetLocationReportingStateResponse.class);

    @Field(1)
    public boolean enabled;

    @Constructor
    public GetLocationReportingStateResponse(@Param(1) boolean enabled) {
        this.enabled = enabled;
    }

    @Constructor
    public GetLocationReportingStateResponse() {
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("GetLocationReportingStateResponse")
                .field("enabled", enabled)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}