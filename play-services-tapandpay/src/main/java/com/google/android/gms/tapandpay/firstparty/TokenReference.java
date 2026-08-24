/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.firstparty;

import org.microg.safeparcel.AutoSafeParcelable;

public class TokenReference extends AutoSafeParcelable {
    @Field(2)
    public String tokenReferenceId;
    @Field(3)
    public int tokenProvider;

    public static final Creator<TokenReference> CREATOR = new AutoCreator<>(TokenReference.class);
}
