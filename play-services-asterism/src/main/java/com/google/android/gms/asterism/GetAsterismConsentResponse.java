package com.google.android.gms.asterism;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class GetAsterismConsentResponse extends AbstractSafeParcelable {
    public static SafeParcelableCreatorAndWriter<GetAsterismConsentResponse> CREATOR =
            findCreator(GetAsterismConsentResponse.class);

    @Field(1)
    public final int requestCode;

    @Field(2)
    public final int consentStateValue;

    @Field(3)
    @Nullable
    public final String gmscoreIidToken;

    @Field(4)
    @Nullable
    public final String fid;

    @Field(5)
    public final int consentTypeValue;

    @Constructor
    public GetAsterismConsentResponse(
            @Param(1) int requestCode,
            @Param(2) int consentStateValue,
            @Param(3) @Nullable String gmscoreIidToken,
            @Param(4) @Nullable String fid,
            @Param(5) int consentTypeValue
    ) {
        this.requestCode = requestCode;
        this.consentStateValue = consentStateValue;
        this.gmscoreIidToken = gmscoreIidToken;
        this.fid = fid;
        this.consentTypeValue = consentTypeValue;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}