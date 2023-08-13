/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.firstparty;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;
import java.util.Objects;

public class AccountInfo extends AutoSafeParcelable {
    @Field(2)
    public final String accountId;
    @Field(3)
    public final String accountName;

    private AccountInfo() {
        accountId = null;
        accountName = null;
    }

    public AccountInfo(String accountId, String accountName) {
        this.accountId = accountId;
        this.accountName = accountName;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof AccountInfo)) return false;
        return Objects.equals(accountId, ((AccountInfo) obj).accountId) && Objects.equals(accountName, ((AccountInfo) obj).accountName);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{accountId, accountName});
    }

    @NonNull
    @Override
    public String toString() {
        return new ToStringHelper("AccountInfo")
                .field("accountId", accountId)
                .field("accountName", accountName)
                .end();
    }

    public static final Creator<AccountInfo> CREATOR = new AutoCreator<>(AccountInfo.class);
}
