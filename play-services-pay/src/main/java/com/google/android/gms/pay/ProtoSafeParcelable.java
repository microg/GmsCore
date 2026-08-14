package com.google.android.gms.pay;

import android.os.Parcel;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class ProtoSafeParcelable extends AbstractSafeParcelable {
    @Field(1)
    public byte[] data;

    @Constructor
    public ProtoSafeParcelable(@Param(1) byte[] data) {
        this.data = data;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ProtoSafeParcelable> CREATOR = findCreator(ProtoSafeParcelable.class);
}
