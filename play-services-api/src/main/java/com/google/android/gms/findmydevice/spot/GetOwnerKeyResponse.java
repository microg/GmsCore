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
public class GetOwnerKeyResponse extends AbstractSafeParcelable {

    @Field(1)
    public int keyType; 
    
    @Field(2)
    public byte[] keyData;

    @Constructor
    public GetOwnerKeyResponse() {
    }

    @Constructor
    public GetOwnerKeyResponse(@Param(1) int keyType, @Param(2) byte[] keyData) {
        this.keyType = keyType;
        this.keyData = keyData;
    }

    public static final SafeParcelableCreatorAndWriter<GetOwnerKeyResponse> CREATOR = findCreator(GetOwnerKeyResponse.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
