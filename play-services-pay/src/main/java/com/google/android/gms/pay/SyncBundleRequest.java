package com.google.android.gms.pay;

import android.os.Parcel;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

@Hide
@SafeParcelable.Class
public class SyncBundleRequest extends AbstractSafeParcelable {
    @Field(1)
    public String unknownField;

    @Constructor
    public SyncBundleRequest(@Param(1) String unknownField) {
        this.unknownField = unknownField;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SyncBundleRequest> CREATOR = findCreator(SyncBundleRequest.class);
}
