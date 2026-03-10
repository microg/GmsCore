package com.google.android.gms.pay;

import android.os.Parcel;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

@Hide
@SafeParcelable.Class
public class GetPendingIntentForWalletOnWearRequest extends AbstractSafeParcelable {
    @Field(2)
    public String wearNodeId;
    @Field(3)
    public int intentSource;

    @Constructor
    public GetPendingIntentForWalletOnWearRequest(@Param(2) String wearNodeId, @Param(3) int intentSource) {
        this.wearNodeId = wearNodeId;
        this.intentSource = intentSource;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetPendingIntentForWalletOnWearRequest> CREATOR = findCreator(GetPendingIntentForWalletOnWearRequest.class);
}
