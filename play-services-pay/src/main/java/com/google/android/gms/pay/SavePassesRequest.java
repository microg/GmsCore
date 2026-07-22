package com.google.android.gms.pay;

import android.os.Parcel;

import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

@Hide
@SafeParcelable.Class
public class SavePassesRequest extends AbstractSafeParcelable {
    @Nullable
    @Field(1)
    public String json;
    @Nullable
    @Field(2)
    public String jwt;

    @Constructor
    public SavePassesRequest(@Nullable @Param(1) String json, @Nullable @Param(2) String jwt) {
        this.json = json;
        this.jwt = jwt;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SavePassesRequest> CREATOR = findCreator(SavePassesRequest.class);
}
