/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.firstparty;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;
import java.util.Objects;

@SafeParcelable.Class
public class AccountInfo extends AbstractSafeParcelable {
    @Field(2)
    public final String accountId;
    @Field(3)
    public final String accountName;
    @Field(4)
    public final int accountType;

    @Constructor
    public AccountInfo(@Param(2) String accountId, @Param(3) String accountName, @Param(4) int accountType) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.accountType = accountType;
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
                .field("accountType", accountType)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<AccountInfo> CREATOR = findCreator(AccountInfo.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
