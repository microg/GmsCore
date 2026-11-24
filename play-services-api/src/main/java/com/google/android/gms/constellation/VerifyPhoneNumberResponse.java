/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import android.os.Bundle;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

/**
 * Response from phone number verification.
 *
 * Contains an array of PhoneNumberVerification objects - one for each
 * phone number that was verified or attempted.
 */
@SafeParcelable.Class
public class VerifyPhoneNumberResponse extends AbstractSafeParcelable {
    @Field(1)
    public PhoneNumberVerification[] verifications;
    @Field(2)
    public Bundle extras;

    @Constructor
    public VerifyPhoneNumberResponse(
            @Param(1) PhoneNumberVerification[] verifications,
            @Param(2) Bundle extras) {
        this.verifications = verifications;
        this.extras = extras;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<VerifyPhoneNumberResponse> CREATOR = findCreator(VerifyPhoneNumberResponse.class);
}
