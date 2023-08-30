/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal;

import android.accounts.Account;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class ResolveAccountRequest extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 2;
    @Field(2)
    public Account account;
    @Field(3)
    public int sessionId;
    @Field(4)
    @Nullable
    public GoogleSignInAccount signInAccountHint;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("ResolveAccountRequest")
                .field("account", account)
                .field("sessionId", sessionId)
                .field("signInAccountHint", signInAccountHint)
                .end();
    }

    public static final Creator<ResolveAccountRequest> CREATOR = new AutoCreator<>(ResolveAccountRequest.class);
}
