/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.identity;

import android.app.PendingIntent;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.Objects;

/**
 * The result returned from calling {@link CredentialSavingClient#savePassword(SavePasswordRequest)} that includes a
 * {@link PendingIntent} that can be used to launch the password saving flow.
 */
@SafeParcelable.Class
public class SavePasswordResult extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getPendingIntent")
    private final PendingIntent pendingIntent;

    @Constructor
    public SavePasswordResult(@Param(1) PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    /**
     * Returns the {@link PendingIntent} that should be launched to start the UI flow for saving the password.
     */
    public PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SavePasswordResult)) return false;

        SavePasswordResult that = (SavePasswordResult) o;

        return Objects.equals(pendingIntent, that.pendingIntent);
    }

    @Override
    public int hashCode() {
        return pendingIntent != null ? pendingIntent.hashCode() : 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SavePasswordResult> CREATOR = findCreator(SavePasswordResult.class);
}
