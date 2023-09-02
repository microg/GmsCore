/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.signin.internal;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class SignInConfiguration extends AutoSafeParcelable {
    @Field(2)
    public String packageName;
    @Field(5)
    public GoogleSignInOptions options;

    public static final Creator<SignInConfiguration> CREATOR = findCreator(SignInConfiguration.class);
}
