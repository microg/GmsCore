/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.issuer;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

@PublicApi
public class TokenStatus extends AutoSafeParcelable {
    @Field(2)
    @PublicApi(exclude = true)
    public String issuerTokenId;
    @Field(3)
    @PublicApi(exclude = true)
    private int tokenState;
    @Field(4)
    @PublicApi(exclude = true)
    private boolean isSelected;

    public int getTokenState() {
        return tokenState;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public static final Creator<TokenStatus> CREATOR = new AutoCreator<>(TokenStatus.class);
}
