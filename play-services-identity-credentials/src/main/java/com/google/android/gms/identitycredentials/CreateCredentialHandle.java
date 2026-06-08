/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.identitycredentials;

import android.app.PendingIntent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;

@SafeParcelable.Class
@Hide
public class CreateCredentialHandle extends AbstractSafeParcelable {
    @Field(1)
    @Nullable
    public final PendingIntent pendingIntent;
    @Field(2)
    @Nullable
    public final CreateCredentialResponse createCredentialResponse;

    @Constructor
    public CreateCredentialHandle(@Param(1) @Nullable PendingIntent pendingIntent, @Param(2) @Nullable CreateCredentialResponse createCredentialResponse) {
        if (pendingIntent == null && createCredentialResponse == null) {
            throw new IllegalArgumentException("pendingIntent or createCredentialResponse must be specified.");
        }
        this.pendingIntent = pendingIntent;
        this.createCredentialResponse = createCredentialResponse;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CreateCredentialHandle> CREATOR = findCreator(CreateCredentialHandle.class);
}
