/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.identitycredentials;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Response of a registry deletion operation.
 */
@SafeParcelable.Class
public class ClearCreationOptionsResponse extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "isDeleted")
    private final boolean isDeleted;

    /**
     * constructs an instance of {@link ClearCreationOptionsResponse}
     * @param isDeleted if true, indicates clear operation deleted some registries, otherwise indicates there was no data to delete; unexpected failures will be thrown as exceptions
     */
    @Constructor
    public ClearCreationOptionsResponse(@Param(1) boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    /**
     * if true, indicates clear operation deleted some registries, otherwise indicates there was no data to delete; unexpected failures will be thrown as exceptions
     */
    public final boolean isDeleted() {
        return this.isDeleted;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ClearCreationOptionsResponse> CREATOR = findCreator(ClearCreationOptionsResponse.class);
}
