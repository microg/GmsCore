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
public class CredentialOption extends AbstractSafeParcelable {
    @Field(1)
    public final String type;
    @Field(2)
    public final Bundle credentialRetrievalData;
    @Field(3)
    public final Bundle candidateQueryData;
    @Field(4)
    public final String requestMatcher;
    @Field(5)
    public final String requestType;
    @Field(6)
    public final String protocolType;

    @Constructor
    public CredentialOption(@Param(1) String type, @Param(2) Bundle credentialRetrievalData, @Param(3) Bundle candidateQueryData, @Param(4) String requestMatcher, @Param(5) String requestType, @Param(6) String protocolType) {
        this.type = type;
        this.credentialRetrievalData = credentialRetrievalData;
        this.candidateQueryData = candidateQueryData;
        this.requestMatcher = requestMatcher;
        this.requestType = requestType;
        this.protocolType = protocolType;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CredentialOption> CREATOR = findCreator(CredentialOption.class);
}
