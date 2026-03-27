package com.google.android.gms.constellation;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class PhoneNumberInfo extends AbstractSafeParcelable {
    public static SafeParcelableCreatorAndWriter<PhoneNumberInfo> CREATOR =
            findCreator(PhoneNumberInfo.class);
    @Field(1)
    public final int version;
    @Field(2)
    @Nullable
    public final String phoneNumber;
    @Field(3)
    public final long verificationTime;
    @Field(4)
    @Nullable
    public final Bundle extras;

    @Constructor
    public PhoneNumberInfo(
            @SafeParcelable.Param(1) int version,
            @SafeParcelable.Param(2) @Nullable String phoneNumber,
            @SafeParcelable.Param(3) long verificationTime,
            @SafeParcelable.Param(4) @Nullable Bundle extras
    ) {
        this.version = version;
        this.phoneNumber = phoneNumber;
        this.verificationTime = verificationTime;
        this.extras = extras;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}