package com.google.android.gms.constellation;

import androidx.annotation.NonNull;

import org.microg.safeparcel.AutoSafeParcelable;

public class IdTokenRequest extends AutoSafeParcelable {
    @Field(1)
    public String certificateHash; // android app cert sha1 e.g. CYChK+mTUowZEHvCGtgRR4xjzvw= (equivalent to 0980a12be993528c19107bc21ad811478c63cefc) for google messages
    @Field(2)
    public String tokenNonce;

    @NonNull
    @Override
    public String toString() {
        return "IdTokenRequest{" +
                "certificateHash='" + certificateHash + '\'' +
                ", tokenNonce='" + tokenNonce + '\'' +
                '}';
    }

    public static final Creator<IdTokenRequest> CREATOR = findCreator(IdTokenRequest.class);
}
