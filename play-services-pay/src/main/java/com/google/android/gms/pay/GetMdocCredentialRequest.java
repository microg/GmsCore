package com.google.android.gms.pay;

import android.os.Parcel;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

@Hide
@SafeParcelable.Class
public class GetMdocCredentialRequest extends AbstractSafeParcelable {
    @Field(1)
    public String unknownFieldA;
    @Field(2)
    public byte[] unknownFieldB;
    @Field(5)
    public byte[] unknownFieldC;

    @Constructor
    public GetMdocCredentialRequest(@Param(1) String unknownFieldA, @Param(2) byte[] unknownFieldB, @Param(5) byte[] unknownFieldC) {
        this.unknownFieldA = unknownFieldA;
        this.unknownFieldB = unknownFieldB;
        this.unknownFieldC = unknownFieldC;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetMdocCredentialRequest> CREATOR = findCreator(GetMdocCredentialRequest.class);
}
