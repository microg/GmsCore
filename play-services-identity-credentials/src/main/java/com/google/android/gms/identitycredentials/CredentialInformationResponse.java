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
public class CredentialInformationResponse extends AbstractSafeParcelable {
    @Field(1)
    @Nullable
    public final List<CredentialInformation> credentialInformationList;

    @Constructor
    public CredentialInformationResponse(@Param(1) @Nullable List<CredentialInformation> credentialInformationList) {
        this.credentialInformationList = credentialInformationList;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CredentialInformationResponse> CREATOR = findCreator(CredentialInformationResponse.class);
}
