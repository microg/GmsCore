/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.gass.internal;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.feedback.ErrorReport;

@SafeParcelable.Class
public class GassResponseParcel extends AbstractSafeParcelable {

    @Field(1)
    public int versionCode;

    @Field(2)
    public byte[] data;

    public ErrorReport report;

    public GassResponseParcel() {
    }

    public GassResponseParcel(int i, byte[] bArr) {
        this.versionCode = i;
        this.report = null;
        this.data = bArr;
    }

    public GassResponseParcel(ErrorReport report) {
        this.versionCode = 1;
        this.report = report;
        this.data = null;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GassResponseParcel> CREATOR = findCreator(GassResponseParcel.class);
}
