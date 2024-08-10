package com.google.android.gms.pay;

import android.os.Parcel;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

@Hide
@SafeParcelable.Class
public class GetWalletStatusResponse extends AbstractSafeParcelable {
    @Field(1)
    public boolean isWalletUiAvailable;
    @Field(2)
    public int[] foundIssues;

    @Constructor
    public GetWalletStatusResponse(@Param(1) boolean isWalletUiAvailable, @Param(2) int[] foundIssues) {
        this.isWalletUiAvailable = isWalletUiAvailable;
        this.foundIssues = foundIssues;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetWalletStatusResponse> CREATOR = findCreator(GetWalletStatusResponse.class);
}
