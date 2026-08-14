/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.locationsharingreporter;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.Arrays;

@SafeParcelable.Class
public class PeriodicLocationReportingIssues extends AbstractSafeParcelable {
    @Field(1)
    final int[] generalIssues;
    @Field(2)
    final Bundle issuesByAccount;
    @Field(4)
    final boolean isCentralizedSharingFlagEnabled;

    @Constructor
    public PeriodicLocationReportingIssues(@Param(1) int[] generalIssues, @Param(2) Bundle issuesByAccount, @Param(4) boolean isCentralizedSharingFlagEnabled) {
        this.generalIssues = generalIssues;
        this.issuesByAccount = issuesByAccount;
        this.isCentralizedSharingFlagEnabled = isCentralizedSharingFlagEnabled;
    }

    @NonNull
    @Override
    public final String toString() {
        return "PeriodicLocationReportingIssues{generalIssues=" + Arrays.toString(this.generalIssues)
                + ", issuesByAccount=" + this.issuesByAccount + ", isCentralizedSharingFlagEnabled="
                + this.isCentralizedSharingFlagEnabled + "}";
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PeriodicLocationReportingIssues> CREATOR = findCreator(PeriodicLocationReportingIssues.class);
}
