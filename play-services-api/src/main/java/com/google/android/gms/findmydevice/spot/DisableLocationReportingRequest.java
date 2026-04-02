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
public class DisableLocationReportingRequest extends AbstractSafeParcelable {
    public static final SafeParcelableCreatorAndWriter<DisableLocationReportingRequest> CREATOR = findCreator(DisableLocationReportingRequest.class);

    @Field(1)
    public String reason;

    @Constructor
    public DisableLocationReportingRequest(@Param(1) String reason) {
        this.reason = reason;
    }

    @Constructor
    public DisableLocationReportingRequest() {

    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("DisableLocationReportingRequest")
                .field("reason", reason)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}