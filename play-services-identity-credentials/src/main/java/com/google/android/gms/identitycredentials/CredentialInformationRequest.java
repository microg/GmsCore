/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.identitycredentials;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;

import java.util.List;

@SafeParcelable.Class
@Hide
public class CredentialInformationRequest extends AbstractSafeParcelable {
    @Field(1)
    @Nullable
    public final List<String> packageNames;

    @Constructor
    public CredentialInformationRequest(@Param(1) @Nullable List<String> packageNames) {
        this.packageNames = packageNames;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CredentialInformationRequest> CREATOR = findCreator(CredentialInformationRequest.class);
}
