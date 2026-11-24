/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Request to get an Instance ID token.
 */
@SafeParcelable.Class
public class GetIidTokenRequest extends AbstractSafeParcelable {
    @Field(1)
    public Long subscriptionId;

    @Constructor
    public GetIidTokenRequest(@Param(1) Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetIidTokenRequest> CREATOR = findCreator(GetIidTokenRequest.class);
}
