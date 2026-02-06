package com.google.android.gms.constellation;

import android.os.Bundle;

import org.microg.safeparcel.AutoSafeParcelable;

public class PhoneNumberVerification extends AutoSafeParcelable {
    @Field(1)
    public String a;
    @Field(2)
    public long timestampMillis;
    @Field(3)
    public int verificationMethod;
    @Field(4)
    public int d;
    @Field(5)
    public String msisdnToken;
    @Field(6)
    public Bundle f;
    /**
     * 1 - ok
     * 2, 4, 5, 6 - retryable error
     * 3 - otp throttled
     * 8 - ineligible
     * 9 - denied
     * 10 - not in service
     */
    @Field(7)
    public int verificationStatus;
    @Field(8)
    public long retryAfterSeconds;

    @Override
    public String toString() {
        return "PhoneNumberVerification{" +
                "a='" + a + '\'' +
                ", timestampMillis=" + timestampMillis +
                ", verificationMethod=" + verificationMethod +
                ", d=" + d +
                ", msisdnToken='" + msisdnToken + '\'' +
                ", f=" + f +
                ", verificationStatus=" + verificationStatus +
                ", retryAfterSeconds=" + retryAfterSeconds +
                '}';
    }

    public static final Creator<PhoneNumberVerification> CREATOR = findCreator(PhoneNumberVerification.class);
}