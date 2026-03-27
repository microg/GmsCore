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
    @Field(1)
    public final PhoneNumberVerification[] verifications;

    @Field(2)
    public final Bundle extras;

    @Constructor
    public VerifyPhoneNumberResponse(
            @Param(1) PhoneNumberVerification[] verifications,
            @Param(2) Bundle extras
    ) {
        this.verifications = verifications;
        this.extras = extras;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static SafeParcelableCreatorAndWriter<VerifyPhoneNumberResponse> CREATOR =
            findCreator(VerifyPhoneNumberResponse.class);

    @SafeParcelable.Class
    public static class PhoneNumberVerification extends AbstractSafeParcelable {
        @Field(1)
        @Nullable
        public final String phoneNumber;

        @Field(2)
        public final long timestampMillis;

        @Field(3)
        public final int verificationMethod;

        @Field(4)
        public final int simSlot;

        @Field(5)
        @Nullable
        public final String verificationToken;

        @Field(6)
        @Nullable
        public final Bundle extras;

        @Field(7)
        public final int verificationStatus;

        @Field(8)
        public final long retryAfterSeconds;

        @Constructor
        public PhoneNumberVerification(
                @Param(1) @Nullable String phoneNumber,
                @Param(2) long timestampMillis,
                @Param(3) int verificationMethod,
                @Param(4) int simSlot,
                @Param(5) @Nullable String verificationToken,
                @Param(6) @Nullable Bundle extras,
                @Param(7) int verificationStatus,
                @Param(8) long retryAfterSeconds
        ) {
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

        public static SafeParcelableCreatorAndWriter<PhoneNumberVerification> CREATOR =
                findCreator(PhoneNumberVerification.class);
    }
}