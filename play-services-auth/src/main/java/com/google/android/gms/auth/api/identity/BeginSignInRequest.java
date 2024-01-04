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
public class BeginSignInRequest extends AbstractSafeParcelable {
    @Field(1)
    public PasswordRequestOptions passwordRequestOptions;
    @Field(2)
    public GoogleIdTokenRequestOptions googleIdTokenRequestOptions;
    @Field(3)
    public String score;
    @Field(4)
    public boolean isPrimary;
    @Field(5)
    public int code;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<BeginSignInRequest> CREATOR = findCreator(BeginSignInRequest.class);
}
