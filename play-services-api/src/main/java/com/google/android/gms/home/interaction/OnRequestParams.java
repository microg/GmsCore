/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.home.interaction;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.home.interaction.internal.ICompletionCallback;

@SafeParcelable.Class
public class OnRequestParams extends AbstractSafeParcelable {
    /**
     * Serialized protobuf of the request
     */
    @Field(1)
    public byte[] request;
    @Field(2)
    public IStatusCallback statusCallback;
    @Field(3)
    public ICompletionCallback completionCallback;

    @Constructor
    public OnRequestParams(@Param(1) byte[] request, @Param(2) IStatusCallback statusCallback, @Param(3) ICompletionCallback completionCallback) {
        this.request = request;
        this.statusCallback = statusCallback;
        this.completionCallback = completionCallback;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<OnRequestParams> CREATOR = findCreator(OnRequestParams.class);
}
