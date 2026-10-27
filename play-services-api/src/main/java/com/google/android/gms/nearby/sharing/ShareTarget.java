/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.nearby.sharing;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class ShareTarget extends AbstractSafeParcelable {
    @Field(1)
    public final long id;
    @Field(2)
    public final String name;
    @Field(3)
    public final int type;
    @Field(4)
    public final String endpointId;
    @Field(5)
    public final String deviceId;

    @Constructor
    public ShareTarget(@Param(1) long id, @Param(2) String name, @Param(3) int type, @Param(4) String endpointId, @Param(5) String deviceId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.endpointId = endpointId;
        this.deviceId = deviceId;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ShareTarget> CREATOR = findCreator(ShareTarget.class);
}
