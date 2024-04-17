/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.feedback;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class LogOptions extends AbstractSafeParcelable {

    @Field(2)
    public String options;
    @Field(3)
    public boolean unknownBool3;
    @Field(4)
    public boolean unknownBool4;
    @Field(5)
    public boolean unknownBool5;
    @Field(6)
    public boolean unknownBool6;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<LogOptions> CREATOR = findCreator(LogOptions.class);

}
