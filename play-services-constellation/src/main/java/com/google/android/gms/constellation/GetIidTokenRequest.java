package com.google.android.gms.constellation;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetIidTokenRequest extends AutoSafeParcelable {
    @Field(1)
    public long sender;

    @Override
    public String toString() {
        return "GetIidTokenRequest{" +
                "sender=" + sender +
                '}';
    }

    public static final Creator<GetIidTokenRequest> CREATOR = findCreator(GetIidTokenRequest.class);
}
