package com.google.android.gms.asterism;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class SetAsterismConsentResponse extends AbstractSafeParcelable {
    public static SafeParcelableCreatorAndWriter<SetAsterismConsentResponse> CREATOR =
            findCreator(SetAsterismConsentResponse.class);

    @Field(1)
    public final int requestCode;

    @Field(2)
    @Nullable
    public final String gmscoreIidToken;

    @Field(3)
    @Nullable
    public final String fid;

    @Constructor
    public SetAsterismConsentResponse(
            @Param(1) int requestCode,
            @Param(2) @Nullable String gmscoreIidToken,
            @Param(3) @Nullable String fid
    ) {
        this.requestCode = requestCode;
        this.gmscoreIidToken = gmscoreIidToken;
        this.fid = fid;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}