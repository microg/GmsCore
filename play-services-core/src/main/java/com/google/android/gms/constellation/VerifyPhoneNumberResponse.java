/*
 * Copyright (C) 2013-2026 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.constellation;

import android.os.Bundle;
import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class VerifyPhoneNumberResponse extends AbstractSafeParcelable {
    public static SafeParcelableCreatorAndWriter<VerifyPhoneNumberResponse> CREATOR = findCreator(VerifyPhoneNumberResponse.class);

    @SafeParcelable.Field(1)
    @Nullable
    public final PhoneNumberVerification[] verifications;

    @SafeParcelable.Field(2)
    @Nullable
    public final Bundle extras;

    @SafeParcelable.Constructor
    public VerifyPhoneNumberResponse(
            @SafeParcelable.Param(1) @Nullable PhoneNumberVerification[] verifications,
            @SafeParcelable.Param(2) @Nullable Bundle extras) {
        this.verifications = verifications;
        this.extras = extras;
    }

    @SafeParcelable.Class
    public static class PhoneNumberVerification extends AbstractSafeParcelable {
        public static SafeParcelableCreatorAndWriter<PhoneNumberVerification> CREATOR = findCreator(PhoneNumberVerification.class);

        @SafeParcelable.Field(1)
        @Nullable
        public final String phoneNumber;

        @SafeParcelable.Field(2)
        public final long timestampMillis;

        @SafeParcelable.Field(3)
        public final int verificationMethod;

        @SafeParcelable.Field(4)
        public final int simSlot;

        @SafeParcelable.Field(5)
        @Nullable
        public final String verificationToken;

        @SafeParcelable.Field(6)
        @Nullable
        public final Bundle extras;

        @SafeParcelable.Field(7)
        public final int verificationStatus;

        @SafeParcelable.Field(8)
        public final long retryAfterSeconds;

        @SafeParcelable.Constructor
        public PhoneNumberVerification(
                @SafeParcelable.Param(1) @Nullable String phoneNumber,
                @SafeParcelable.Param(2) long timestampMillis,
                @SafeParcelable.Param(3) int verificationMethod,
                @SafeParcelable.Param(4) int simSlot,
                @SafeParcelable.Param(5) @Nullable String verificationToken,
                @SafeParcelable.Param(6) @Nullable Bundle extras,
                @SafeParcelable.Param(7) int verificationStatus,
                @SafeParcelable.Param(8) long retryAfterSeconds) {
            this.phoneNumber = phoneNumber;
            this.timestampMillis = timestampMillis;
            this.verificationMethod = verificationMethod;
            this.simSlot = simSlot;
            this.verificationToken = verificationToken;
            this.extras = extras;
            this.verificationStatus = verificationStatus;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
