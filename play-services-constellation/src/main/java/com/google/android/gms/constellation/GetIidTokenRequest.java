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
public class GetIidTokenRequest extends AbstractSafeParcelable {

    @Field(1)
    @Nullable
    public String appId;

    @Field(2)
    @Nullable
    public String scope;

    private GetIidTokenRequest() {
    }

    @Constructor
    public GetIidTokenRequest(@Param(1) @Nullable String appId, @Param(2) @Nullable String scope) {
        this.appId = appId;
        this.scope = scope;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetIidTokenRequest> CREATOR = findCreator(GetIidTokenRequest.class);
}
