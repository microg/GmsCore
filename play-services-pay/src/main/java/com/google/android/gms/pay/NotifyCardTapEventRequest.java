package com.google.android.gms.pay;

import android.os.Parcel;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class NotifyCardTapEventRequest extends AbstractSafeParcelable {
    @Field(1)
    public String eventJson;

    @Constructor
    public NotifyCardTapEventRequest(@Param(1) String eventJson) {
        this.eventJson = eventJson;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        CREATOR.writeToParcel(this, out, flags);
    }

    public static final SafeParcelableCreatorAndWriter<NotifyCardTapEventRequest> CREATOR = findCreator(NotifyCardTapEventRequest.class);
}
