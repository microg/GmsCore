/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.PublicApi;

@PublicApi
@SafeParcelable.Class
public class PrfExtension extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getEntries")
    private final byte[][] entries;

    @Constructor
    public PrfExtension(@Param(1) byte[][] entries) {
        this.entries = entries;
    }

    @NonNull
    public byte[][] getEntries() {
        return entries;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PrfExtension> CREATOR =
            AbstractSafeParcelable.findCreator(PrfExtension.class);
}
