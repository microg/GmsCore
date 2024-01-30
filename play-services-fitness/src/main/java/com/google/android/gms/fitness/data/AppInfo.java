/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.data;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class AppInfo extends AbstractSafeParcelable {

    public static final AppInfo DEFAULT = new AppInfo("com.google.android.gms");

    @Field(1)
    public String packageName;

    public AppInfo() {
    }

    public AppInfo(String packageName) {
        this.packageName = packageName;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("AppInfo")
                .field("packageName", packageName)
                .end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AppInfo> CREATOR = findCreator(AppInfo.class);

}
