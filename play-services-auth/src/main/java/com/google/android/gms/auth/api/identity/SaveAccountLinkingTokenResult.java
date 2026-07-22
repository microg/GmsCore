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
import org.microg.gms.common.Hide;

import java.util.Objects;

/**
 * Result returned from the initial call to save an account linking token that includes a {@link PendingIntent} that can be used to
 * continue the flow.
 */
@SafeParcelable.Class
public class SaveAccountLinkingTokenResult extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getPendingIntent")
    private final PendingIntent pendingIntent;

    @Constructor
    @Hide
    public SaveAccountLinkingTokenResult(@Param(1) PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    /**
     * Returns the {@link PendingIntent} that can be used to launch the flow. Note that this method can return a {@code null} value if such
     * flow cannot be started. It is expected that the caller first calls {@link #hasResolution()} to make sure the flow can be started,
     * instead of examining the nullness of the result returned by this method.
     */
    public PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    /**
     * Returns {@code true} if and only if this result contains a resolution that needs to be launched.
     */
    public boolean hasResolution() {
        return pendingIntent != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SaveAccountLinkingTokenResult)) return false;

        SaveAccountLinkingTokenResult that = (SaveAccountLinkingTokenResult) o;

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

    public static final SafeParcelableCreatorAndWriter<SaveAccountLinkingTokenResult> CREATOR = findCreator(SaveAccountLinkingTokenResult.class);
}
