package com.google.android.gms.pay;

import android.os.Parcel;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class GetWalletStatusResponse extends AbstractSafeParcelable {
    @Field(1)
    public boolean a;
    @Field(2)
    public int[] b;

    @Constructor
    public GetWalletStatusResponse() {
    }

    @Constructor
    public GetWalletStatusResponse(@Param(1) boolean a, @Param(2) int[] b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetWalletStatusResponse> CREATOR = findCreator(GetWalletStatusResponse.class);
}
