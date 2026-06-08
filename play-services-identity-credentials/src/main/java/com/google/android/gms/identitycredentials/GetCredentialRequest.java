/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.identitycredentials;

import android.os.Bundle;
import android.os.Parcel;
import android.os.ResultReceiver;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;

import java.util.List;

@SafeParcelable.Class
@Hide
public class GetCredentialRequest extends AbstractSafeParcelable {
    @Field(1)
    public final List<CredentialOption> credentialOptions;
    @Field(2)
    public final Bundle data;
    @Field(3)
    public final String origin;
    @Field(4)
    public final ResultReceiver resultReceiver;

    @Constructor
    public GetCredentialRequest(@Param(1) List<CredentialOption> credentialOptions, @Param(2) Bundle data, @Param(3) String origin, @Param(4) ResultReceiver resultReceiver) {
        this.credentialOptions = credentialOptions;
        this.data = data;
        this.origin = origin;
        this.resultReceiver = resultReceiver;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetCredentialRequest> CREATOR = findCreator(GetCredentialRequest.class);
}
