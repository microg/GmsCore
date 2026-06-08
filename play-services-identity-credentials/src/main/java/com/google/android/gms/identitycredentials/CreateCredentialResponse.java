/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.identitycredentials;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;

@SafeParcelable.Class
@Hide
public class CreateCredentialResponse extends AbstractSafeParcelable {
    @Field(1)
    @NonNull
    public final String type;
    @Field(2)
    @NonNull
    public final Bundle data;

    @Constructor
    public CreateCredentialResponse(@Param(1) @NonNull String type, @Param(2) @NonNull Bundle data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CreateCredentialResponse> CREATOR = findCreator(CreateCredentialResponse.class);
}
