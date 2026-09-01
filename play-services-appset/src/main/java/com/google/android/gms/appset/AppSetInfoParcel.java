/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.appset;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

@SafeParcelable.Class
@Hide
public class AppSetInfoParcel extends AbstractSafeParcelable {
    @Field(1)
    public final String id;
    @Field(2)
    public final int scope;

    @Constructor
    public AppSetInfoParcel(@Param(1) String id, @Param(2) int scope) {
        this.id = id;
        this.scope = scope;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AppSetInfoParcel> CREATOR = findCreator(AppSetInfoParcel.class);

}
