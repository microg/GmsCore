/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.signin;

import androidx.annotation.NonNull;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class SignInAccount extends AutoSafeParcelable {
    @Field(4)
    public String email;
    @Field(7)
    public GoogleSignInAccount googleSignInAccount;
    @Field(8)
    public String userId;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("SignInAccount").field("email", email).field("account", googleSignInAccount).field("userId", userId).end();
    }

    public static final Creator<SignInAccount> CREATOR = findCreator(SignInAccount.class);
}
