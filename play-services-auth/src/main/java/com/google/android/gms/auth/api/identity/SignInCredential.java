/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.identity;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class SignInCredential extends AbstractSafeParcelable {
    @Field(1)
    public String email;
    @Field(2)
    public String displayName;
    @Field(3)
    public String familyName;
    @Field(4)
    public String givenName;
    @Field(5)
    public String avatar;
    @Field(6)
    public String serverAuthCode;
    @Field(7)
    public String idToken;
    @Field(8)
    public String obfuscatedIdentifier;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SignInCredential> CREATOR = findCreator(SignInCredential.class);
}
