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
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Returns a response for the {@link IdentityCredentialClient#createCredential} API that can be used to launch the credential selector UIs to
 * finalize on a credential of the user's choice that can be used for app sign-in, or the actual credential response itself if no UI is needed.
 */
@SafeParcelable.Class
public class CreateCredentialHandle extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getPendingIntent")
    @Nullable
    private final PendingIntent pendingIntent;
    @Field(value = 2, getterName = "getCreateCredentialResponse")
    @Nullable
    private final CreateCredentialResponse createCredentialResponse;

    /**
     * constructs an instance of {@link CreateCredentialHandle}
     *
     * @param pendingIntent            the {@link PendingIntent} to launch the credential selector UI
     * @param createCredentialResponse the {@link CreateCredentialResponse} if no UI is needed
     */
    @Constructor
    public CreateCredentialHandle(@Param(1) @Nullable PendingIntent pendingIntent, @Param(2) @Nullable CreateCredentialResponse createCredentialResponse) {
        if (pendingIntent == null && createCredentialResponse == null) {
            throw new IllegalArgumentException("pendingIntent or createCredentialResponse must be specified.");
        }
        this.pendingIntent = pendingIntent;
        this.createCredentialResponse = createCredentialResponse;
    }

    /**
     * the {@link CreateCredentialResponse} if no UI is needed
     */
    @Nullable
    public CreateCredentialResponse getCreateCredentialResponse() {
        return createCredentialResponse;
    }

    /**
     * the {@link PendingIntent} to launch the credential selector UI
     */
    @Nullable
    public PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CreateCredentialHandle> CREATOR = findCreator(CreateCredentialHandle.class);
}
