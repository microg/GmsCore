/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.identitycredentials;

import android.app.PendingIntent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Returns a response for the {@link IdentityCredentialClient#getCredential} API that can be used to launch the credential selector UIs to
 * finalize on a credential of the user's choice that can be used for app sign-in.
 */
@SafeParcelable.Class
public class PendingGetCredentialHandle extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getPendingIntent")
    @NonNull
    private final PendingIntent pendingIntent;

    /**
     * constructs an instance of {@link PendingGetCredentialHandle}
     *
     * @param pendingIntent the {@link PendingIntent} to launch the credential selector UI
     */
    @Constructor
    public PendingGetCredentialHandle(@NonNull @Param(1) PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    /**
     * the {@link PendingIntent} to launch the credential selector UI
     */
    @NonNull
    public PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PendingGetCredentialHandle> CREATOR = findCreator(PendingGetCredentialHandle.class);
}
