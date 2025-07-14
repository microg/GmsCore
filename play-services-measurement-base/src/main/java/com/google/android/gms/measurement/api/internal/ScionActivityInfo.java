/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.measurement.api.internal;

import android.content.Intent;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class ScionActivityInfo extends AbstractSafeParcelable {
    @Field(1)
    public final int id;
    @Field(2)
    public final String className;
    @Field(3)
    public final Intent intent;

    @Constructor
    public ScionActivityInfo(@Param(1) int id, @Param(2) String className, @Param(3) Intent intent) {
        this.id = id;
        this.className = className;
        this.intent = intent;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ScionActivityInfo> CREATOR = findCreator(ScionActivityInfo.class);
}
