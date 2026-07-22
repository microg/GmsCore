/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.firstparty;

import org.microg.safeparcel.AutoSafeParcelable;

public class TokenStatus extends AutoSafeParcelable {
    @Field(2)
    public TokenReference tokenReference;
    @Field(3)
    public int tokenState;
    @Field(4)
    public boolean isSelected;

    public static final Creator<TokenStatus> CREATOR = new AutoCreator<>(TokenStatus.class);
}
