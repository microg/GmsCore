/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.findmydevice.spot;

import android.accounts.Account;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class OwnersLocationReportRequest extends AbstractSafeParcelable {
    public static final SafeParcelableCreatorAndWriter<OwnersLocationReportRequest> CREATOR = findCreator(OwnersLocationReportRequest.class);

    @Field(1)
    public Account account;
    @Field(2)
    public ScanResult scanResult;

    @Constructor
    public OwnersLocationReportRequest() {
    }

    @Constructor
    public OwnersLocationReportRequest(@Param(1) Account account, @Param(2) ScanResult scanResult) {
        this.account = account;
        this.scanResult = scanResult;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}