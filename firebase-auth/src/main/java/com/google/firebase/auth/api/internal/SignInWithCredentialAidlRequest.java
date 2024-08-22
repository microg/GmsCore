/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class SignInWithCredentialAidlRequest extends AbstractSafeParcelable {

    @Field(1)
    public VerifyAssertionRequest request;

    public SignInWithCredentialAidlRequest(VerifyAssertionRequest request) {
        this.request = request;
    }

    public SignInWithCredentialAidlRequest() {
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SignInWithCredentialAidlRequest> CREATOR = findCreator(SignInWithCredentialAidlRequest.class);

}
