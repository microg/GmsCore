/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.request;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.internal.IStatusCallback;

@SafeParcelable.Class
public class SubscribeRequest extends AbstractSafeParcelable {
    @Field(1)
    public Subscription subscription;
    @Field(2)
    public boolean isServerType; // SERVER:true  LOCAL_AND_SERVER:false
    @Field(3)
    public IStatusCallback callback;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SubscribeRequest> CREATOR = findCreator(SubscribeRequest.class);
}