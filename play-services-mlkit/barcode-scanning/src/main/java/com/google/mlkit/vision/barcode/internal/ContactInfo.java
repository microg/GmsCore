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
public class ContactInfo extends AbstractSafeParcelable {
    @Field(1)
    @Nullable
    public PersonName name;
    @Field(2)
    @Nullable
    public String organization;
    @Field(3)
    @Nullable
    public String title;
    @Field(4)
    @Nullable
    public Phone[] phones;
    @Field(5)
    @Nullable
    public Email[] emails;
    @Field(6)
    @Nullable
    public String[] urls;
    @Field(7)
    @Nullable
    public Address[] addresses;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ContactInfo> CREATOR = findCreator(ContactInfo.class);
}
