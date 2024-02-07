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

@SafeParcelable.Class
public class AppSetInfoParcel extends AbstractSafeParcelable {

    @Field(1)
    public String info;
    @Field(2)
    public int code;

    public AppSetInfoParcel() {
    }

    public AppSetInfoParcel(String info, int code) {
        this.info = info;
        this.code = code;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AppSetInfoParcel> CREATOR = findCreator(AppSetInfoParcel.class);

}
