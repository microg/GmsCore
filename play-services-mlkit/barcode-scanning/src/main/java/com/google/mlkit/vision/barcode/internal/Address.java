/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.barcode.internal;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class Address extends AbstractSafeParcelable {
    @Field(1)
    public int type;
    @Field(2)
    public String[] addressLines;

    // TODO: Copied from com.google.mlkit.vision.barcode.common.Barcode.Address
    public static final int UNKNOWN = 0;
    public static final int WORK = 1;
    public static final int HOME = 2;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Address> CREATOR = findCreator(Address.class);
}
