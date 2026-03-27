package com.google.android.gms.constellation;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class GetIidTokenResponse extends AbstractSafeParcelable {
    public static SafeParcelableCreatorAndWriter<GetIidTokenResponse> CREATOR =
            findCreator(GetIidTokenResponse.class);
    @Field(1)
    public final String iidToken;
    @Field(2)
    public final String fid;
    @Field(3)
    @Nullable
    public final byte[] signature;
    @Field(4)
    public final long timestamp;

    @Constructor
    public GetIidTokenResponse(
            @Param(1) String iidToken,
            @Param(2) String fid,
            @Param(3) @Nullable byte[] signature,
            @Param(4) long timestamp
    ) {
        this.iidToken = iidToken;
        this.fid = fid;
        this.signature = signature;
        this.timestamp = timestamp;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}