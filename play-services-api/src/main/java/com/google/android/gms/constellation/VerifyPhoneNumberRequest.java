/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import org.microg.safeparcel.AutoSafeParcelable;

public class VerifyPhoneNumberRequest extends AutoSafeParcelable {
    @Field(1)
    public String policyId;
    @Field(2)
    public String[] gcmCapablePackageNames;
    @Field(3)
    public boolean forceSmsVerification;
    @Field(4)
    public int simSlot;
    @Field(5)
    public ImsiRequest[] imsiRequests;
    @Field(6)
    public String verificationMethod;

    public static final Creator<VerifyPhoneNumberRequest> CREATOR = new AutoCreator<>(VerifyPhoneNumberRequest.class);

    public static class ImsiRequest extends AutoSafeParcelable {
        @Field(1)
        public String imsi;
        @Field(2)
        public String msisdn;

        public static final Creator<ImsiRequest> CREATOR = new AutoCreator<>(ImsiRequest.class);
    }
}
