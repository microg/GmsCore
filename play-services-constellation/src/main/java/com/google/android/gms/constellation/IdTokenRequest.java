/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

@Hide
@SafeParcelable.Class
public class IdTokenRequest extends AbstractSafeParcelable {

    @Field(1)
    public String appId;

    @Field(2)
    public String hash;

    private IdTokenRequest() {
    }

    @Constructor
    public IdTokenRequest(@Param(1) String appId, @Param(2) String hash) {
        this.appId = appId;
        this.hash = hash;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<IdTokenRequest> CREATOR = findCreator(IdTokenRequest.class);
}
