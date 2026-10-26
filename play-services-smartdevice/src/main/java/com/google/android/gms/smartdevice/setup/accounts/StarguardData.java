/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.smartdevice.setup.accounts;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;

@SafeParcelable.Class
@Hide
public class StarguardData extends AbstractSafeParcelable {
    @Field(value = 2)
    public String droidguardResult;

    public StarguardData() {
    }

    @Constructor
    public StarguardData(@Param(2) String droidguardResult) {
        this.droidguardResult = droidguardResult;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<StarguardData> CREATOR =
            findCreator(StarguardData.class);
}
