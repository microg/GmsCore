/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.signin.internal;

import androidx.annotation.NonNull;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class SignInConfiguration extends AutoSafeParcelable {
    @Field(2)
    public String packageName;
    @Field(5)
    public GoogleSignInOptions options;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("SignInConfiguration").field("packageName", packageName).field("options", options).end();
    }

    public static final Creator<SignInConfiguration> CREATOR = findCreator(SignInConfiguration.class);
}
