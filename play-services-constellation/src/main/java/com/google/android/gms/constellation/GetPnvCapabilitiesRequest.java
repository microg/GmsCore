package com.google.android.gms.constellation;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public class GetPnvCapabilitiesRequest extends AbstractSafeParcelable {
    public static SafeParcelableCreatorAndWriter<GetPnvCapabilitiesRequest> CREATOR =
            findCreator(GetPnvCapabilitiesRequest.class);
    @Field(1)
    public final String policyId;
    @Field(2)
    public final List<Integer> verificationTypes;
    @Field(3)
    public final List<Integer> simSlotIndices;

    @Constructor
    public GetPnvCapabilitiesRequest(
            @Param(1) String policyId,
            @Param(2) List<Integer> verificationTypes,
            @Param(3) List<Integer> simSlotIndices
    ) {
        this.policyId = policyId;
        this.verificationTypes = verificationTypes;
        this.simSlotIndices = simSlotIndices;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}