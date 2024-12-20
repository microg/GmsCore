package com.google.android.gms.pay;

import android.os.Parcel;

import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

@Hide
@SafeParcelable.Class
public class CheckReadinessForEmoneyRequest extends AbstractSafeParcelable {
    @Field(1)
    public String serviceProvider;
    @Nullable
    @Field(2)
    public String accountName;

    @Constructor
    public CheckReadinessForEmoneyRequest(@Param(1) String serviceProvider, @Nullable @Param(2) String accountName) {
        this.serviceProvider = serviceProvider;
        this.accountName = accountName;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CheckReadinessForEmoneyRequest> CREATOR = findCreator(CheckReadinessForEmoneyRequest.class);
}
