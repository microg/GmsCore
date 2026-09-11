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
 * Response to {@link IdentityCredentialClient#importCredentials} API, containing a {@link PendingIntent} that can be used to launch a selector
 * that allows the user to select a credential provider to import credentials from
 */
@SafeParcelable.Class
public class PendingImportCredentialsHandle extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getPendingIntent")
    @NonNull
    private final PendingIntent pendingIntent;

    /**
     * @param pendingIntent the intent that launches the selector UI
     */
    @Constructor
    public PendingImportCredentialsHandle(@NonNull @Param(1) PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    /**
     * the intent that launches the selector UI
     */
    @NonNull
    public PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PendingImportCredentialsHandle> CREATOR = findCreator(PendingImportCredentialsHandle.class);
}
