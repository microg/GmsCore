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

@SafeParcelable.Class
public class IdTokenRequest extends AbstractSafeParcelable {
    @Field(1)
    public String audience;
    @Field(2)
    public String nonce;

    @Constructor
    public IdTokenRequest(@Param(1) String audience, @Param(2) String nonce) {
        this.audience = audience;
        this.nonce = nonce;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<IdTokenRequest> CREATOR = findCreator(IdTokenRequest.class);
}
