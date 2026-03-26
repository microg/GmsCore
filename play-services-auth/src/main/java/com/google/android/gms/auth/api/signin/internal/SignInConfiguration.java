/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.signin.internal;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

import java.util.Objects;

@Hide
@SafeParcelable.Class
public class SignInConfiguration extends AbstractSafeParcelable {
    @Field(value = 2, getterName = "getPackageName")
    @NonNull
    private final String packageName;
    @Field(value = 5, getterName = "getOptions")
    @NonNull
    private final GoogleSignInOptions options;

    @Constructor
    public SignInConfiguration(@Param(2) @NonNull String packageName, @Param(5) @NonNull GoogleSignInOptions options) {
        this.packageName = packageName;
        this.options = options;
    }

    @NonNull
    public String getPackageName() {
        return packageName;
    }

    @NonNull
    public GoogleSignInOptions getOptions() {
        return options;
    }


    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof SignInConfiguration)) return false;

        SignInConfiguration that = (SignInConfiguration) o;
        return packageName.equals(that.packageName) && Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        int hash = packageName.hashCode() + 31;
        hash = Objects.hashCode(options) + (hash * 31);;
        return hash;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("SignInConfiguration").field("packageName", packageName).field("options", options).end();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SignInConfiguration> CREATOR = findCreator(SignInConfiguration.class);
}
