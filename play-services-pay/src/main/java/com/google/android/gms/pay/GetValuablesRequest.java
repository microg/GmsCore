package com.google.android.gms.pay;

import android.os.Parcel;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

@SafeParcelable.Class
public class GetValuablesRequest extends AbstractSafeParcelable {

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetValuablesRequest> CREATOR = findCreator(GetValuablesRequest.class);
}
