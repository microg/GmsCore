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

import org.microg.gms.common.Constants;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
@Hide
public class Application extends AbstractSafeParcelable {

    public static final Application GMS_APP = new Application(Constants.GMS_PACKAGE_NAME);

    @Field(value = 1, getterName = "getPackageName")
    @NonNull
    private final String packageName;

    @Constructor
    public Application(@Param(1) @NonNull String packageName) {
        this.packageName = packageName;
    }

    @NonNull
    public String getPackageName() {
        return packageName;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("Application").value(packageName).end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Application> CREATOR = findCreator(Application.class);

}
