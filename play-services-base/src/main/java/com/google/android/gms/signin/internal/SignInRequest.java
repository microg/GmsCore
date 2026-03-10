/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.signin.internal;

import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.ResolveAccountRequest;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class SignInRequest extends AutoSafeParcelable {
    @Field(1)
    private final int versionCode = 1;
    @Field(2)
    public ResolveAccountRequest request;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("SignInRequest")
                .field("request", request)
                .end();
    }

    public static final Creator<SignInRequest> CREATOR = new AutoCreator<>(SignInRequest.class);
}
