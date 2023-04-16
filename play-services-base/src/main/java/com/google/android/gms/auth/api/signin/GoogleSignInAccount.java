/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.signin;

import org.microg.safeparcel.AutoSafeParcelable;

public class GoogleSignInAccount extends AutoSafeParcelable {
    public static final Creator<GoogleSignInAccount> CREATOR = new AutoCreator<GoogleSignInAccount>(GoogleSignInAccount.class);
}
