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

@SafeParcelable.Class
@Hide
public class CredentialInformation extends AbstractSafeParcelable {
    @Field(1)
    @Nullable
    public final String packageName;
    @Field(2)
    public final int hasPasswordCredential;
    @Field(3)
    public final int hasPublicKeyCredential;
    @Field(4)
    public final int hasGoogleAccount;
    @Field(5)
    public final int reserved;

    @Constructor
    public CredentialInformation(@Param(1) @Nullable String packageName, @Param(2) int hasPasswordCredential, @Param(3) int hasPublicKeyCredential, @Param(4) int hasGoogleAccount, @Param(5) int reserved) {
        this.packageName = packageName;
        this.hasPasswordCredential = hasPasswordCredential;
        this.hasPublicKeyCredential = hasPublicKeyCredential;
        this.hasGoogleAccount = hasGoogleAccount;
        this.reserved = reserved;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CredentialInformation> CREATOR = findCreator(CredentialInformation.class);
}
