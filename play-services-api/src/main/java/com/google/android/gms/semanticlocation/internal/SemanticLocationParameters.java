/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.semanticlocation.internal;

import android.accounts.Account;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class SemanticLocationParameters extends AbstractSafeParcelable {
    @Field(1)
    public final Account account;
    @Field(2)
    public final String clientIdentifier;
    @Field(3)
    public final String packageName;

    @Constructor
    public SemanticLocationParameters(@Param(1) Account account, @Param(2) String clientIdentifier, @Param(3) String packageName) {
        this.account = account;
        this.clientIdentifier = clientIdentifier;
        this.packageName = packageName;
    }

    public static final SafeParcelableCreatorAndWriter<SemanticLocationParameters> CREATOR = findCreator(SemanticLocationParameters.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
