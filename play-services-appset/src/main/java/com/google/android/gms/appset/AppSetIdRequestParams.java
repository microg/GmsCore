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
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
@Hide
public class AppSetIdRequestParams extends AbstractSafeParcelable {
    @Field(1)
    public final String version;
    @Field(2)
    public final String clientAppPackageName;

    @Constructor
    public AppSetIdRequestParams(@Param(1) String version, @Param(2) String clientAppPackageName) {
        this.version = version;
        this.clientAppPackageName = clientAppPackageName;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("AppSetIdRequestParams").field("version", version).field("clientAppPackageName", clientAppPackageName).end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AppSetIdRequestParams> CREATOR = findCreator(AppSetIdRequestParams.class);
}
