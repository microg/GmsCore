/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory;

import android.accounts.Account;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;


@SafeParcelable.Class
public class RequestCredentials extends AbstractSafeParcelable {

    @Field(1)
    public Account account;
    @Field(2)
    public String function;
    @Field(3)
    public String packageName;

    public RequestCredentials() {
    }

    @Constructor
    public RequestCredentials(@Param(1) Account account, @Param(2) String function, @Param(3) String packageName) {
        this.account = account;
        this.function = function;
        this.packageName = packageName;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<RequestCredentials> CREATOR = findCreator(RequestCredentials.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("RequestCredentials").field("account", account.name).field("function", function).field("packageName", packageName).end();
    }
}
