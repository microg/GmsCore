/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.identitycredentials;

import android.os.Bundle;
import android.os.Parcel;
import android.os.ResultReceiver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;

@SafeParcelable.Class
@Hide
public class CreateCredentialRequest extends AbstractSafeParcelable {
    @Field(1)
    @NonNull
    public final String type;
    @Field(2)
    @NonNull
    public final Bundle credentialData;
    @Field(3)
    @NonNull
    public final Bundle candidateQueryData;
    @Field(4)
    @Nullable
    public final String origin;
    @Field(5)
    @Nullable
    public final String requestJson;
    @Field(6)
    @Nullable
    public final ResultReceiver resultReceiver;

    @Constructor
    public CreateCredentialRequest(@Param(1) @NonNull String type, @Param(2) @NonNull Bundle credentialData, @Param(3) @NonNull Bundle candidateQueryData, @Param(4) @Nullable String origin, @Param(5) @Nullable String requestJson, @Param(6) @Nullable ResultReceiver resultReceiver) {
        this.type = type;
        this.credentialData = credentialData;
        this.candidateQueryData = candidateQueryData;
        this.origin = origin;
        this.requestJson = requestJson;
        this.resultReceiver = resultReceiver;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CreateCredentialRequest> CREATOR = findCreator(CreateCredentialRequest.class);
}
