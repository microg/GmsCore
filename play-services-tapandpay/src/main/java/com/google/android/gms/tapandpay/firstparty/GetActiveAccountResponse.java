/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.firstparty;

import androidx.annotation.Nullable;
import org.microg.safeparcel.AutoSafeParcelable;

public class GetActiveAccountResponse extends AutoSafeParcelable {
    @Field(2)
    @Nullable
    public final AccountInfo accountInfo;

    private GetActiveAccountResponse() {
        accountInfo = null;
    }

    public GetActiveAccountResponse(@Nullable AccountInfo accountInfo) {
        this.accountInfo = accountInfo;
    }

    public static final Creator<GetActiveAccountResponse> CREATOR = new AutoCreator<>(GetActiveAccountResponse.class);
}
