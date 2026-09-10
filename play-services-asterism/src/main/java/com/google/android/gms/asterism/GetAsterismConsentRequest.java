package com.google.android.gms.asterism;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class GetAsterismConsentRequest extends AbstractSafeParcelable {
    public static SafeParcelableCreatorAndWriter<GetAsterismConsentRequest> CREATOR =
            findCreator(GetAsterismConsentRequest.class);

    @Field(1)
    public final int requestCode;

    @Field(2)
    public final int asterismClientValue;

    @Constructor
    public GetAsterismConsentRequest(
            @Param(1) int requestCode,
            @Param(2) int asterismClientValue
    ) {
        this.requestCode = requestCode;
        this.asterismClientValue = asterismClientValue;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}