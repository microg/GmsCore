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
public class DriverLicense extends AbstractSafeParcelable {
    @Field(1)
    @Nullable
    public String documentType;
    @Field(2)
    @Nullable
    public String firstName;
    @Field(3)
    @Nullable
    public String middleName;
    @Field(4)
    @Nullable
    public String listName;
    @Field(5)
    @Nullable
    public String gender;
    @Field(6)
    @Nullable
    public String addressStreet;
    @Field(7)
    @Nullable
    public String addressCity;
    @Field(8)
    @Nullable
    public String addressState;
    @Field(9)
    @Nullable
    public String addressZip;
    @Field(10)
    @Nullable
    public String licenseNumber;
    @Field(11)
    @Nullable
    public String issueDate;
    @Field(12)
    @Nullable
    public String expiryDate;
    @Field(13)
    @Nullable
    public String birthDate;
    @Field(14)
    @Nullable
    public String issuingCountry;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DriverLicense> CREATOR = findCreator(DriverLicense.class);
}
