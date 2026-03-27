package com.google.android.gms.constellation;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class GetIidTokenRequest extends AbstractSafeParcelable {
    public static SafeParcelableCreatorAndWriter<GetIidTokenRequest> CREATOR =
            findCreator(GetIidTokenRequest.class);
    @Field(1)
    @Nullable
    public final Long projectNumber;

    @Constructor
    public GetIidTokenRequest(
            @Param(1) @Nullable Long projectNumber
    ) {
        this.projectNumber = projectNumber;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}