/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.identity;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.Arrays;

/**
 * An account ID such as a username or an email, and a password that can be used to sign a user in.
 */
@SafeParcelable.Class
public class SignInPassword extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getId")
    @NonNull
    private final String id;
    @Field(value = 2, getterName = "getPassword")
    @NonNull
    private final String password;

    @Constructor
    public SignInPassword(@Param(1) @NonNull String id, @Param(2) @NonNull String password) {
        this.id = id;
        this.password = password;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SignInPassword)) return false;

        SignInPassword that = (SignInPassword) o;

        if (!id.equals(that.id)) return false;
        return password.equals(that.password);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new String[]{id, password});
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SignInPassword> CREATOR = findCreator(SignInPassword.class);
}
