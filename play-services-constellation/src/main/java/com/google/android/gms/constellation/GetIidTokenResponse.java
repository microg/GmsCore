/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

@Hide
@SafeParcelable.Class
public class GetIidTokenResponse extends AbstractSafeParcelable {

    @Field(1)
    @Nullable
    public String token;

    @Field(2)
    @Nullable
    public String secondaryToken;

    @Field(3)
    @Nullable
    public byte[] signature;

    @Field(4)
    public long timestamp;

    private GetIidTokenResponse() {
    }

    @Constructor
    public GetIidTokenResponse(
            @Param(1) @Nullable String token,
            @Param(2) @Nullable String secondaryToken,
            @Param(3) @Nullable byte[] signature,
            @Param(4) long timestamp) {
        this.token = token;
        this.secondaryToken = secondaryToken;
        this.signature = signature;
        this.timestamp = timestamp;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetIidTokenResponse> CREATOR = findCreator(GetIidTokenResponse.class);
}
