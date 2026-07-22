/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.locationsharingreporter;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class NoticeAckedUpdateRequest extends AbstractSafeParcelable {
    @Field(1)
    public final int isConfirmed;

    @Constructor
    public NoticeAckedUpdateRequest(@Param(1) int isConfirmed) {
        this.isConfirmed = isConfirmed;
    }

    @NonNull
    @Override
    public final String toString() {
        return "NoticeAckedUpdateRequest{" +
                "isConfirmed=" + isConfirmed +
                '}';
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<NoticeAckedUpdateRequest> CREATOR = findCreator(NoticeAckedUpdateRequest.class);
}
