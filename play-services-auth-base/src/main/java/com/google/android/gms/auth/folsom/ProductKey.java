/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.auth.folsom;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class ProductKey extends AbstractSafeParcelable {
    @Field(1)
    public int key;
    @Field(2)
    public byte[] keyMaterial;

    @Constructor
    public ProductKey(@Param(1) int key, @Param(2) byte[] keyMaterial) {
        this.key = key;
        this.keyMaterial = keyMaterial;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ProductKey> CREATOR = findCreator(ProductKey.class);
}
