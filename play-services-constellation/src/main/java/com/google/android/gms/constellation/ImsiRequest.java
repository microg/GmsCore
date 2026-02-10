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
public class ImsiRequest extends AbstractSafeParcelable {

    @Field(1)
    public String imsi;

    @Field(2)
    public String carrierId;

    private ImsiRequest() {
    }

    @Constructor
    public ImsiRequest(@Param(1) String imsi, @Param(2) String carrierId) {
        this.imsi = imsi;
        this.carrierId = carrierId;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ImsiRequest> CREATOR = findCreator(ImsiRequest.class);
}
