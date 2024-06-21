package com.google.android.gms.pay;

import android.os.Parcel;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class SavePassesRequest extends AbstractSafeParcelable {
    @Field(1)
    public String passesJson;
    @Field(2)
    public String jwtToken;

    @Constructor
    public SavePassesRequest(@Param(1) String passesJson, @Param(2) String jwtToken) {
        this.passesJson = passesJson;
        this.jwtToken = jwtToken;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SavePassesRequest> CREATOR = findCreator(SavePassesRequest.class);
}
