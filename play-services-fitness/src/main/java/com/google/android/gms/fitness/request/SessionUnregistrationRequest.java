/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.request;

import android.app.PendingIntent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class SessionUnregistrationRequest extends AbstractSafeParcelable {
    @Field(1)
    public PendingIntent intent;
    @Field(2)
    public IStatusCallback callback;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SessionUnregistrationRequest> CREATOR = findCreator(SessionUnregistrationRequest.class);
}
