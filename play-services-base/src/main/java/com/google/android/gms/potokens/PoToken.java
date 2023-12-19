package com.google.android.gms.potokens;

import org.microg.safeparcel.AutoSafeParcelable;

public class PoToken extends AutoSafeParcelable {

    @Field(1)
    public  byte[] data;

    public PoToken(byte[] data) {
        this.data = data;
    }

    public static Creator<PoToken> CREATOR = new AutoCreator<>(PoToken.class);
}
