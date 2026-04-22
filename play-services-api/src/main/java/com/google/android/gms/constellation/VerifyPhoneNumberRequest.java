/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class VerifyPhoneNumberRequest extends AbstractSafeParcelable {
    @Field(1)
    public Long subscriptionId;
    @Field(2)
    public String phoneNumber;
    @Field(3)
    public int verificationMethod;
    @Field(4)
    public String locale;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<VerifyPhoneNumberRequest> CREATOR = findCreator(VerifyPhoneNumberRequest.class);
}
