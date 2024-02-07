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

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class AppSetIdRequestParams extends AbstractSafeParcelable {

    @Field(1)
    public String label;
    @Field(2)
    public String data;

    public AppSetIdRequestParams() {
    }

    public AppSetIdRequestParams(String str, String str2) {
        this.label = str;
        this.data = str2;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("AppSetIdRequestParams").value(label).value(data).end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AppSetIdRequestParams> CREATOR = findCreator(AppSetIdRequestParams.class);
}
