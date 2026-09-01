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

@SafeParcelable.Class
public class ImportGivenOwnerKeyResponse extends AbstractSafeParcelable {
    @Field(1)
    public boolean success;

    @Constructor
    public ImportGivenOwnerKeyResponse() {
    }

    @Constructor
    public ImportGivenOwnerKeyResponse(@Param(1) boolean success) {
        this.success = success;
    }

    public static final SafeParcelableCreatorAndWriter<ImportGivenOwnerKeyResponse> CREATOR = findCreator(ImportGivenOwnerKeyResponse.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
