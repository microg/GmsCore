package com.google.android.gms.pay;

import android.os.Parcel;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

@Hide
@SafeParcelable.Class
public class PushEmoneyCardRequest extends AbstractSafeParcelable {
    @Field(1)
    public String json;

    @Constructor
    public PushEmoneyCardRequest(@Param(1) String json) {
        this.json = json;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PushEmoneyCardRequest> CREATOR = findCreator(PushEmoneyCardRequest.class);
}
