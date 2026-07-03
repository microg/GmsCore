/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import org.microg.safeparcel.AutoSafeParcelable;

public class VerifyPhoneNumberResponse extends AutoSafeParcelable {
    @Field(1)
    public String verifiedPhoneNumber;
    @Field(2)
    public PhoneNumberInfo[] phoneNumberInfos;
    @Field(3)
    public int verificationStatus;
    @Field(4)
    public String iidToken;

    public static final Creator<VerifyPhoneNumberResponse> CREATOR = new AutoCreator<>(VerifyPhoneNumberResponse.class);
}
