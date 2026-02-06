package com.google.android.gms.constellation;

import org.microg.safeparcel.AutoSafeParcelable;

public class ImsiRequest extends AutoSafeParcelable {
    @Field(1)
    public String imsi;
    @Field(2)
    public String msisdn;

    @Override
    public String toString() {
        return "ImsiRequest{" +
                "imsi='" + imsi + '\'' +
                ", msisdn='" + msisdn + '\'' +
                '}';
    }

    public static final Creator<ImsiRequest> CREATOR = findCreator(ImsiRequest.class);
}
