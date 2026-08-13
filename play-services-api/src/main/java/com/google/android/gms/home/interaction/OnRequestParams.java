/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.home.interaction;

import android.os.IBinder;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class OnRequestParams extends AbstractSafeParcelable {
    @Field(1)
    public byte[] request;
    @Field(2)
    public IBinder eventCallback;
    @Field(3)
    public IBinder completionCallback;

    @Constructor
    public OnRequestParams(@Param(1) byte[] request, @Param(2) IBinder eventCallback, @Param(3) IBinder completionCallback) {
        this.request = request;
        this.eventCallback = eventCallback;
        this.completionCallback = completionCallback;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<OnRequestParams> CREATOR = findCreator(OnRequestParams.class);
}
