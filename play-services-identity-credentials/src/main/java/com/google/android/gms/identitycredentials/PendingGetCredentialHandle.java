/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.identitycredentials;

import android.app.PendingIntent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;

@SafeParcelable.Class
@Hide
public class PendingGetCredentialHandle extends AbstractSafeParcelable {
    @Field(1)
    public final PendingIntent pendingIntent;

    @Constructor
    public PendingGetCredentialHandle(@Param(1) PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PendingGetCredentialHandle> CREATOR = findCreator(PendingGetCredentialHandle.class);
}
