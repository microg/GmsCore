package com.google.android.gms.pay;

import android.os.Parcel;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

@Hide
@SafeParcelable.Class
public class GetPayApiAvailabilityStatusRequest extends AbstractSafeParcelable {
    @Field(1)
    public int requestType;

    @Constructor
    public GetPayApiAvailabilityStatusRequest(@Param(1) @PayClient.RequestType int requestType) {
        this.requestType = requestType;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetPayApiAvailabilityStatusRequest> CREATOR = findCreator(GetPayApiAvailabilityStatusRequest.class);

    @Override
    public String toString() {
        return ToStringHelper.name("GetPayApiAvailabilityStatusRequest")
                .field("requestType", requestType)
                .end();
    }
}
