package com.google.android.gms.constellation;

import android.os.Bundle;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;

public class VerifyPhoneNumberResponse extends AutoSafeParcelable {
    @Field(1)
    public PhoneNumberVerification[] verifications;
    @Field(2)
    Bundle b;

    @Override
    public String toString() {
        return "VerifyPhoneNumberResponse{" +
                "a=" + Arrays.toString(verifications) +
                ", b=" + b +
                '}';
    }

    public static final Creator<VerifyPhoneNumberResponse> CREATOR = findCreator(VerifyPhoneNumberResponse.class);
}
