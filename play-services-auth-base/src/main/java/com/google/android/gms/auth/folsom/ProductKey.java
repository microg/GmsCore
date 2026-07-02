/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.folsom;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

import java.util.Objects;

@SafeParcelable.Class
public class ProductKey extends AbstractSafeParcelable {

    @Field(1)
    public int key;
    @Field(2)
    public byte[] keyMaterial;

    public ProductKey() {
    }

    @Constructor
    public ProductKey(@Param(1) int key, @Param(2) byte[] keyMaterial) {
        Objects.requireNonNull(keyMaterial, "keyMaterial cannot be null");
        this.key = key;
        this.keyMaterial = keyMaterial;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ProductKey> CREATOR = findCreator(ProductKey.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("ProductKey").field("key", key).end();
    }
}
