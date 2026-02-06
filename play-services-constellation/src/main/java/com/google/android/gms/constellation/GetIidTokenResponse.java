package com.google.android.gms.constellation;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;

public class GetIidTokenResponse extends AutoSafeParcelable {
    @Field(1)
    public String iidToken;
    @Field(2)
    public String fid;
    @Field(3)
    public byte[] clientSignature;
    @Field(4)
    public long currentTimeMs;

    @Override
    public String toString() {
        return "GetIidTokenResponse{" +
                "iidToken='" + iidToken + '\'' +
                ", fid='" + fid + '\'' +
                ", clientSignature=" + Arrays.toString(clientSignature) +
                ", currentTimeMs=" + currentTimeMs +
                '}';
    }

    public static final Creator<GetIidTokenResponse> CREATOR = findCreator(GetIidTokenResponse.class);
}
