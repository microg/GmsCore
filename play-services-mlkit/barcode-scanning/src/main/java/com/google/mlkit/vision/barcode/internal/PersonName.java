/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.barcode.internal;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class PersonName extends AbstractSafeParcelable {
    @Field(1)
    @Nullable
    public String formattedName;
    @Field(2)
    @Nullable
    public String pronunciation;
    @Field(3)
    @Nullable
    public String prefix;
    @Field(4)
    @Nullable
    public String first;
    @Field(5)
    @Nullable
    public String middle;
    @Field(6)
    @Nullable
    public String last;
    @Field(7)
    @Nullable
    public String suffix;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PersonName> CREATOR = findCreator(PersonName.class);
}
