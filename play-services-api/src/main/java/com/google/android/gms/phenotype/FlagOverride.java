/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.phenotype;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class FlagOverride extends AbstractSafeParcelable {
    @Field(2)
    public final String packageName;
    @Field(3)
    public final String user;
    @Field(4)
    public final Flag flag;
    @Field(5)
    public final boolean committed;

    @Constructor
    public FlagOverride(@Param(2) String packageName, @Param(3) String user,
                        @Param(4) Flag flag, @Param(5) boolean committed) {
        this.packageName = packageName;
        this.user = user;
        this.flag = flag;
        this.committed = committed;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<FlagOverride> CREATOR = findCreator(FlagOverride.class);
}
