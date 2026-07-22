/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.api;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class ComplianceOptions extends AbstractSafeParcelable {
    @Field(1)
    public int callerProductId;
    @Field(2)
    public int dataOwnerProductId;
    @Field(3)
    public int processingReason;
    @Field(4)
    public boolean isUserData;

    public ComplianceOptions() {
    }

    @Constructor
    public ComplianceOptions(@Param(1) int callerProductId, @Param(2) int dataOwnerProductId, @Param(3) int processingReason, @Param(4) boolean isUserData) {
        this.callerProductId = callerProductId;
        this.dataOwnerProductId = dataOwnerProductId;
        this.processingReason = processingReason;
        this.isUserData = isUserData;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ComplianceOptions> CREATOR = findCreator(ComplianceOptions.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("ComplianceOptions").field("callerProductId", callerProductId).field("dataOwnerProductId", dataOwnerProductId).field("processingReason", processingReason).field("isUserData", isUserData).end();
    }
}
