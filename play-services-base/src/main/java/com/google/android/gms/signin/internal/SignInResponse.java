/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.signin.internal;

import androidx.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.internal.ResolveAccountResponse;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class SignInResponse extends AutoSafeParcelable {
    @Field(1)
    private final int versionCode = 1;
    @Field(2)
    public ConnectionResult connectionResult;
    @Field(3)
    public ResolveAccountResponse response;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("SignInResponse")
                .field("connectionResult", connectionResult)
                .field("response", response)
                .end();
    }

    public static final Creator<SignInResponse> CREATOR = new AutoCreator<>(SignInResponse.class);
}
