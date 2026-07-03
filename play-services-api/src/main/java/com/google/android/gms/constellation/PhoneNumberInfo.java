/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import org.microg.safeparcel.AutoSafeParcelable;

public class PhoneNumberInfo extends AutoSafeParcelable {
    @Field(1)
    public String phoneNumber;
    @Field(2)
    public int carrierId;
    @Field(3)
    public int transportType;

    public static final Creator<PhoneNumberInfo> CREATOR = new AutoCreator<>(PhoneNumberInfo.class);
}
